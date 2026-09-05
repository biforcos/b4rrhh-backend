package com.b4rrhh.employee.address.domain.exception;

import com.b4rrhh.employee.address.domain.model.AddressOccurrence;
import com.b4rrhh.employee.address.domain.model.AddressPeriod;

import java.util.List;

/**
 * The resulting series would leave a stretch of the employee's presence
 * without an address of a type whose coverage is mandatory (ADR-057,
 * backend#53): the domicile. It names the gaps and the neighbouring
 * addresses the user could stretch to cover them.
 */
public class AddressCoverageGapException extends RuntimeException {

    private final String addressTypeCode;
    private final List<AddressPeriod> gaps;
    private final List<AddressOccurrence> stretchCandidates;

    public AddressCoverageGapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String addressTypeCode,
            List<AddressPeriod> gaps,
            List<AddressOccurrence> stretchCandidates
    ) {
        super("Address series would leave the presence uncovered for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ", addressTypeCode="
                + addressTypeCode
                + ", gaps="
                + gaps
                + ", stretchCandidates="
                + stretchCandidates);
        this.addressTypeCode = addressTypeCode;
        this.gaps = List.copyOf(gaps);
        this.stretchCandidates = List.copyOf(stretchCandidates);
    }

    public String addressTypeCode() {
        return addressTypeCode;
    }

    public List<AddressPeriod> gaps() {
        return gaps;
    }

    public List<AddressOccurrence> stretchCandidates() {
        return stretchCandidates;
    }
}
