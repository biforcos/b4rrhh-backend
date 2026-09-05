package com.b4rrhh.employee.labor_classification.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * {@code startDate} is the corrected start, omitted to keep the one in the
 * path; {@code endDate} is the corrected end, omitted for an occurrence that
 * stays open (ADR-057).
 */
public record UpdateLaborClassificationRequest(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
        String agreementCode,
        String agreementCategoryCode
) {
}
