package com.b4rrhh.employee.cost_center.infrastructure.web.dto;

import java.util.Map;

/**
 * {@code code} is what the screen maps; {@code details} is what a plan
 * rejection names (ADR-057): the shared dates, the gap and the neighbours to
 * stretch, or the window an add would correct. Null when there is nothing to
 * name.
 */
public record CostCenterErrorResponse(
        String code,
        String message,
        Map<String, Object> details
) {
}
