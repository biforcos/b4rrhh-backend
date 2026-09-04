package com.b4rrhh.employee.temporal.support;

/**
 * The two ways a temporal series is written (ADR-057): an occurrence is
 * added, or an occurrence is removed. Stretching or shrinking one is the
 * user's act and not an operation of the component.
 */
public enum TimelineOperation {
    ADD,
    REMOVE
}
