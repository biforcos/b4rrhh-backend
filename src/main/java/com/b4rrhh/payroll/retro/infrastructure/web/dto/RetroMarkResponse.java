package com.b4rrhh.payroll.retro.infrastructure.web.dto;

import java.time.Instant;

public record RetroMarkResponse(
        Long id,
        Integer presenceNumber,
        String fromPeriodCode,
        String status,
        Instant createdAt,
        String sourceVerticalCode,
        String sourceTable,
        Long sourceRowId,
        String sourceRowKey,
        Instant discardedAt,
        String discardedBy,
        String discardReason,
        Instant consumedAt,
        String consumedPeriodCode,
        Long consumedRunId
) {
}
