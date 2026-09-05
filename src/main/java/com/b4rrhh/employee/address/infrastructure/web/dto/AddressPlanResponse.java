package com.b4rrhh.employee.address.infrastructure.web.dto;

import java.util.List;

/**
 * What would happen to the address series of {@code addressTypeCode} if the
 * change were applied (ADR-057). {@code rejection} is null when the plan is
 * accepted; {@code adjustedOccurrence} is null when nothing else would move;
 * {@code correctedOccurrence} is the address a correction replaces, null on
 * an add and on a removal.
 */
public record AddressPlanResponse(
        String operation,
        boolean accepted,
        String rejection,
        String addressTypeCode,
        AddressOccurrenceResponse occurrence,
        AddressOccurrenceResponse correctedOccurrence,
        AddressPlanAdjustmentResponse adjustedOccurrence,
        List<AddressPeriodResponse> overlaps,
        List<AddressPeriodResponse> gaps,
        List<AddressOccurrenceResponse> stretchCandidates,
        List<AddressOccurrenceResponse> projected
) {
}
