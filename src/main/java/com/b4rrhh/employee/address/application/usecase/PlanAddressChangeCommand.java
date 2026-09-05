package com.b4rrhh.employee.address.application.usecase;

import com.b4rrhh.employee.temporal.support.TimelineOperation;

import java.time.LocalDate;

/**
 * What the user intends to do to an address series, so that the plan can be
 * shown before it is applied (ADR-057, decision 6). {@code addressTypeCode}
 * names the series an ADD goes into; {@code addressNumber} names the address
 * to REMOVE or CORRECT, whose type is the series; {@code startDate} and
 * {@code endDate} are the dates to add or the corrected ones.
 */
public record PlanAddressChangeCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        TimelineOperation operation,
        String addressTypeCode,
        Integer addressNumber,
        LocalDate startDate,
        LocalDate endDate
) {
}
