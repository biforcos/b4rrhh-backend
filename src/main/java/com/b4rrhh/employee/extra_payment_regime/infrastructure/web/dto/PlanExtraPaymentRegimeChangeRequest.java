package com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto;

import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * What the user intends to do, so the plan can be shown before confirming.
 * {@code extraPaymentRegimeNumber} for REMOVE and CORRECT; {@code startDate} and
 * {@code endDate} for ADD and CORRECT.
 */
public record PlanExtraPaymentRegimeChangeRequest(
        TimelineOperation operation,
        Integer extraPaymentRegimeNumber,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate
) {
}
