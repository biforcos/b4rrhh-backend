package com.b4rrhh.employee.address.domain.model;

import java.time.LocalDate;

/**
 * A stretch of dates in an address series: a gap the series leaves inside
 * the presence, or the dates two addresses of the same type would share. An
 * open end date means "onwards".
 */
public record AddressPeriod(
        LocalDate startDate,
        LocalDate endDate
) {

    public AddressPeriod {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }
    }
}
