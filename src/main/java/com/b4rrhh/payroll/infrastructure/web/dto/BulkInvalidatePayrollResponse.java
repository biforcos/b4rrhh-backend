package com.b4rrhh.payroll.infrastructure.web.dto;

public record BulkInvalidatePayrollResponse(
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
