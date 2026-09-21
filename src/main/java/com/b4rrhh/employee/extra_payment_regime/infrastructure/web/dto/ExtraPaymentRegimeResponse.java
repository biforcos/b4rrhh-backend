package com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto;

import java.time.LocalDate;

public record ExtraPaymentRegimeResponse(
        Integer extraPaymentRegimeNumber,
        LocalDate startDate,
        LocalDate endDate,
        boolean prorated
) {
}
