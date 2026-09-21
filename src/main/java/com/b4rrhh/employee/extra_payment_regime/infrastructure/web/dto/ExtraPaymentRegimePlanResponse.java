package com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto;

import java.util.List;

/**
 * What would happen to the series if the change were applied (ADR-057).
 * {@code rejection} is null when the plan is accepted; {@code adjustedOccurrence}
 * is null when nothing else would move; {@code correctedOccurrence} is the
 * extra payment regime a correction replaces, null on an add and on a removal.
 */
public record ExtraPaymentRegimePlanResponse(
        String operation,
        boolean accepted,
        String rejection,
        ExtraPaymentRegimeOccurrenceResponse occurrence,
        ExtraPaymentRegimeOccurrenceResponse correctedOccurrence,
        ExtraPaymentRegimePlanAdjustmentResponse adjustedOccurrence,
        List<ExtraPaymentRegimePeriodResponse> overlaps,
        List<ExtraPaymentRegimePeriodResponse> gaps,
        List<ExtraPaymentRegimeOccurrenceResponse> stretchCandidates,
        List<ExtraPaymentRegimeOccurrenceResponse> projected
) {
}
