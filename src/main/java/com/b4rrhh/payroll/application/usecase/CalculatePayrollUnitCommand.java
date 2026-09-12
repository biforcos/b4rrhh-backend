package com.b4rrhh.payroll.application.usecase;

import java.time.LocalDate;

public record CalculatePayrollUnitCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String payrollPeriodCode,
        String payrollTypeCode,
        Integer presenceNumber,
        LocalDate periodStart,
        LocalDate periodEnd,
        String calculationEngineCode,
        String calculationEngineVersion,
        Long runId
) {
}