package com.b4rrhh.employee.extra_payment_regime.domain.exception;

import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegimeOccurrence;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegimePeriod;

/**
 * What was asked for as an add is not an add (ADR-057, backend#58): the new
 * extra payment regime starts on the start date of an existing one, so it would
 * correct that one, not add a second one. Nothing is applied. It names the
 * extra payment regime that would be corrected so the user can ask for the
 * correction as such.
 */
public final class ExtraPaymentRegimeIsACorrectionException extends ExtraPaymentRegimeSeriesInvariantException {

    private final ExtraPaymentRegimeOccurrence correctedOccurrence;
    private final ExtraPaymentRegimePeriod requested;

    public ExtraPaymentRegimeIsACorrectionException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            ExtraPaymentRegimeOccurrence correctedOccurrence,
            ExtraPaymentRegimePeriod requested
    ) {
        super("Adding a extra payment regime from "
                + requested.startDate()
                + " to "
                + requested.endDate()
                + " is not an add for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ": it starts on the start date of extra payment regime #"
                + correctedOccurrence.extraPaymentRegimeNumber()
                + " ("
                + correctedOccurrence.startDate()
                + " to "
                + correctedOccurrence.endDate()
                + ") and would correct it. Ask for it as a correction of that extra payment regime.");
        this.correctedOccurrence = correctedOccurrence;
        this.requested = requested;
    }

    /** The existing extra payment regime, with its current dates, that the request would correct. */
    public ExtraPaymentRegimeOccurrence correctedOccurrence() {
        return correctedOccurrence;
    }

    /** The dates that were asked for as an add. */
    public ExtraPaymentRegimePeriod requested() {
        return requested;
    }
}
