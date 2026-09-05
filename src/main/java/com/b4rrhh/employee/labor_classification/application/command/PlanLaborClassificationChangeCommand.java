package com.b4rrhh.employee.labor_classification.application.command;

import com.b4rrhh.employee.temporal.support.TimelineOperation;

import java.time.LocalDate;

/**
 * What the user intends to do to the series, so that the plan can be shown
 * before it is applied (ADR-057, decision 6). {@code laborClassificationStartDate}
 * identifies the occurrence to remove or correct (a labor classification is
 * identified by the day it starts); {@code startDate} and {@code endDate} are
 * the dates to add or the corrected ones.
 */
public record PlanLaborClassificationChangeCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        TimelineOperation operation,
        LocalDate laborClassificationStartDate,
        LocalDate startDate,
        LocalDate endDate
) {
}
