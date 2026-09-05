package com.b4rrhh.employee.address.infrastructure.web.dto;

import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * What the user intends to do, so the plan can be shown before confirming.
 * {@code addressTypeCode} for ADD; {@code addressNumber} for REMOVE and
 * CORRECT; {@code startDate} and {@code endDate} for ADD and CORRECT.
 */
public record PlanAddressChangeRequest(
        TimelineOperation operation,
        String addressTypeCode,
        Integer addressNumber,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate
) {
}
