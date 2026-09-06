package com.b4rrhh.employee.workcenter.application.usecase;

import com.b4rrhh.employee.temporal.support.TimelineOperation;

import java.time.LocalDate;

/**
 * What the user intends to do to the series, so that the plan can be shown
 * before it is applied (ADR-057, decision 6). {@code workCenterAssignmentNumber}
 * names the assignment to remove or correct; {@code startDate} and
 * {@code endDate} are the dates to add or the corrected ones.
 */
public record PlanWorkCenterChangeCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        TimelineOperation operation,
        Integer workCenterAssignmentNumber,
        LocalDate startDate,
        LocalDate endDate
) {
}
