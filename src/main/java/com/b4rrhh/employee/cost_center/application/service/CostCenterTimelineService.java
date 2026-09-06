package com.b4rrhh.employee.cost_center.application.service;

import com.b4rrhh.employee.cost_center.application.model.CostCenterDistributionPlan;
import com.b4rrhh.employee.cost_center.application.model.CostCenterDistributionPlanAdjustment;
import com.b4rrhh.employee.cost_center.application.port.CostCenterPresenceConsistencyPort;
import com.b4rrhh.employee.cost_center.application.port.PresencePeriod;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionCoverageGapException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionIsACorrectionException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionOverlapException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionPeriod;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
import com.b4rrhh.employee.cost_center.domain.service.CostCenterDistributionWindowGrouper;
import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.Timeline;
import com.b4rrhh.employee.temporal.support.TimelineCoverage;
import com.b4rrhh.employee.temporal.support.TimelinePlan;
import com.b4rrhh.employee.temporal.support.TimelinePlanner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Where the cost center series meets the temporal component (ADR-057). It
 * declares the coverage of the series as optional, builds the timeline from
 * the persisted lines and the employee's presence, asks the planner what an
 * operation would do, and gives the answer back in the vertical's own terms.
 *
 * <p>Optional coverage (ADR-057, decision 1) is what sets this series apart
 * from contract, working time, labor classification and work center: a
 * distribution is an analytical allocation, not a legal requirement nor
 * something the payroll needs, and an employee may simply not have one.
 * Overlaps are rejected as everywhere; a gap inside the presence is a legal
 * state, so a plan that leaves one comes back accepted and still names the
 * gap for the screen to show. It is the first series to use the weak
 * variant (backend#54).
 *
 * <p>What makes this series different from the others (ADR-057, decision 0)
 * is what an occurrence is: not a line but a <b>distribution window</b>, the
 * set of lines that share a start date. The timeline the planner sees has one
 * range per window, so two lines of the same window with different cost
 * centers are never an overlap, and closing or reopening a window moves every
 * line of it at once. A window has no number: its start date identifies it.
 *
 * <p>It plans and it judges; it never writes. The write use cases read the
 * plan and apply it, and the plan use case returns it as it is.
 */
@Component
public class CostCenterTimelineService {

    private static final TimelineCoverage COVERAGE = TimelineCoverage.OPTIONAL;

    private final CostCenterRepository costCenterRepository;
    private final CostCenterPresenceConsistencyPort costCenterPresenceConsistencyPort;
    private final CostCenterDistributionWindowGrouper windowGrouper;
    private final TimelinePlanner timelinePlanner = new TimelinePlanner();

    public CostCenterTimelineService(
            CostCenterRepository costCenterRepository,
            CostCenterPresenceConsistencyPort costCenterPresenceConsistencyPort,
            CostCenterDistributionWindowGrouper windowGrouper
    ) {
        this.costCenterRepository = costCenterRepository;
        this.costCenterPresenceConsistencyPort = costCenterPresenceConsistencyPort;
        this.windowGrouper = windowGrouper;
    }

    public CostCenterDistributionPlan planAdd(Long employeeId, DateRange occurrence) {
        return toPlan(timelinePlanner.planAdd(load(employeeId), occurrence));
    }

    public CostCenterDistributionPlan planRemove(Long employeeId, CostCenterDistributionWindow window) {
        return toPlan(timelinePlanner.planRemove(load(employeeId), rangeOf(window)));
    }

    public CostCenterDistributionPlan planCorrect(
            Long employeeId,
            CostCenterDistributionWindow window,
            DateRange corrected
    ) {
        return toPlan(timelinePlanner.planCorrect(load(employeeId), rangeOf(window), corrected));
    }

    /**
     * Throws the business exception a rejected plan stands for. Accepted
     * plans pass through. The switch is an expression on purpose: a rejection
     * the component adds and this vertical does not translate stops compiling
     * instead of slipping through (backend#58). With optional coverage the
     * planner never rejects for a gap, so the {@code GAP_NOT_ALLOWED} branch
     * is not reached from here today; it stays because the switch is
     * exhaustive and the translation is this vertical's to keep.
     */
    public void requireAccepted(
            CostCenterDistributionPlan plan,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        if (plan.isAccepted()) {
            return;
        }

        CostCenterDistributionPeriod occurrence = plan.occurrence();
        throw switch (plan.rejection()) {
            case OUTSIDE_PRESENCE -> new CostCenterOutsidePresencePeriodException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    occurrence.startDate(),
                    occurrence.endDate()
            );
            case OVERLAP -> new CostCenterDistributionOverlapException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    occurrence.startDate(),
                    occurrence.endDate(),
                    plan.overlaps()
            );
            case GAP_NOT_ALLOWED -> new CostCenterDistributionCoverageGapException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    plan.gaps(),
                    plan.stretchCandidates()
            );
            case IS_A_CORRECTION -> new CostCenterDistributionIsACorrectionException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    plan.correctedOccurrence(),
                    occurrence
            );
        };
    }

    private Timeline load(Long employeeId) {
        List<DateRange> windows = windowGrouper
                .group(costCenterRepository.findByEmployeeIdOrderByStartDate(employeeId))
                .getWindows()
                .stream()
                .map(CostCenterTimelineService::rangeOf)
                .toList();
        List<DateRange> presence = costCenterPresenceConsistencyPort
                .findPresencePeriodsByEmployeeIdOrderByStartDate(employeeId)
                .stream()
                .map(CostCenterTimelineService::rangeOf)
                .toList();

        return new Timeline(COVERAGE, presence, windows);
    }

    private static CostCenterDistributionPlan toPlan(TimelinePlan plan) {
        CostCenterDistributionPlanAdjustment adjustment = null;
        if (plan.adjustsAnOccurrence()) {
            adjustment = new CostCenterDistributionPlanAdjustment(
                    toPeriod(plan.adjustedOccurrence().before()),
                    toPeriod(plan.adjustedOccurrence().after())
            );
        }

        return new CostCenterDistributionPlan(
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

    private static List<CostCenterDistributionPeriod> toPeriods(List<DateRange> ranges) {
        return ranges.stream().map(CostCenterTimelineService::toPeriod).toList();
    }

    private static CostCenterDistributionPeriod toPeriod(DateRange range) {
        return new CostCenterDistributionPeriod(range.startDate(), range.endDate());
    }

    private static DateRange rangeOf(CostCenterDistributionWindow window) {
        return new DateRange(window.getStartDate(), window.getEndDate());
    }

    private static DateRange rangeOf(PresencePeriod presence) {
        return new DateRange(presence.startDate(), presence.endDate());
    }
}
