package com.b4rrhh.employee.working_time.application.service;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.Timeline;
import com.b4rrhh.employee.temporal.support.TimelineCoverage;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelinePlan;
import com.b4rrhh.employee.temporal.support.TimelinePlanner;
import com.b4rrhh.employee.working_time.application.model.WorkingTimePlan;
import com.b4rrhh.employee.working_time.application.model.WorkingTimePlanAdjustment;
import com.b4rrhh.employee.working_time.application.port.WorkingTimePresenceConsistencyPort;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeCoverageGapException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeIsACorrectionException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOutsidePresencePeriodException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOverlapException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeOccurrence;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimePeriod;
import com.b4rrhh.employee.working_time.domain.port.WorkingTimeRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Where the working time series meets the temporal component (ADR-057).
 * It declares the coverage of the series as mandatory, builds the timeline
 * from the persisted occurrences and the employee's presence, asks the
 * planner what an operation would do, and gives every occurrence of the
 * answer back its number.
 *
 * <p>It plans and it judges; it never writes. The write use cases read the
 * plan and apply it, and the plan use case returns it as it is.
 */
@Component
public class WorkingTimeTimelineService {

    private static final TimelineCoverage COVERAGE = TimelineCoverage.MANDATORY;

    private final WorkingTimeRepository workingTimeRepository;
    private final WorkingTimePresenceConsistencyPort workingTimePresenceConsistencyPort;
    private final TimelinePlanner timelinePlanner = new TimelinePlanner();

    public WorkingTimeTimelineService(
            WorkingTimeRepository workingTimeRepository,
            WorkingTimePresenceConsistencyPort workingTimePresenceConsistencyPort
    ) {
        this.workingTimeRepository = workingTimeRepository;
        this.workingTimePresenceConsistencyPort = workingTimePresenceConsistencyPort;
    }

    public WorkingTimePlan planAdd(Long employeeId, DateRange occurrence) {
        Series series = load(employeeId);
        TimelinePlan plan = timelinePlanner.planAdd(series.timeline(), occurrence);

        return toPlan(plan, series, null);
    }

    public WorkingTimePlan planRemove(Long employeeId, WorkingTime occurrence) {
        Series series = load(employeeId);
        DateRange removed = rangeOf(occurrence);
        TimelinePlan plan = timelinePlanner.planRemove(series.timeline(), removed);

        return toPlan(plan, series, occurrence.getWorkingTimeNumber());
    }

    public WorkingTimePlan planCorrect(Long employeeId, WorkingTime occurrence, DateRange corrected) {
        Series series = load(employeeId);
        DateRange existing = rangeOf(occurrence);
        TimelinePlan plan = timelinePlanner.planCorrect(series.timeline(), existing, corrected);

        return toPlan(plan, series, occurrence.getWorkingTimeNumber());
    }

    /**
     * Throws the business exception a rejected plan stands for. Accepted
     * plans pass through. The switch is an expression on purpose: a rejection
     * the component adds and this vertical does not translate stops compiling
     * instead of slipping through (backend#58).
     */
    public void requireAccepted(
            WorkingTimePlan plan,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        if (plan.isAccepted()) {
            return;
        }

        WorkingTimeOccurrence occurrence = plan.occurrence();
        throw switch (plan.rejection()) {
            case OUTSIDE_PRESENCE -> new WorkingTimeOutsidePresencePeriodException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    occurrence.startDate(),
                    occurrence.endDate()
            );
            case OVERLAP -> new WorkingTimeOverlapException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    occurrence.startDate(),
                    occurrence.endDate(),
                    plan.overlaps()
            );
            case GAP_NOT_ALLOWED -> new WorkingTimeCoverageGapException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    plan.gaps(),
                    plan.stretchCandidates()
            );
            case IS_A_CORRECTION -> new WorkingTimeIsACorrectionException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    plan.correctedOccurrence(),
                    new WorkingTimePeriod(occurrence.startDate(), occurrence.endDate())
            );
        };
    }

    private Series load(Long employeeId) {
        List<WorkingTime> occurrences = workingTimeRepository.findByEmployeeIdOrderByStartDate(employeeId);
        List<DateRange> presence = workingTimePresenceConsistencyPort
                .findPresencePeriodsByEmployeeIdOrderByStartDate(employeeId);

        Timeline timeline = new Timeline(
                COVERAGE,
                presence,
                occurrences.stream().map(WorkingTimeTimelineService::rangeOf).toList()
        );

        return new Series(occurrences, timeline);
    }

    /**
     * Gives the ranges of the plan their numbers. The numbers travel with the
     * ranges: an adjusted or corrected occurrence keeps its number under its
     * new dates, a removed one gives it up, and the one an add would create
     * is the only range left without one.
     *
     * <p>{@code subjectNumber} is the number of the occurrence the caller
     * asked about, when it asked about one. An add asks about none: if the
     * component answers that the add is a correction, the corrected
     * occurrence is the one whose start date it landed on, and its number is
     * found here (backend#58).
     */
    private WorkingTimePlan toPlan(TimelinePlan plan, Series series, Integer subjectNumber) {
        Map<DateRange, Deque<Integer>> numbers = series.numbersByRange();

        WorkingTimePlanAdjustment adjustment = null;
        if (plan.adjustsAnOccurrence()) {
            DateRange before = plan.adjustedOccurrence().before();
            DateRange after = plan.adjustedOccurrence().after();
            Integer adjustedNumber = take(numbers, before);
            give(numbers, after, adjustedNumber);
            adjustment = new WorkingTimePlanAdjustment(adjustedNumber, toPeriod(before), toPeriod(after));
        }

        if (plan.operation() == TimelineOperation.REMOVE) {
            take(numbers, plan.occurrence());
        }

        WorkingTimeOccurrence correctedOccurrence = null;
        if (plan.operation() == TimelineOperation.CORRECT) {
            DateRange before = plan.correctedOccurrence();
            Integer numberUnderThoseDates = take(numbers, before);
            if (subjectNumber == null) {
                subjectNumber = numberUnderThoseDates;
            }
            give(numbers, plan.occurrence(), subjectNumber);
            correctedOccurrence = new WorkingTimeOccurrence(subjectNumber, before.startDate(), before.endDate());
        }

        List<WorkingTimeOccurrence> projected = new ArrayList<>();
        for (DateRange range : plan.projected()) {
            projected.add(new WorkingTimeOccurrence(take(numbers, range), range.startDate(), range.endDate()));
        }

        List<WorkingTimeOccurrence> stretchCandidates = plan.stretchCandidates().stream()
                .map(candidate -> projected.stream()
                        .filter(occurrence -> sameDates(occurrence, candidate))
                        .findFirst()
                        .orElseThrow())
                .toList();

        return new WorkingTimePlan(
                plan.operation(),
                plan.rejection(),
                new WorkingTimeOccurrence(subjectNumber, plan.occurrence().startDate(), plan.occurrence().endDate()),
                correctedOccurrence,
                adjustment,
                plan.overlaps().stream().map(WorkingTimeTimelineService::toPeriod).toList(),
                plan.gaps().stream().map(WorkingTimeTimelineService::toPeriod).toList(),
                stretchCandidates,
                projected
        );
    }

    private static boolean sameDates(WorkingTimeOccurrence occurrence, DateRange range) {
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

    private static DateRange rangeOf(WorkingTime workingTime) {
        return new DateRange(workingTime.getStartDate(), workingTime.getEndDate());
    }

    private static WorkingTimePeriod toPeriod(DateRange range) {
        return new WorkingTimePeriod(range.startDate(), range.endDate());
    }

    private record Series(List<WorkingTime> occurrences, Timeline timeline) {

        Map<DateRange, Deque<Integer>> numbersByRange() {
            Map<DateRange, Deque<Integer>> numbers = new HashMap<>();
            for (WorkingTime occurrence : occurrences) {
                give(numbers, rangeOf(occurrence), occurrence.getWorkingTimeNumber());
            }
            return numbers;
        }
    }
}
