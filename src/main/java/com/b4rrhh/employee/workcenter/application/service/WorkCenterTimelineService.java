package com.b4rrhh.employee.workcenter.application.service;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.Timeline;
import com.b4rrhh.employee.temporal.support.TimelineCoverage;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelinePlan;
import com.b4rrhh.employee.temporal.support.TimelinePlanner;
import com.b4rrhh.employee.workcenter.application.model.WorkCenterPlan;
import com.b4rrhh.employee.workcenter.application.model.WorkCenterPlanAdjustment;
import com.b4rrhh.employee.workcenter.application.port.PresencePeriod;
import com.b4rrhh.employee.workcenter.application.port.WorkCenterPresenceConsistencyPort;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterIsACorrectionException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOverlapException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterPresenceCoverageGapException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterOccurrence;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterPeriod;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Where the work center series meets the temporal component (ADR-057). It
 * declares the coverage of the series as mandatory, builds the timeline from
 * the persisted assignments and the employee's presence, asks the planner
 * what an operation would do, and gives every assignment of the answer back
 * its number.
 *
 * <p>It plans and it judges; it never writes. The write use cases read the
 * plan and apply it, and the plan use case returns it as it is.
 */
@Component
public class WorkCenterTimelineService {

    private static final TimelineCoverage COVERAGE = TimelineCoverage.MANDATORY;

    private final WorkCenterRepository workCenterRepository;
    private final WorkCenterPresenceConsistencyPort workCenterPresenceConsistencyPort;
    private final TimelinePlanner timelinePlanner = new TimelinePlanner();

    public WorkCenterTimelineService(
            WorkCenterRepository workCenterRepository,
            WorkCenterPresenceConsistencyPort workCenterPresenceConsistencyPort
    ) {
        this.workCenterRepository = workCenterRepository;
        this.workCenterPresenceConsistencyPort = workCenterPresenceConsistencyPort;
    }

    public WorkCenterPlan planAdd(Long employeeId, DateRange occurrence) {
        Series series = load(employeeId);
        TimelinePlan plan = timelinePlanner.planAdd(series.timeline(), occurrence);

        return toPlan(plan, series, null);
    }

    public WorkCenterPlan planRemove(Long employeeId, WorkCenter occurrence) {
        Series series = load(employeeId);
        TimelinePlan plan = timelinePlanner.planRemove(series.timeline(), rangeOf(occurrence));

        return toPlan(plan, series, occurrence.getWorkCenterAssignmentNumber());
    }

    public WorkCenterPlan planCorrect(Long employeeId, WorkCenter occurrence, DateRange corrected) {
        Series series = load(employeeId);
        TimelinePlan plan = timelinePlanner.planCorrect(series.timeline(), rangeOf(occurrence), corrected);

        return toPlan(plan, series, occurrence.getWorkCenterAssignmentNumber());
    }

    /**
     * Throws the business exception a rejected plan stands for. Accepted
     * plans pass through. The switch is an expression on purpose: a rejection
     * the component adds and this vertical does not translate stops compiling
     * instead of slipping through (backend#58).
     */
    public void requireAccepted(
            WorkCenterPlan plan,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        if (plan.isAccepted()) {
            return;
        }

        WorkCenterOccurrence occurrence = plan.occurrence();
        throw switch (plan.rejection()) {
            case OUTSIDE_PRESENCE -> new WorkCenterOutsidePresencePeriodException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    occurrence.startDate(),
                    occurrence.endDate()
            );
            case OVERLAP -> new WorkCenterOverlapException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    occurrence.startDate(),
                    occurrence.endDate(),
                    plan.overlaps()
            );
            case GAP_NOT_ALLOWED -> new WorkCenterPresenceCoverageGapException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    plan.gaps(),
                    plan.stretchCandidates()
            );
            case IS_A_CORRECTION -> new WorkCenterIsACorrectionException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    plan.correctedOccurrence(),
                    new WorkCenterPeriod(occurrence.startDate(), occurrence.endDate())
            );
        };
    }

    private Series load(Long employeeId) {
        List<WorkCenter> occurrences = workCenterRepository.findByEmployeeIdOrderByStartDate(employeeId);
        List<DateRange> presence = workCenterPresenceConsistencyPort
                .findPresencePeriodsByEmployeeIdOrderByStartDate(employeeId)
                .stream()
                .map(WorkCenterTimelineService::rangeOf)
                .toList();

        Timeline timeline = new Timeline(
                COVERAGE,
                presence,
                occurrences.stream().map(WorkCenterTimelineService::rangeOf).toList()
        );

        return new Series(occurrences, timeline);
    }

    /**
     * Gives the ranges of the plan their numbers. The numbers travel with the
     * ranges: an adjusted or corrected assignment keeps its number under its
     * new dates, a removed one gives it up, and the one an add would create
     * is the only range left without one.
     *
     * <p>{@code subjectNumber} is the number of the assignment the caller
     * asked about, when it asked about one. An add asks about none: if the
     * component answers that the add is a correction, the corrected
     * assignment is the one whose start date it landed on, and its number is
     * found here (backend#58).
     */
    private WorkCenterPlan toPlan(TimelinePlan plan, Series series, Integer subjectNumber) {
        Map<DateRange, Deque<Integer>> numbers = series.numbersByRange();

        WorkCenterPlanAdjustment adjustment = null;
        if (plan.adjustsAnOccurrence()) {
            DateRange before = plan.adjustedOccurrence().before();
            DateRange after = plan.adjustedOccurrence().after();
            Integer adjustedNumber = take(numbers, before);
            give(numbers, after, adjustedNumber);
            adjustment = new WorkCenterPlanAdjustment(adjustedNumber, toPeriod(before), toPeriod(after));
        }

        if (plan.operation() == TimelineOperation.REMOVE) {
            take(numbers, plan.occurrence());
        }

        WorkCenterOccurrence correctedOccurrence = null;
        if (plan.operation() == TimelineOperation.CORRECT) {
            DateRange before = plan.correctedOccurrence();
            Integer numberUnderThoseDates = take(numbers, before);
            if (subjectNumber == null) {
                subjectNumber = numberUnderThoseDates;
            }
            give(numbers, plan.occurrence(), subjectNumber);
            correctedOccurrence = new WorkCenterOccurrence(subjectNumber, before.startDate(), before.endDate());
        }

        List<WorkCenterOccurrence> projected = new ArrayList<>();
        for (DateRange range : plan.projected()) {
            projected.add(new WorkCenterOccurrence(take(numbers, range), range.startDate(), range.endDate()));
        }

        List<WorkCenterOccurrence> stretchCandidates = plan.stretchCandidates().stream()
                .map(candidate -> projected.stream()
                        .filter(occurrence -> sameDates(occurrence, candidate))
                        .findFirst()
                        .orElseThrow())
                .toList();

        return new WorkCenterPlan(
                plan.operation(),
                plan.rejection(),
                new WorkCenterOccurrence(subjectNumber, plan.occurrence().startDate(), plan.occurrence().endDate()),
                correctedOccurrence,
                adjustment,
                plan.overlaps().stream().map(WorkCenterTimelineService::toPeriod).toList(),
                plan.gaps().stream().map(WorkCenterTimelineService::toPeriod).toList(),
                stretchCandidates,
                projected
        );
    }

    private static boolean sameDates(WorkCenterOccurrence occurrence, DateRange range) {
        return occurrence.startDate().equals(range.startDate())
                && Objects.equals(occurrence.endDate(), range.endDate());
    }

    private static Integer take(Map<DateRange, Deque<Integer>> numbers, DateRange range) {
        Deque<Integer> queue = numbers.get(range);
        return queue == null ? null : queue.pollFirst();
    }

    private static void give(Map<DateRange, Deque<Integer>> numbers, DateRange range, Integer number) {
        if (number != null) {
            numbers.computeIfAbsent(range, ignored -> new ArrayDeque<>()).addLast(number);
        }
    }

    private static DateRange rangeOf(WorkCenter workCenter) {
        return new DateRange(workCenter.getStartDate(), workCenter.getEndDate());
    }

    private static DateRange rangeOf(PresencePeriod presence) {
        return new DateRange(presence.startDate(), presence.endDate());
    }

    private static WorkCenterPeriod toPeriod(DateRange range) {
        return new WorkCenterPeriod(range.startDate(), range.endDate());
    }

    private record Series(List<WorkCenter> occurrences, Timeline timeline) {

        Map<DateRange, Deque<Integer>> numbersByRange() {
            Map<DateRange, Deque<Integer>> numbers = new HashMap<>();
            for (WorkCenter occurrence : occurrences) {
                give(numbers, rangeOf(occurrence), occurrence.getWorkCenterAssignmentNumber());
            }
            return numbers;
        }
    }
}
