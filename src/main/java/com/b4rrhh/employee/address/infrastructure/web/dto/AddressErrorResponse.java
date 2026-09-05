package com.b4rrhh.employee.address.infrastructure.web.dto;

import java.util.Map;

/**
 * An error of the address API. {@code code} is what the screen branches on;
 * {@code details} carries what a plan rejection names (ADR-057): the gaps and
 * the neighbours to stretch, the shared dates, or the address a correction
 * would replace. {@code null} when the error has nothing to name.
 */
public record AddressErrorResponse(
        String code,
        String message,
        Map<String, Object> details
) {
}
