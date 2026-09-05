package com.b4rrhh.employee.contract.application.service;

import com.b4rrhh.employee.contract.application.model.ContractPlan;
import com.b4rrhh.employee.contract.application.model.ContractPlanAdjustment;
import com.b4rrhh.employee.contract.application.port.ContractPresenceConsistencyPort;
import com.b4rrhh.employee.contract.application.port.PresencePeriod;
import com.b4rrhh.employee.contract.domain.exception.ContractCoverageIncompleteException;
import com.b4rrhh.employee.contract.domain.exception.ContractIsACorrectionException;
import com.b4rrhh.employee.contract.domain.exception.ContractOutsidePresencePeriodException;
import com.b4rrhh.employee.contract.domain.exception.ContractOverlapException;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.contract.domain.model.ContractPeriod;
import com.b4rrhh.employee.contract.domain.port.ContractRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.Timeline;
import com.b4rrhh.employee.temporal.support.TimelineCoverage;
import com.b4rrhh.employee.temporal.support.TimelinePlan;
import com.b4rrhh.employee.temporal.support.TimelinePlanner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Where the contract series meets the temporal component (ADR-057). It
 * declares the coverage of the series as mandatory, builds the timeline from
 * the persisted contracts and the employee's presence, and asks the planner
 * what an operation would do.
 *
 * <p>A contract is identified by the day it starts, so the ranges the planner
 * answers with already name the contracts: there is no number to give back.
 *
 * <p>It plans and it judges; it never writes. The write use cases read the
 * plan and apply it, and the plan use case returns it as it is.
 */
@Component
public class ContractTimelineService {

    private static final TimelineCoverage COVERAGE = TimelineCoverage.MANDATORY;

    private final ContractRepository contractRepository;
    private final ContractPresenceConsistencyPort contractPresenceConsistencyPort;
    private final TimelinePlanner timelinePlanner = new TimelinePlanner();

    public ContractTimelineService(
            ContractRepository contractRepository,
            ContractPresenceConsistencyPort contractPresenceConsistencyPort
    ) {
        this.contractRepository = contractRepository;
        this.contractPresenceConsistencyPort = contractPresenceConsistencyPort;
    }

    public ContractPlan planAdd(Long employeeId, DateRange occurrence) {
        return toPlan(timelinePlanner.planAdd(load(employeeId), occurrence));
    }

    public ContractPlan planRemove(Long employeeId, Contract occurrence) {
        return toPlan(timelinePlanner.planRemove(load(employeeId), rangeOf(occurrence)));
    }

    public ContractPlan planCorrect(Long employeeId, Contract occurrence, DateRange corrected) {
        return toPlan(timelinePlanner.planCorrect(load(employeeId), rangeOf(occurrence), corrected));
    }

    /**
     * Throws the business exception a rejected plan stands for. Accepted
     * plans pass through. The switch is an expression on purpose: a rejection
     * the component adds and this vertical does not translate stops compiling
     * instead of slipping through (backend#58).
     */
    public void requireAccepted(
            ContractPlan plan,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        if (plan.isAccepted()) {
            return;
        }

        ContractPeriod occurrence = plan.occurrence();
        throw switch (plan.rejection()) {
            case OUTSIDE_PRESENCE -> new ContractOutsidePresencePeriodException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    occurrence.startDate(),
                    occurrence.endDate()
            );
            case OVERLAP -> new ContractOverlapException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    occurrence.startDate(),
                    occurrence.endDate(),
                    plan.overlaps()
            );
            case GAP_NOT_ALLOWED -> new ContractCoverageIncompleteException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    plan.gaps(),
                    plan.stretchCandidates()
            );
            case IS_A_CORRECTION -> new ContractIsACorrectionException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    plan.correctedOccurrence(),
                    occurrence
            );
        };
    }

    private Timeline load(Long employeeId) {
        List<DateRange> occurrences = contractRepository
                .findByEmployeeIdOrderByStartDate(employeeId)
                .stream()
                .map(ContractTimelineService::rangeOf)
                .toList();
        List<DateRange> presence = contractPresenceConsistencyPort
                .findPresencePeriodsByEmployeeIdOrderByStartDate(employeeId)
                .stream()
                .map(ContractTimelineService::rangeOf)
                .toList();

        return new Timeline(COVERAGE, presence, occurrences);
    }

    private static ContractPlan toPlan(TimelinePlan plan) {
        ContractPlanAdjustment adjustment = plan.adjustsAnOccurrence()
                ? new ContractPlanAdjustment(
                        toPeriod(plan.adjustedOccurrence().before()),
                        toPeriod(plan.adjustedOccurrence().after())
                )
                : null;

        return new ContractPlan(
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

    private static DateRange rangeOf(Contract contract) {
        return new DateRange(contract.getStartDate(), contract.getEndDate());
    }

    private static DateRange rangeOf(PresencePeriod presence) {
        return new DateRange(presence.startDate(), presence.endDate());
    }

    private static ContractPeriod toPeriod(DateRange range) {
        return new ContractPeriod(range.startDate(), range.endDate());
    }

    private static List<ContractPeriod> toPeriods(List<DateRange> ranges) {
        return ranges.stream().map(ContractTimelineService::toPeriod).toList();
    }
}
