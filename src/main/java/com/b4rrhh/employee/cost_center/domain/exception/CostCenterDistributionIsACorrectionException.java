package com.b4rrhh.employee.cost_center.domain.exception;

import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionPeriod;

/**
 * What was asked for as an add is not an add (ADR-057, backend#52): the new
 * distribution starts on the start date of an existing window, so it would
 * correct that window, not add a second one. Nothing is applied. It names
 * the window that would be corrected so the user can ask for the correction
 * as such (PUT on that start date).
 */
public class CostCenterDistributionIsACorrectionException extends RuntimeException {

    private final CostCenterDistributionPeriod correctedOccurrence;
    private final CostCenterDistributionPeriod requested;

    public CostCenterDistributionIsACorrectionException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            CostCenterDistributionPeriod correctedOccurrence,
            CostCenterDistributionPeriod requested
    ) {
        super("Adding a cost center distribution from "
                + requested.startDate()
                + " to "
                + requested.endDate()
                + " is not an add for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ": it starts on the start date of the distribution window "
                + correctedOccurrence.startDate()
                + " to "
                + correctedOccurrence.endDate()
                + " and would correct it. Ask for it as a correction of that window.");
        this.correctedOccurrence = correctedOccurrence;
        this.requested = requested;
    }

    /** The existing window, with its current dates, that the request would correct. */
    public CostCenterDistributionPeriod correctedOccurrence() {
        return correctedOccurrence;
    }

    /** The dates that were asked for as an add. */
    public CostCenterDistributionPeriod requested() {
        return requested;
    }
}
