package com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto;

import java.time.LocalDate;

/** A stretch of dates named by a plan or an error: a gap, or an overlap. Open end date means onwards. */
public record ExtraPaymentRegimePeriodResponse(
        LocalDate startDate,
        LocalDate endDate
) {
}
