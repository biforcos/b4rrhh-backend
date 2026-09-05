package com.b4rrhh.employee.contract.infrastructure.rest.dto;

import java.util.Map;

/**
 * An error of the contract API. {@code message} is what the screen showed
 * until now and keeps its shape; {@code code} is what a screen can branch
 * on, and {@code details} carries what a plan rejection names (ADR-057): the
 * gaps and the neighbours to stretch, the shared dates, or the contract a
 * correction would replace. {@code null} when the error has nothing to name.
 */
public record ContractErrorResponse(
        String code,
        String message,
        Map<String, Object> details
) {
}
