package com.b4rrhh.employee.extra_payment_regime.domain.exception;

import java.time.LocalDate;

public final class ExtraPaymentRegimeOutsidePresencePeriodException extends ExtraPaymentRegimeSeriesInvariantException {

    public ExtraPaymentRegimeOutsidePresencePeriodException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
        super("Extra payment regime period is outside employee presence history for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ", startDate="
                + startDate
                + ", endDate="
                + endDate);
    }
}