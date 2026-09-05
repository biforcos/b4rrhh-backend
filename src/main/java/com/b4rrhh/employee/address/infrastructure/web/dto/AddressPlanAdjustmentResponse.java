package com.b4rrhh.employee.address.infrastructure.web.dto;

/** The one existing address a plan would move on its own: only its end date changes. */
public record AddressPlanAdjustmentResponse(
        Integer addressNumber,
        AddressPeriodResponse before,
        AddressPeriodResponse after
) {
}
