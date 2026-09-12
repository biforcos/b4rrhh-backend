package com.b4rrhh.payroll.infrastructure.web.dto;

import java.time.LocalDateTime;

public record PayrollCalculationRunResponse(
        Long runId,
        String status,
        String ruleSystemCode,
        String payrollPeriodCode,
        String payrollTypeCode,
        String calculationEngineCode,
        String calculationEngineVersion,
        Integer totalCandidates,
        Integer totalEligible,
        Integer totalClaimed,
        Integer totalSkippedNotEligible,
        Integer totalSkippedAlreadyClaimed,
        Integer totalCalculated,
        Integer totalNotValid,
        Integer totalErrors,
        LocalDateTime requestedAt,
        String requestedBy,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}