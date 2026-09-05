package com.b4rrhh.employee.labor_classification.infrastructure.rest.dto;

/** The one existing labor classification a plan would move on its own: only its end date changes. */
public record LaborClassificationPlanAdjustmentResponse(
        LaborClassificationPeriodResponse before,
        LaborClassificationPeriodResponse after
) {
}
