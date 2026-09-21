package com.b4rrhh.employee.extra_payment_regime.application.usecase;

import com.b4rrhh.employee.temporal.support.TimelineOperation;

import java.time.LocalDate;

/**
 * What the user intends to do to the series, so that the plan can be shown
 * before it is applied (ADR-057, decision 6). {@code extraPaymentRegimeNumber}
 * names the occurrence to remove or correct; {@code startDate} and
 * {@code endDate} are the dates to add or the corrected ones.
 */
public record PlanExtraPaymentRegimeChangeCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        TimelineOperation operation,
        Integer extraPaymentRegimeNumber,
        LocalDate startDate,
        LocalDate endDate
) {
}
