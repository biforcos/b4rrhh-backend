package com.b4rrhh.payroll.infrastructure.web.dto;

public record BulkFinalizePayrollResponse(
        String ruleSystemCode,
        String payrollPeriodCode,
        String payrollTypeCode,
        int totalCandidates,
        int totalFound,
        int totalFinalized,
        int totalSkippedAlreadyDefinitive,
        int totalSkippedNotEligibleByStatus,
        int totalSkippedNotFound
) {
}
