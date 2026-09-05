package com.b4rrhh.employee.contract.infrastructure.rest.dto;

import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * What the user intends to do, so the plan can be shown before confirming.
 * {@code contractStartDate} identifies the contract for REMOVE and CORRECT (a
 * contract is identified by the day it starts); {@code startDate} and
 * {@code endDate} are the dates for ADD and CORRECT.
 */
public record PlanContractChangeRequest(
        TimelineOperation operation,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate contractStartDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate
) {
}
