package com.b4rrhh.employee.temporal.support;

/**
 * Coverage a vertical declares for its temporal series (ADR-057).
 *
 * <p>Every series is subject to the no-overlap invariant. Only a series with
 * {@link #MANDATORY} coverage is also subject to the no-gap-within-presence
 * invariant: while the employee is present there must be exactly one
 * occurrence in force at every date.
 */
public enum TimelineCoverage {

    /** No gaps allowed inside the presence period (contract, working time, labor classification). */
    MANDATORY,

    /** Gaps inside the presence period are legal (a vertical the employee may simply not have). */
    OPTIONAL;

    public boolean requiresNoGaps() {
        return this == MANDATORY;
    }
}
