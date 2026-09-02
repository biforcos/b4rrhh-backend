package com.b4rrhh.employee.labor_classification.infrastructure.rest.dto;

import java.time.LocalDate;

public record LaborClassificationResponse(
        String agreementCode,
        String agreementName,
        String agreementCategoryCode,
        String agreementCategoryName,
        String grupoCotizacionCode,
        String grupoCotizacionName,
        LocalDate startDate,
        LocalDate endDate
) {
}
