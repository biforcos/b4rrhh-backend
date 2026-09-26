package com.b4rrhh.payroll.retro.application.usecase;

public record ListEmployeeRetroMarksCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber
) {
}
