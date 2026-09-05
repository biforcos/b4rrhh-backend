package com.b4rrhh.employee.labor_classification.domain.exception;

import com.b4rrhh.employee.labor_classification.domain.model.LaborClassificationPeriod;

/**
 * What was asked for as an add is not an add (ADR-057, backend#52): the new
 * labor classification starts on the start date of an existing one, so it
 * would correct that one, not add a second one. Nothing is applied. It names
 * the labor classification that would be corrected so the user can ask for
 * the correction as such. This is what the old {@code ReplaceMode.EXACT_START}
 * did silently.
 */
public class LaborClassificationIsACorrectionException extends RuntimeException {

    private final LaborClassificationPeriod correctedOccurrence;
    private final LaborClassificationPeriod requested;

    public LaborClassificationIsACorrectionException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LaborClassificationPeriod correctedOccurrence,
            LaborClassificationPeriod requested
    ) {
        super("Adding a labor classification from "
                + requested.startDate()
                + " to "
                + requested.endDate()
                + " is not an add for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ": it starts on the start date of the labor classification from "
                + correctedOccurrence.startDate()
                + " to "
                + correctedOccurrence.endDate()
                + " and would correct it. Ask for it as a correction of that labor classification.");
        this.correctedOccurrence = correctedOccurrence;
        this.requested = requested;
    }

    /** The existing labor classification, with its current dates, that the request would correct. */
    public LaborClassificationPeriod correctedOccurrence() {
        return correctedOccurrence;
    }

    /** The dates that were asked for as an add. */
    public LaborClassificationPeriod requested() {
        return requested;
    }
}
