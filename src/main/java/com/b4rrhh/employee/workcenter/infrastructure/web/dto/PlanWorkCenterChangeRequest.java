package com.b4rrhh.employee.workcenter.infrastructure.web.dto;

import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * What the user intends to do, so the plan can be shown before confirming.
 * {@code workCenterAssignmentNumber} for REMOVE and CORRECT; {@code startDate}
 * and {@code endDate} for ADD and CORRECT.
 */
public record PlanWorkCenterChangeRequest(
        TimelineOperation operation,
        Integer workCenterAssignmentNumber,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate
) {
}
