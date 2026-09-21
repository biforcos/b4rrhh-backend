package com.b4rrhh.employee.extra_payment_regime.application.usecase;

public record DeleteExtraPaymentRegimeCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        Integer extraPaymentRegimeNumber
) {
}
