package com.b4rrhh.employee.labor_classification.domain.exception;

import com.b4rrhh.employee.labor_classification.domain.model.LaborClassificationPeriod;

import java.time.LocalDate;
import java.util.List;

public class LaborClassificationOverlapException extends RuntimeException {

    private final List<LaborClassificationPeriod> overlaps;

    public LaborClassificationOverlapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this(ruleSystemCode, employeeTypeCode, employeeNumber, startDate, endDate, List.of());
    }

    public LaborClassificationOverlapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate startDate,
            LocalDate endDate,
            List<LaborClassificationPeriod> overlaps
    ) {
        super("Labor classification period overlaps for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ", periodStart="
                + startDate
                + ", periodEnd="
                + endDate
                + ", overlaps="
                + overlaps);
        this.overlaps = List.copyOf(overlaps);
    }

    /** The stretches of dates the rejected occurrence would share with existing ones (ADR-057). */
    public List<LaborClassificationPeriod> overlaps() {
        return overlaps;
    }
}
