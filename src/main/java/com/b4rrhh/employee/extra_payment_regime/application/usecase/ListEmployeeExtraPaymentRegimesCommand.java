package com.b4rrhh.employee.extra_payment_regime.application.usecase;

public record ListEmployeeExtraPaymentRegimesCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber
) {
}