package com.b4rrhh.employee.address.application.usecase;

public record DeleteAddressCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        Integer addressNumber
) {
}
