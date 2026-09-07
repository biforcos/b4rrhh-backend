package com.b4rrhh.employee.labor_classification.application.command;

import java.time.LocalDate;

/**
 * Corrects the labor classification that starts on {@code startDate}.
 * {@code newStartDate} is the start the occurrence has after the correction
 * and is required: leaving it where it is means sending the same date again,
 * not leaving it out (backend#69).
 * {@code endDate} is the corrected end as it should be, {@code null} for an
 * occurrence that stays open (ADR-057: stretching or shrinking an occurrence
 * is the user's act).
 */
public record UpdateLaborClassificationCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        LocalDate startDate,
        LocalDate newStartDate,
        LocalDate endDate,
        String agreementCode,
        String agreementCategoryCode
) {
}
