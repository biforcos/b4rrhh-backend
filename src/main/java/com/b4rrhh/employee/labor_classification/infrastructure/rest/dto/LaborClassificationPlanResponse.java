package com.b4rrhh.employee.labor_classification.infrastructure.rest.dto;

import java.util.List;

/**
 * What would happen to the series if the change were applied (ADR-057).
 * {@code rejection} is null when the plan is accepted; {@code adjustedOccurrence}
 * is null when nothing else would move; {@code correctedOccurrence} is the
 * occurrence a correction replaces, null on an add and on a removal. Every
 * occurrence is named by its dates: a labor classification is identified by
 * its start.
 */
public record LaborClassificationPlanResponse(
        String operation,
        boolean accepted,
        String rejection,
        LaborClassificationPeriodResponse occurrence,
        LaborClassificationPeriodResponse correctedOccurrence,
        LaborClassificationPlanAdjustmentResponse adjustedOccurrence,
        List<LaborClassificationPeriodResponse> overlaps,
        List<LaborClassificationPeriodResponse> gaps,
        List<LaborClassificationPeriodResponse> stretchCandidates,
        List<LaborClassificationPeriodResponse> projected
) {
}
