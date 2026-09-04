package com.b4rrhh.employee.working_time.infrastructure.web.dto;

import java.util.List;

/**
 * What would happen to the series if the change were applied (ADR-057).
 * {@code rejection} is null when the plan is accepted; {@code adjustedOccurrence}
 * is null when nothing else would move.
 */
public record WorkingTimePlanResponse(
        String operation,
        boolean accepted,
        String rejection,
        WorkingTimeOccurrenceResponse occurrence,
        WorkingTimePlanAdjustmentResponse adjustedOccurrence,
        List<WorkingTimePeriodResponse> overlaps,
        List<WorkingTimePeriodResponse> gaps,
        List<WorkingTimeOccurrenceResponse> stretchCandidates,
        List<WorkingTimeOccurrenceResponse> projected
) {
}
