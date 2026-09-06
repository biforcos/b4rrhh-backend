package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.temporal.support.TimelineOperation;

import java.time.LocalDate;

/**
 * What the user intends to do to the series, so that the plan can be shown
 * before it is applied (ADR-057, decision 6). {@code windowStartDate} names
 * the window to remove or correct, by the day it starts; {@code startDate}
 * and {@code endDate} are the dates to add or the corrected ones.
 */
public record PlanCostCenterDistributionChangeCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        TimelineOperation operation,
        LocalDate windowStartDate,
        LocalDate startDate,
        LocalDate endDate
) {
}
