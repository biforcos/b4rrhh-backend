package com.b4rrhh.employee.labor_classification.domain.model;

import java.time.LocalDate;

/**
 * A stretch of dates in the labor classification series as a plan or an
 * error names it: an occurrence (a labor classification is identified by the
 * day it starts, so its dates are all a plan needs to name it), a gap the
 * series would leave inside the presence, or the dates two occurrences would
 * share. An open end date means "onwards".
 */
public record LaborClassificationPeriod(
        LocalDate startDate,
        LocalDate endDate
) {

    public LaborClassificationPeriod {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }
    }
}
