package com.b4rrhh.employee.working_time.domain.exception;

import com.b4rrhh.employee.working_time.domain.model.WorkingTimeOccurrence;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimePeriod;

/**
 * What was asked for as an add is not an add (ADR-057, backend#58): the new
 * working time starts on the start date of an existing one, so it would
 * correct that one, not add a second one. Nothing is applied. It names the
 * working time that would be corrected so the user can ask for the
 * correction as such.
 */
public class WorkingTimeIsACorrectionException extends RuntimeException {

    private final WorkingTimeOccurrence correctedOccurrence;
    private final WorkingTimePeriod requested;

    public WorkingTimeIsACorrectionException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            WorkingTimeOccurrence correctedOccurrence,
            WorkingTimePeriod requested
    ) {
        super("Adding a working time from "
                + requested.startDate()
                + " to "
                + requested.endDate()
                + " is not an add for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ": it starts on the start date of working time #"
                + correctedOccurrence.workingTimeNumber()
                + " ("
                + correctedOccurrence.startDate()
                + " to "
                + correctedOccurrence.endDate()
                + ") and would correct it. Ask for it as a correction of that working time.");
        this.correctedOccurrence = correctedOccurrence;
        this.requested = requested;
    }

    /** The existing working time, with its current dates, that the request would correct. */
    public WorkingTimeOccurrence correctedOccurrence() {
        return correctedOccurrence;
    }

    /** The dates that were asked for as an add. */
    public WorkingTimePeriod requested() {
        return requested;
    }
}
