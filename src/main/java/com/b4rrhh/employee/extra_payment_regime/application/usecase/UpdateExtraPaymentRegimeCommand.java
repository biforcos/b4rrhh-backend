package com.b4rrhh.employee.extra_payment_regime.application.usecase;

import java.time.LocalDate;

public record UpdateExtraPaymentRegimeCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        Integer extraPaymentRegimeNumber,
        LocalDate startDate,
        LocalDate endDate,
        Boolean prorated
) {
}
