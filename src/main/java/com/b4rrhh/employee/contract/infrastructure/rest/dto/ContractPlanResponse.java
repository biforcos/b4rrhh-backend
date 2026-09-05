package com.b4rrhh.employee.contract.infrastructure.rest.dto;

import java.util.List;

/**
 * What would happen to the series if the change were applied (ADR-057).
 * {@code rejection} is null when the plan is accepted; {@code adjustedOccurrence}
 * is null when nothing else would move; {@code correctedOccurrence} is the
 * contract a correction replaces, null on an add and on a removal. Every
 * occurrence is named by its dates: a contract is identified by its start.
 */
public record ContractPlanResponse(
        String operation,
        boolean accepted,
        String rejection,
        ContractPeriodResponse occurrence,
        ContractPeriodResponse correctedOccurrence,
        ContractPlanAdjustmentResponse adjustedOccurrence,
        List<ContractPeriodResponse> overlaps,
        List<ContractPeriodResponse> gaps,
        List<ContractPeriodResponse> stretchCandidates,
        List<ContractPeriodResponse> projected
) {
}
