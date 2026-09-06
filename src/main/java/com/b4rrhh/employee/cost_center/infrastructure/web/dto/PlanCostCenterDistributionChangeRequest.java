package com.b4rrhh.employee.cost_center.infrastructure.web.dto;

import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * What the user intends to do, so the plan can be shown before confirming.
 * {@code windowStartDate} names the window for REMOVE and CORRECT;
 * {@code startDate} and {@code endDate} are the dates for ADD and CORRECT.
 */
public record PlanCostCenterDistributionChangeRequest(
        TimelineOperation operation,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate windowStartDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate
) {
}
