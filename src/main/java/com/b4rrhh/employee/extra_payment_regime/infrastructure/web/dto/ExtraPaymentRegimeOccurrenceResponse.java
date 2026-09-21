package com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto;

import java.time.LocalDate;

/** An occurrence as a plan or an error names it. The number is null only for the one a plan would add. */
public record ExtraPaymentRegimeOccurrenceResponse(
        Integer extraPaymentRegimeNumber,
        LocalDate startDate,
        LocalDate endDate
) {
}
