package com.b4rrhh.payroll.infrastructure.web.dto;

public record BulkFinalizePayrollRequest(
        String ruleSystemCode,
        String payrollPeriodCode,
        String payrollTypeCode,
        PayrollLaunchTargetSelectionRequest targetSelection
) {
}
