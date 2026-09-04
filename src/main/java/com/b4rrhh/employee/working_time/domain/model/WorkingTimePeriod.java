package com.b4rrhh.employee.working_time.domain.model;

import java.time.LocalDate;

/**
 * A stretch of dates in the working time series: a gap the series leaves
 * inside the presence, or the dates two occurrences would share. An open
 * end date means "onwards".
 */
public record WorkingTimePeriod(
        LocalDate startDate,
        LocalDate endDate
) {

    public WorkingTimePeriod {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }
    }
}
