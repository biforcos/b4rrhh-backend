package com.b4rrhh.employee.cost_center.infrastructure.web.dto;

/** The one existing window a plan would move on its own: only its end date changes, for every line of it. */
public record CostCenterDistributionPlanAdjustmentResponse(
        CostCenterDistributionPeriodResponse before,
        CostCenterDistributionPeriodResponse after
) {
}
