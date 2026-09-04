package com.b4rrhh.employee.temporal.support;

/**
 * The ways a temporal series is written (ADR-057): an occurrence is added,
 * an occurrence is removed, or the user corrects one. Stretching or
 * shrinking an occurrence is the user's act, never a consequence the
 * component produces on its own; the component only judges the result.
 */
public enum TimelineOperation {
    ADD,
    REMOVE,
    CORRECT
}
