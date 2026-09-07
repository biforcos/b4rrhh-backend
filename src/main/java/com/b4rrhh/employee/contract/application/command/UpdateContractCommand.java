package com.b4rrhh.employee.contract.application.command;

import java.time.LocalDate;

/**
 * Corrects the contract that starts on {@code startDate}. {@code newStartDate}
 * is the start the contract has after the correction and is required: leaving
 * it where it is means sending the same date again, not leaving it out
 * (backend#69). {@code endDate} is the corrected end as it should be,
 * {@code null} for a contract that stays open (ADR-057: stretching or
 * shrinking a contract is the user's act).
 */
public record UpdateContractCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        LocalDate startDate,
        LocalDate newStartDate,
        LocalDate endDate,
        String contractCode,
        String contractSubtypeCode
) {
}
