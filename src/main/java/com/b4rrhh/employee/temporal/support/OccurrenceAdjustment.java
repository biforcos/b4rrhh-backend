package com.b4rrhh.employee.temporal.support;

/**
 * An existing occurrence whose end date changes as the automatic consequence
 * of a plan: closed the day before a new occurrence, or reopened when the one
 * that closed it is removed. The start date never changes: it is what
 * identifies the occurrence.
 */
public record OccurrenceAdjustment(
        DateRange before,
        DateRange after
) {

    public OccurrenceAdjustment {
        if (before == null) {
            throw new IllegalArgumentException("before is required");
        }
        if (after == null) {
            throw new IllegalArgumentException("after is required");
        }
        if (!before.startDate().equals(after.startDate())) {
            throw new IllegalArgumentException("an adjustment only moves the end date");
        }
    }
}
