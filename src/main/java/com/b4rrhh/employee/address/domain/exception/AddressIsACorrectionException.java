package com.b4rrhh.employee.address.domain.exception;

import com.b4rrhh.employee.address.domain.model.AddressOccurrence;
import com.b4rrhh.employee.address.domain.model.AddressPeriod;

/**
 * What was asked for as an add is not an add (ADR-057, backend#58): the new
 * address starts on the start date of an existing one of the same type, so it
 * would correct that one, not add a second one. Nothing is applied. It names
 * the address that would be corrected so the user can ask for the correction
 * as such.
 */
public class AddressIsACorrectionException extends RuntimeException {

    private final AddressOccurrence correctedOccurrence;
    private final AddressPeriod requested;

    public AddressIsACorrectionException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String addressTypeCode,
            AddressOccurrence correctedOccurrence,
            AddressPeriod requested
    ) {
        super("Adding a " + addressTypeCode + " address from "
                + requested.startDate()
                + " to "
                + requested.endDate()
                + " is not an add for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ": it starts on the start date of address #"
                + correctedOccurrence.addressNumber()
                + " ("
                + correctedOccurrence.startDate()
                + " to "
                + correctedOccurrence.endDate()
                + ") and would correct it. Ask for it as a correction of that address.");
        this.correctedOccurrence = correctedOccurrence;
        this.requested = requested;
    }

    /** The existing address, with its current dates, that the request would correct. */
    public AddressOccurrence correctedOccurrence() {
        return correctedOccurrence;
    }

    /** The dates that were asked for as an add. */
    public AddressPeriod requested() {
        return requested;
    }
}
