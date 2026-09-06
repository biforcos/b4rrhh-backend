package com.b4rrhh.employee.cost_center.application.model;

import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionPeriod;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;

import java.util.List;

/**
 * What would happen to the employee's cost center series if an operation
 * were applied (ADR-057). Every occurrence here is a distribution window
 * named by its dates: the set of lines that share a start date is the unit
 * the invariants judge, never a line on its own. It is what the screen shows
 * before confirming and what the write use cases apply; it is never
 * persisted.
 *
 * <ul>
 *   <li>{@link #occurrence()} is the window added, removed or corrected,
 *       under its new dates on a correction.</li>
 *   <li>{@link #correctedOccurrence()} is, on a correction, the window as it
 *       stands today, the one {@link #occurrence()} replaces. It is also how
 *       the plan tells the screen that an add on an existing start date is
 *       the correction of that window (backend#52). {@code null} on an add
 *       and on a removal.</li>
 *   <li>{@link #adjustedOccurrence()} is the only automatic consequence, or
 *       {@code null} when nothing else moves.</li>
 *   <li>{@link #stretchCandidates()} are the neighbours of the gaps: the
 *       windows the user could stretch. They are named, never moved.</li>
 *   <li>{@link #projected()} is the series as it would be, accepted or not.</li>
 * </ul>
 */
public record CostCenterDistributionPlan(
        TimelineOperation operation,
        TimelineRejection rejection,
        CostCenterDistributionPeriod occurrence,
        CostCenterDistributionPeriod correctedOccurrence,
        CostCenterDistributionPlanAdjustment adjustedOccurrence,
        List<CostCenterDistributionPeriod> overlaps,
        List<CostCenterDistributionPeriod> gaps,
        List<CostCenterDistributionPeriod> stretchCandidates,
        List<CostCenterDistributionPeriod> projected
) {

    public CostCenterDistributionPlan {
        if (operation == null) {
            throw new IllegalArgumentException("operation is required");
        }
        if (occurrence == null) {
            throw new IllegalArgumentException("occurrence is required");
        }
        overlaps = List.copyOf(overlaps);
        gaps = List.copyOf(gaps);
        stretchCandidates = List.copyOf(stretchCandidates);
        projected = List.copyOf(projected);
    }

    public boolean isAccepted() {
        return rejection == null;
    }

    public boolean adjustsAnOccurrence() {
        return adjustedOccurrence != null;
    }
}
