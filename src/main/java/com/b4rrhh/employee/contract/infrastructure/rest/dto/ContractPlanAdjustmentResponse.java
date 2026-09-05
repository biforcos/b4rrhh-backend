package com.b4rrhh.employee.contract.infrastructure.rest.dto;

/** The one existing contract a plan would move on its own: only its end date changes. */
public record ContractPlanAdjustmentResponse(
        ContractPeriodResponse before,
        ContractPeriodResponse after
) {
}
