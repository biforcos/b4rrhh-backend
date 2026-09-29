package com.b4rrhh.payroll.year.application.usecase;

public record GetEmployeeYearCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        int year
) {
}
