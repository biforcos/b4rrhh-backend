package com.b4rrhh.employee.extra_payment_regime.application.usecase;

import java.time.LocalDate;

public record CloseExtraPaymentRegimeCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        Integer extraPaymentRegimeNumber,
        LocalDate endDate
) {
}