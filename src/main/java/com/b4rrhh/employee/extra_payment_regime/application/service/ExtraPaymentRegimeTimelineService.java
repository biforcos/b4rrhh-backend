package com.b4rrhh.employee.extra_payment_regime.application.service;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.Timeline;
import com.b4rrhh.employee.temporal.support.TimelineCoverage;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelinePlan;
import com.b4rrhh.employee.temporal.support.TimelinePlanner;
import com.b4rrhh.employee.extra_payment_regime.application.model.ExtraPaymentRegimePlan;
import com.b4rrhh.employee.extra_payment_regime.application.model.ExtraPaymentRegimePlanAdjustment;
import com.b4rrhh.employee.extra_payment_regime.application.port.ExtraPaymentRegimePresenceConsistencyPort;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeCoverageGapException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeIsACorrectionException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeOutsidePresencePeriodException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeOverlapException;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegime;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegimeOccurrence;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegimePeriod;
import com.b4rrhh.employee.extra_payment_regime.domain.port.ExtraPaymentRegimeRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Where the extra payment regime series meets the temporal component (ADR-057).
 * It declares the coverage of the series as mandatory, builds the timeline
 * from the persisted occurrences and the employee's presence, asks the
 * planner what an operation would do, and gives every occurrence of the
 * answer back its number.
 *
 * <p>It plans and it judges; it never writes. The write use cases read the
 * plan and apply it, and the plan use case returns it as it is.
 */
@Component
public class ExtraPaymentRegimeTimelineService {

    private static final TimelineCoverage COVERAGE = TimelineCoverage.MANDATORY;

    private final ExtraPaymentRegimeRepository extraPaymentRegimeRepository;
    private final ExtraPaymentRegimePresenceConsistencyPort extraPaymentRegimePresenceConsistencyPort;
    private final TimelinePlanner timelinePlanner = new TimelinePlanner();

    public ExtraPaymentRegimeTimelineService(
            ExtraPaymentRegimeRepository extraPaymentRegimeRepository,
            ExtraPaymentRegimePresenceConsistencyPort extraPaymentRegimePresenceConsistencyPort
    ) {
        this.extraPaymentRegimeRepository = extraPaymentRegimeRepository;
        this.extraPaymentRegimePresenceConsistencyPort = extraPaymentRegimePresenceConsistencyPort;
    }

    public ExtraPaymentRegimePlan planAdd(Long employeeId, DateRange occurrence) {
        Series series = load(employeeId);
        TimelinePlan plan = timelinePlanner.planAdd(series.timeline(), occurrence);

        return toPlan(plan, series, null);
    }

    public ExtraPaymentRegimePlan planRemove(Long employeeId, ExtraPaymentRegime occurrence) {
        Series series = load(employeeId);
        DateRange removed = rangeOf(occurrence);
        TimelinePlan plan = timelinePlanner.planRemove(series.timeline(), removed);

        return toPlan(plan, series, occurrence.getExtraPaymentRegimeNumber());
    }

    public ExtraPaymentRegimePlan planCorrect(Long employeeId, ExtraPaymentRegime occurrence, DateRange corrected) {
        Series series = load(employeeId);
        DateRange existing = rangeOf(occurrence);
        TimelinePlan plan = timelinePlanner.planCorrect(series.timeline(), existing, corrected);

        return toPlan(plan, series, occurrence.getExtraPaymentRegimeNumber());
    }

    /**
     * Throws the business exception a rejected plan stands for. Accepted
     * plans pass through. The switch is an expression on purpose: a rejection
     * the component adds and this vertical does not translate stops compiling
     * instead of slipping through (backend#58).
     */
    public void requireAccepted(
            ExtraPaymentRegimePlan plan,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        if (plan.isAccepted()) {
            return;
        }

        ExtraPaymentRegimeOccurrence occurrence = plan.occurrence();
        throw switch (plan.rejection()) {
            case OUTSIDE_PRESENCE -> new ExtraPaymentRegimeOutsidePresencePeriodException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    occurrence.startDate(),
                    occurrence.endDate()
            );
            case OVERLAP -> new ExtraPaymentRegimeOverlapException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    occurrence.startDate(),
                    occurrence.endDate(),
                    plan.overlaps()
            );
            case GAP_NOT_ALLOWED -> new ExtraPaymentRegimeCoverageGapException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    plan.gaps(),
                    plan.stretchCandidates()
            );
            case IS_A_CORRECTION -> new ExtraPaymentRegimeIsACorrectionException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    plan.correctedOccurrence(),
                    new ExtraPaymentRegimePeriod(occurrence.startDate(), occurrence.endDate())
            );
        };
    }

    private Series load(Long employeeId) {
        List<ExtraPaymentRegime> occurrences = extraPaymentRegimeRepository.findByEmployeeIdOrderByStartDate(employeeId);
        List<DateRange> presence = extraPaymentRegimePresenceConsistencyPort
                .findPresencePeriodsByEmployeeIdOrderByStartDate(employeeId);

        Timeline timeline = new Timeline(
                COVERAGE,
                presence,
                occurrences.stream().map(ExtraPaymentRegimeTimelineService::rangeOf).toList()
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
    private ExtraPaymentRegimePlan toPlan(TimelinePlan plan, Series series, Integer subjectNumber) {
        Map<DateRange, Deque<Integer>> numbers = series.numbersByRange();

        ExtraPaymentRegimePlanAdjustment adjustment = null;
        if (plan.adjustsAnOccurrence()) {
            DateRange before = plan.adjustedOccurrence().before();
            DateRange after = plan.adjustedOccurrence().after();
            Integer adjustedNumber = take(numbers, before);
            give(numbers, after, adjustedNumber);
            adjustment = new ExtraPaymentRegimePlanAdjustment(adjustedNumber, toPeriod(before), toPeriod(after));
        }

        if (plan.operation() == TimelineOperation.REMOVE) {
            take(numbers, plan.occurrence());
        }

        ExtraPaymentRegimeOccurrence correctedOccurrence = null;
        if (plan.operation() == TimelineOperation.CORRECT) {
            DateRange before = plan.correctedOccurrence();
            Integer numberUnderThoseDates = take(numbers, before);
            if (subjectNumber == null) {
                subjectNumber = numberUnderThoseDates;
            }
            give(numbers, plan.occurrence(), subjectNumber);
            correctedOccurrence = new ExtraPaymentRegimeOccurrence(subjectNumber, before.startDate(), before.endDate());
        }

        List<ExtraPaymentRegimeOccurrence> projected = new ArrayList<>();
        for (DateRange range : plan.projected()) {
            projected.add(new ExtraPaymentRegimeOccurrence(take(numbers, range), range.startDate(), range.endDate()));
        }

        List<ExtraPaymentRegimeOccurrence> stretchCandidates = plan.stretchCandidates().stream()
                .map(candidate -> projected.stream()
                        .filter(occurrence -> sameDates(occurrence, candidate))
                        .findFirst()
                        .orElseThrow())
                .toList();

        return new ExtraPaymentRegimePlan(
                plan.operation(),
                plan.rejection(),
                new ExtraPaymentRegimeOccurrence(subjectNumber, plan.occurrence().startDate(), plan.occurrence().endDate()),
                correctedOccurrence,
                adjustment,
                plan.overlaps().stream().map(ExtraPaymentRegimeTimelineService::toPeriod).toList(),
                plan.gaps().stream().map(ExtraPaymentRegimeTimelineService::toPeriod).toList(),
                stretchCandidates,
                projected
        );
    }

    private static boolean sameDates(ExtraPaymentRegimeOccurrence occurrence, DateRange range) {
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

    private static DateRange rangeOf(ExtraPaymentRegime extraPaymentRegime) {
        return new DateRange(extraPaymentRegime.getStartDate(), extraPaymentRegime.getEndDate());
    }

    private static ExtraPaymentRegimePeriod toPeriod(DateRange range) {
        return new ExtraPaymentRegimePeriod(range.startDate(), range.endDate());
    }

    private record Series(List<ExtraPaymentRegime> occurrences, Timeline timeline) {

        Map<DateRange, Deque<Integer>> numbersByRange() {
            Map<DateRange, Deque<Integer>> numbers = new HashMap<>();
            for (ExtraPaymentRegime occurrence : occurrences) {
                give(numbers, rangeOf(occurrence), occurrence.getExtraPaymentRegimeNumber());
            }
            return numbers;
        }
    }
}
