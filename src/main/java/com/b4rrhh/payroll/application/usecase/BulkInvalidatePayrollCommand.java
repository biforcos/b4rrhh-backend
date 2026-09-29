package com.b4rrhh.payroll.application.usecase;

public record BulkInvalidatePayrollCommand(
        String ruleSystemCode,
        String payrollPeriodCode,
        String payrollTypeCode,
        PayrollLaunchTargetSelection targetSelection
) {
}
