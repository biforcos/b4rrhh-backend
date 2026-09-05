package com.b4rrhh.employee.labor_classification.application.service;

import com.b4rrhh.employee.labor_classification.application.model.LaborClassificationPlan;
import com.b4rrhh.employee.labor_classification.application.model.LaborClassificationPlanAdjustment;
import com.b4rrhh.employee.labor_classification.application.port.LaborClassificationPresenceConsistencyPort;
import com.b4rrhh.employee.labor_classification.application.port.PresencePeriod;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationCoverageIncompleteException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationIsACorrectionException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationOutsidePresencePeriodException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationOverlapException;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassificationPeriod;
import com.b4rrhh.employee.labor_classification.domain.port.LaborClassificationRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.Timeline;
import com.b4rrhh.employee.temporal.support.TimelineCoverage;
import com.b4rrhh.employee.temporal.support.TimelinePlan;
import com.b4rrhh.employee.temporal.support.TimelinePlanner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Where the labor classification series meets the temporal component
 * (ADR-057). It declares the coverage of the series as mandatory, builds the
 * timeline from the persisted occurrences and the employee's presence, and
 * asks the planner what an operation would do.
 *
 * <p>A labor classification is identified by the day it starts, so the ranges
 * the planner answers with already name the occurrences: there is no number
 * to give back.
 *
 * <p>It plans and it judges; it never writes. The write use cases read the
 * plan and apply it, and the plan use case returns it as it is.
 */
@Component
public class LaborClassificationTimelineService {

    private static final TimelineCoverage COVERAGE = TimelineCoverage.MANDATORY;

    private final LaborClassificationRepository laborClassificationRepository;
    private final LaborClassificationPresenceConsistencyPort laborClassificationPresenceConsistencyPort;
    private final TimelinePlanner timelinePlanner = new TimelinePlanner();

    public LaborClassificationTimelineService(
            LaborClassificationRepository laborClassificationRepository,
            LaborClassificationPresenceConsistencyPort laborClassificationPresenceConsistencyPort
    ) {
        this.laborClassificationRepository = laborClassificationRepository;
        this.laborClassificationPresenceConsistencyPort = laborClassificationPresenceConsistencyPort;
    }

    public LaborClassificationPlan planAdd(Long employeeId, DateRange occurrence) {
        return toPlan(timelinePlanner.planAdd(load(employeeId), occurrence));
    }

    public LaborClassificationPlan planRemove(Long employeeId, LaborClassification occurrence) {
        return toPlan(timelinePlanner.planRemove(load(employeeId), rangeOf(occurrence)));
    }

    public LaborClassificationPlan planCorrect(Long employeeId, LaborClassification occurrence, DateRange corrected) {
        return toPlan(timelinePlanner.planCorrect(load(employeeId), rangeOf(occurrence), corrected));
    }

    /**
     * Throws the business exception a rejected plan stands for. Accepted
     * plans pass through. The switch is an expression on purpose: a rejection
     * the component adds and this vertical does not translate stops compiling
     * instead of slipping through (backend#58).
     */
    public void requireAccepted(
            LaborClassificationPlan plan,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        if (plan.isAccepted()) {
            return;
        }

        LaborClassificationPeriod occurrence = plan.occurrence();
        throw switch (plan.rejection()) {
            case OUTSIDE_PRESENCE -> new LaborClassificationOutsidePresencePeriodException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    occurrence.startDate(),
                    occurrence.endDate()
            );
            case OVERLAP -> new LaborClassificationOverlapException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    occurrence.startDate(),
                    occurrence.endDate(),
                    plan.overlaps()
            );
            case GAP_NOT_ALLOWED -> new LaborClassificationCoverageIncompleteException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    plan.gaps(),
                    plan.stretchCandidates()
            );
            case IS_A_CORRECTION -> new LaborClassificationIsACorrectionException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    plan.correctedOccurrence(),
                    occurrence
            );
        };
    }

    private Timeline load(Long employeeId) {
        List<DateRange> occurrences = laborClassificationRepository
                .findByEmployeeIdOrderByStartDate(employeeId)
                .stream()
                .map(LaborClassificationTimelineService::rangeOf)
                .toList();
        List<DateRange> presence = laborClassificationPresenceConsistencyPort
                .findPresencePeriodsByEmployeeIdOrderByStartDate(employeeId)
                .stream()
                .map(LaborClassificationTimelineService::rangeOf)
                .toList();

        return new Timeline(COVERAGE, presence, occurrences);
    }

    private static LaborClassificationPlan toPlan(TimelinePlan plan) {
        LaborClassificationPlanAdjustment adjustment = plan.adjustsAnOccurrence()
                ? new LaborClassificationPlanAdjustment(
                        toPeriod(plan.adjustedOccurrence().before()),
                        toPeriod(plan.adjustedOccurrence().after())
                )
                : null;

        return new LaborClassificationPlan(
                plan.operation(),
                plan.rejection(),
                toPeriod(plan.occurrence()),
                plan.correctedOccurrence() == null ? null : toPeriod(plan.correctedOccurrence()),
                adjustment,
                toPeriods(plan.overlaps()),
                toPeriods(plan.gaps()),
                toPeriods(plan.stretchCandidates()),
                toPeriods(plan.projected())
        );
    }

    private static DateRange rangeOf(LaborClassification laborClassification) {
        return new DateRange(laborClassification.getStartDate(), laborClassification.getEndDate());
    }

    private static DateRange rangeOf(PresencePeriod presence) {
        return new DateRange(presence.startDate(), presence.endDate());
    }

    private static LaborClassificationPeriod toPeriod(DateRange range) {
        return new LaborClassificationPeriod(range.startDate(), range.endDate());
    }

    private static List<LaborClassificationPeriod> toPeriods(List<DateRange> ranges) {
        return ranges.stream().map(LaborClassificationTimelineService::toPeriod).toList();
    }
}
