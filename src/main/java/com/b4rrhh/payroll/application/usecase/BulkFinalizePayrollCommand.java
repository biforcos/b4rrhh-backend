package com.b4rrhh.payroll.application.usecase;

public record BulkFinalizePayrollCommand(
        String ruleSystemCode,
        String payrollPeriodCode,
        String payrollTypeCode,
        PayrollLaunchTargetSelection targetSelection
) {
}
