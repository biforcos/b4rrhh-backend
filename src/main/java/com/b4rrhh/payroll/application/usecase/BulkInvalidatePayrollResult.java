package com.b4rrhh.payroll.application.usecase;

public record BulkInvalidatePayrollResult(
        String ruleSystemCode,
        String payrollPeriodCode,
        String payrollTypeCode,
        int totalCandidates,
        int totalFound,
        int totalInvalidated,
        int totalSkippedAlreadyNotValid,
        int totalSkippedProtected,
        int totalSkippedNotFound
) {
}
