package com.b4rrhh.employee.address.domain.model;

import java.time.LocalDate;

/**
 * An occurrence of an address series as a plan names it: its number and its
 * dates. The number is {@code null} only for the occurrence a plan would add,
 * which has not been numbered yet.
 */
public record AddressOccurrence(
        Integer addressNumber,
        LocalDate startDate,
        LocalDate endDate
) {

    public AddressOccurrence {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }
    }
}
