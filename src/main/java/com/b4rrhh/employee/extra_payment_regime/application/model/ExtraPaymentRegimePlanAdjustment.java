package com.b4rrhh.employee.extra_payment_regime.application.model;

import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegimePeriod;

/**
 * The one existing occurrence a plan moves on its own (ADR-057): closed the
 * day before a new one, or reopened when the one that closed it is removed.
 * Only its end date changes.
 */
public record ExtraPaymentRegimePlanAdjustment(
        Integer extraPaymentRegimeNumber,
        ExtraPaymentRegimePeriod before,
        ExtraPaymentRegimePeriod after
) {

    public ExtraPaymentRegimePlanAdjustment {
        if (extraPaymentRegimeNumber == null) {
            throw new IllegalArgumentException("extraPaymentRegimeNumber is required");
        }
        if (before == null || after == null) {
            throw new IllegalArgumentException("before and after are required");
        }
    }
}
