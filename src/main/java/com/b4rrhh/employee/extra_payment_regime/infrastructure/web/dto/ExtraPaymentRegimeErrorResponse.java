package com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto;

import java.util.Map;

public record ExtraPaymentRegimeErrorResponse(
        String code,
        String message,
        Map<String, Object> details
) {
}