package com.b4rrhh.employee.labor_classification.infrastructure.rest.dto;

import java.time.LocalDate;

/**
 * A stretch of dates named by a plan or an error: a labor classification
 * (identified by its start date), a gap, or an overlap. Open end date means
 * onwards.
 */
public record LaborClassificationPeriodResponse(
        LocalDate startDate,
        LocalDate endDate
) {
}
