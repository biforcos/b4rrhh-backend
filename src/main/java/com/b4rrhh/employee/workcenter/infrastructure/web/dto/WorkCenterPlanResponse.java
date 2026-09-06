package com.b4rrhh.employee.workcenter.infrastructure.web.dto;

import java.util.List;

/**
 * What would happen to the series if the change were applied (ADR-057).
 * {@code rejection} is null when the plan is accepted; {@code adjustedOccurrence}
 * is null when nothing else would move; {@code correctedOccurrence} is the
 * assignment a correction replaces, null on an add and on a removal.
 */
public record WorkCenterPlanResponse(
        String operation,
        boolean accepted,
        String rejection,
        WorkCenterOccurrenceResponse occurrence,
        WorkCenterOccurrenceResponse correctedOccurrence,
        WorkCenterPlanAdjustmentResponse adjustedOccurrence,
        List<WorkCenterPeriodResponse> overlaps,
        List<WorkCenterPeriodResponse> gaps,
        List<WorkCenterOccurrenceResponse> stretchCandidates,
        List<WorkCenterOccurrenceResponse> projected
) {
}
