package com.b4rrhh.employee.extra_payment_regime.domain.model;

import java.time.LocalDate;

/**
 * A stretch of dates in the extra payment regime series: a gap the series leaves
 * inside the presence, or the dates two occurrences would share. An open
 * end date means "onwards".
 */
public record ExtraPaymentRegimePeriod(
        LocalDate startDate,
        LocalDate endDate
) {

    public ExtraPaymentRegimePeriod {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }
    }
}
