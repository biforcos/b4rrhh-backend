package com.b4rrhh.employee.workcenter.domain.exception;

import com.b4rrhh.employee.workcenter.domain.model.WorkCenterOccurrence;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterPeriod;

/**
 * What was asked for as an add is not an add (ADR-057, backend#52): the new
 * work center assignment starts on the start date of an existing one, so it
 * would correct that one, not add a second one. Nothing is applied. It names
 * the assignment that would be corrected so the user can ask for the
 * correction as such. This is what the old {@code EXACT_START} replacement
 * did silently.
 */
public class WorkCenterIsACorrectionException extends RuntimeException {

    private final WorkCenterOccurrence correctedOccurrence;
    private final WorkCenterPeriod requested;

    public WorkCenterIsACorrectionException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            WorkCenterOccurrence correctedOccurrence,
            WorkCenterPeriod requested
    ) {
        super("Adding a work center assignment from "
                + requested.startDate()
                + " to "
                + requested.endDate()
                + " is not an add for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ": it starts on the start date of work center assignment #"
                + correctedOccurrence.workCenterAssignmentNumber()
                + " ("
                + correctedOccurrence.startDate()
                + " to "
                + correctedOccurrence.endDate()
                + ") and would correct it. Ask for it as a correction of that assignment.");
        this.correctedOccurrence = correctedOccurrence;
        this.requested = requested;
    }

    /** The existing assignment, with its current dates, that the request would correct. */
    public WorkCenterOccurrence correctedOccurrence() {
        return correctedOccurrence;
    }

    /** The dates that were asked for as an add. */
    public WorkCenterPeriod requested() {
        return requested;
    }
}
