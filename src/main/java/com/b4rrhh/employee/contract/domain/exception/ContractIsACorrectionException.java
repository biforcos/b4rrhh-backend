package com.b4rrhh.employee.contract.domain.exception;

import com.b4rrhh.employee.contract.domain.model.ContractPeriod;

/**
 * What was asked for as an add is not an add (ADR-057, backend#52): the new
 * contract starts on the start date of an existing one, so it would correct
 * that one, not add a second one. Nothing is applied. It names the contract
 * that would be corrected so the user can ask for the correction as such.
 * This is what the old {@code ReplaceMode.EXACT_START} did silently.
 */
public class ContractIsACorrectionException extends RuntimeException {

    private final ContractPeriod correctedOccurrence;
    private final ContractPeriod requested;

    public ContractIsACorrectionException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            ContractPeriod correctedOccurrence,
            ContractPeriod requested
    ) {
        super("Adding a contract from "
                + requested.startDate()
                + " to "
                + requested.endDate()
                + " is not an add for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ": it starts on the start date of the contract from "
                + correctedOccurrence.startDate()
                + " to "
                + correctedOccurrence.endDate()
                + " and would correct it. Ask for it as a correction of that contract.");
        this.correctedOccurrence = correctedOccurrence;
        this.requested = requested;
    }

    /** The existing contract, with its current dates, that the request would correct. */
    public ContractPeriod correctedOccurrence() {
        return correctedOccurrence;
    }

    /** The dates that were asked for as an add. */
    public ContractPeriod requested() {
        return requested;
    }
}
