package com.b4rrhh.employee.temporal.support;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A temporal series of an employee as the planner sees it (ADR-057): the
 * coverage and the containment the vertical declares, the presence periods
 * that frame the series, and the occurrences currently in it.
 *
 * <p>Occurrences and presence periods are sorted by start date on
 * construction: the order of a series is derived from its dates and nothing
 * else. The timeline does not validate its own invariants; existing data may
 * break them and it is the plan that says whether the resulting state holds.
 */
public record Timeline(
        TimelineCoverage coverage,
        TimelineContainment containment,
        List<DateRange> presence,
        List<DateRange> occurrences
) {

    public Timeline {
        if (coverage == null) {
            throw new IllegalArgumentException("coverage is required");
        }
        if (containment == null) {
            throw new IllegalArgumentException("containment is required");
        }
        presence = sortedCopy(presence, "presence");
        occurrences = sortedCopy(occurrences, "occurrences");
    }

    /** A series whose occurrences must stay inside the presence: the common case. */
    public Timeline(TimelineCoverage coverage, List<DateRange> presence, List<DateRange> occurrences) {
        this(coverage, TimelineContainment.WITHIN_PRESENCE, presence, occurrences);
    }

    private static List<DateRange> sortedCopy(List<DateRange> periods, String name) {
        if (periods == null) {
            throw new IllegalArgumentException(name + " is required");
        }

        List<DateRange> copy = new ArrayList<>(periods);
        for (DateRange period : copy) {
            if (period == null) {
                throw new IllegalArgumentException(name + " contains null period");
            }
        }
        copy.sort(Comparator.comparing(DateRange::startDate));
        return List.copyOf(copy);
    }
}
