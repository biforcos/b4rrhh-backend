package com.b4rrhh.employee.address.domain.exception;

import com.b4rrhh.employee.address.domain.model.AddressPeriod;

import java.time.LocalDate;
import java.util.List;

/**
 * Two addresses of the same type would be in force on the same date
 * (ADR-057). The series is the one of the employee and that type: an address
 * of another type is never an overlap.
 */
public class AddressOverlapException extends RuntimeException {

    private final List<AddressPeriod> overlaps;

    public AddressOverlapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String addressTypeCode
    ) {
        this(ruleSystemCode, employeeTypeCode, employeeNumber, addressTypeCode, null, null, List.of());
    }

    public AddressOverlapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String addressTypeCode,
            LocalDate startDate,
            LocalDate endDate,
            List<AddressPeriod> overlaps
    ) {
        super("Address period overlaps for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ", addressTypeCode="
                + addressTypeCode
                + ", periodStart="
                + startDate
                + ", periodEnd="
                + endDate
                + ", overlaps="
                + overlaps);
        this.overlaps = List.copyOf(overlaps);
    }

    /** The stretches of dates the rejected address would share with existing ones of its type. */
    public List<AddressPeriod> overlaps() {
        return overlaps;
    }
}
