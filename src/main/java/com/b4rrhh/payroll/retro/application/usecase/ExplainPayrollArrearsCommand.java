package com.b4rrhh.payroll.retro.application.usecase;

public record ExplainPayrollArrearsCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String payrollPeriodCode,
        String payrollTypeCode,
        Integer presenceNumber
) {
}
