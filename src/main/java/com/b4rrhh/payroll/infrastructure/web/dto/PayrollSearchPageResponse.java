package com.b4rrhh.payroll.infrastructure.web.dto;

import java.util.List;

/**
 * Una página de la búsqueda de recibos ({@code b4rrhh/frontend#93}), con la forma de la del
 * directorio de empleados: {@code total} cuenta todos los que cumplen los filtros, no los de la
 * página.
 */
public record PayrollSearchPageResponse(
        List<PayrollSummaryResponse> items,
        int page,
        int size,
        long total
) {}
