package com.b4rrhh.employee.extra_payment_regime.application.port;

public record EmployeeExtraPaymentRegimeContext(
        Long employeeId,
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber
) {
}