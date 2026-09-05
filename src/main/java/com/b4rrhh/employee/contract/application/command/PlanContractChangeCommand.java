package com.b4rrhh.employee.contract.application.command;

import com.b4rrhh.employee.temporal.support.TimelineOperation;

import java.time.LocalDate;

/**
 * What the user intends to do to the series, so that the plan can be shown
 * before it is applied (ADR-057, decision 6). {@code contractStartDate}
 * identifies the contract to remove or correct (a contract is identified by
 * the day it starts); {@code startDate} and {@code endDate} are the dates to
 * add or the corrected ones.
 */
public record PlanContractChangeCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        TimelineOperation operation,
        LocalDate contractStartDate,
        LocalDate startDate,
        LocalDate endDate
) {
}
