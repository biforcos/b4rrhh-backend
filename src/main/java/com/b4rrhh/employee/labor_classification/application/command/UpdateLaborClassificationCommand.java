package com.b4rrhh.employee.labor_classification.application.command;

import java.time.LocalDate;

/**
 * Corrects the labor classification that starts on {@code startDate}.
 * {@code newStartDate} is the corrected start, or {@code null} to keep it;
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
