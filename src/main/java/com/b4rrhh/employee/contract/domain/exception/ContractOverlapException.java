package com.b4rrhh.employee.contract.domain.exception;

import com.b4rrhh.employee.contract.domain.model.ContractPeriod;

import java.time.LocalDate;
import java.util.List;

public class ContractOverlapException extends RuntimeException {

    private final List<ContractPeriod> overlaps;

    public ContractOverlapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this(ruleSystemCode, employeeTypeCode, employeeNumber, startDate, endDate, List.of());
    }

    public ContractOverlapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate startDate,
            LocalDate endDate,
            List<ContractPeriod> overlaps
    ) {
        super("Contract period overlaps for ruleSystemCode="
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

    /** The stretches of dates the rejected contract would share with existing ones (ADR-057). */
    public List<ContractPeriod> overlaps() {
        return overlaps;
    }
}
