package com.b4rrhh.employee.temporal.support;

/**
 * Why a {@link TimelinePlan} cannot be applied. Each value is a rule of the
 * resulting state, not a precondition of the operation that produced it.
 */
public enum TimelineRejection {

    /** The occurrence would fall, in whole or in part, outside every presence period. */
    OUTSIDE_PRESENCE,

    /** Two occurrences would be in force on the same date. */
    OVERLAP,

    /** A gap would appear inside the presence period and the coverage is mandatory. */
    GAP_NOT_ALLOWED,

    /**
     * The plan is not the operation it was asked for. Asked for as an add, the
     * occurrence starts on the start date of an existing one: it would correct
     * that one, named in {@link TimelinePlan#correctedOccurrence()}, and a
     * correction has to be asked for as a correction (backend#58).
     */
    IS_A_CORRECTION
}
