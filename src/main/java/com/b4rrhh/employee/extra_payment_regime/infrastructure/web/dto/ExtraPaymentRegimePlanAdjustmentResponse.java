package com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto;

/** The one existing extra payment regime a plan would move on its own: only its end date changes. */
public record ExtraPaymentRegimePlanAdjustmentResponse(
        Integer extraPaymentRegimeNumber,
        ExtraPaymentRegimePeriodResponse before,
        ExtraPaymentRegimePeriodResponse after
) {
}
