package com.b4rrhh.employee.temporal.support;

/**
 * Whether the occurrences of a temporal series have to fall inside the
 * employee's presence (ADR-057).
 *
 * <p>Most series are facts of the employment and cannot exist outside it: a
 * working time, a contract, a work center. Some are facts of the person that
 * the employment merely frames: an address does not expire because the
 * employee leaves, and the company keeps writing to it afterwards. Such a
 * series still has to cover the presence when its coverage is mandatory, but
 * an occurrence that starts before the hire or outlives the termination is not
 * a fault (backend#53).
 */
public enum TimelineContainment {

    /** Every occurrence falls inside a presence period; one that does not is rejected as OUTSIDE_PRESENCE. */
    WITHIN_PRESENCE,

    /** Occurrences may start before the presence and outlive it; the presence only frames the gap invariant. */
    MAY_OUTLIVE_PRESENCE;

    public boolean requiresContainment() {
        return this == WITHIN_PRESENCE;
    }
}
