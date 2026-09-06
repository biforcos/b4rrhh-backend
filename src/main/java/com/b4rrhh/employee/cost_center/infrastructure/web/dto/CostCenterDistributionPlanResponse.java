package com.b4rrhh.employee.cost_center.infrastructure.web.dto;

import java.util.List;

/**
 * What would happen to the series if the change were applied (ADR-057).
 * {@code rejection} is null when the plan is accepted; {@code adjustedOccurrence}
 * is null when nothing else would move; {@code correctedOccurrence} is the
 * window a correction replaces, null on an add and on a removal.
 */
public record CostCenterDistributionPlanResponse(
        String operation,
        boolean accepted,
        String rejection,
        CostCenterDistributionPeriodResponse occurrence,
        CostCenterDistributionPeriodResponse correctedOccurrence,
        CostCenterDistributionPlanAdjustmentResponse adjustedOccurrence,
        List<CostCenterDistributionPeriodResponse> overlaps,
        List<CostCenterDistributionPeriodResponse> gaps,
        List<CostCenterDistributionPeriodResponse> stretchCandidates,
        List<CostCenterDistributionPeriodResponse> projected
) {
}
