package com.b4rrhh.employee.temporal.support;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Second invariant of a temporal series (ADR-057): while the employee is
 * present, some occurrence is in force. It only applies to series whose
 * coverage is {@link TimelineCoverage#MANDATORY}.
 *
 * <p>Dates outside every presence period are never a gap: a series may leave
 * them uncovered, and whether it may cover them is the containment rule, not
 * this invariant.
 *
 * <p>Technical helper: it reports the uncovered stretches and does not throw
 * business exceptions.
 */
public final class NoGapWithinPresenceInvariant {

    public boolean holds(List<DateRange> series, List<DateRange> presence) {
        return gaps(series, presence).isEmpty();
    }

    /**
     * Returns the stretches inside the presence periods that no occurrence
     * covers. A trailing gap in an open presence period is open-ended. Sorted
     * by start date.
     */
    public List<DateRange> gaps(List<DateRange> series, List<DateRange> presence) {
        List<DateRange> occurrences = sortedByStartDate(series, "series");
        List<DateRange> presencePeriods = sortedByStartDate(presence, "presence");
        List<DateRange> gaps = new ArrayList<>();

        for (DateRange presencePeriod : presencePeriods) {
            collectGaps(occurrences, presencePeriod, gaps);
        }

        return gaps;
    }

    private static void collectGaps(
            List<DateRange> occurrences,
            DateRange presencePeriod,
            List<DateRange> gaps
    ) {
        LocalDate presenceEnd = presencePeriod.effectiveEnd(TemporalDates.MAX_DATE);
        LocalDate cursor = presencePeriod.startDate();

        for (DateRange occurrence : occurrences) {
            if (!occurrence.overlaps(presencePeriod, TemporalDates.MAX_DATE)) {
                continue;
            }

            LocalDate coveredStart = latest(occurrence.startDate(), presencePeriod.startDate());
            LocalDate coveredEnd = earliest(occurrence.effectiveEnd(TemporalDates.MAX_DATE), presenceEnd);

            if (coveredStart.isAfter(cursor)) {
                gaps.add(new DateRange(cursor, TemporalDates.previousDay(coveredStart)));
            }

            if (!coveredEnd.isBefore(cursor)) {
                cursor = TemporalDates.nextDay(coveredEnd);
            }

            if (isBeyond(cursor, presenceEnd)) {
                return;
            }
        }

        if (!isBeyond(cursor, presenceEnd)) {
            gaps.add(new DateRange(cursor, presencePeriod.endDate()));
        }
    }

    private static boolean isBeyond(LocalDate cursor, LocalDate presenceEnd) {
        return cursor.isAfter(presenceEnd)
                || (TemporalDates.MAX_DATE.equals(cursor) && TemporalDates.MAX_DATE.equals(presenceEnd));
    }

    private static LocalDate latest(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private static LocalDate earliest(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    private static List<DateRange> sortedByStartDate(List<DateRange> periods, String name) {
        if (periods == null) {
            throw new IllegalArgumentException(name + " is required");
        }

        List<DateRange> sorted = new ArrayList<>(periods);
        for (DateRange period : sorted) {
            if (period == null) {
                throw new IllegalArgumentException(name + " contains null period");
            }
        }
        sorted.sort(Comparator.comparing(DateRange::startDate));
        return sorted;
    }
}
