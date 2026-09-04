package com.b4rrhh.employee.temporal.support;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * First invariant of a temporal series (ADR-057): no two occurrences are in
 * force on the same date. It applies to every series, whatever its coverage.
 *
 * <p>Technical helper: it reports the overlapping stretches and does not throw
 * business exceptions.
 */
public final class NoOverlapInvariant {

    public boolean holds(List<DateRange> series) {
        return overlaps(series).isEmpty();
    }

    /**
     * Returns, for each occurrence that overlaps an earlier one, the stretch of
     * dates both are in force. Sorted by start date.
     */
    public List<DateRange> overlaps(List<DateRange> series) {
        List<DateRange> sorted = sortedByStartDate(series);
        List<DateRange> overlaps = new ArrayList<>();

        LocalDate reachedEnd = null;
        for (DateRange occurrence : sorted) {
            LocalDate occurrenceEnd = occurrence.effectiveEnd(TemporalDates.MAX_DATE);

            if (reachedEnd != null && !occurrence.startDate().isAfter(reachedEnd)) {
                LocalDate overlapEnd = occurrenceEnd.isBefore(reachedEnd) ? occurrenceEnd : reachedEnd;
                overlaps.add(new DateRange(occurrence.startDate(), toEndDate(overlapEnd)));
            }

            if (reachedEnd == null || occurrenceEnd.isAfter(reachedEnd)) {
                reachedEnd = occurrenceEnd;
            }
        }

        return overlaps;
    }

    private static List<DateRange> sortedByStartDate(List<DateRange> series) {
        if (series == null) {
            throw new IllegalArgumentException("series is required");
        }

        List<DateRange> sorted = new ArrayList<>(series);
        for (DateRange occurrence : sorted) {
            if (occurrence == null) {
                throw new IllegalArgumentException("series contains null occurrence");
            }
        }
        sorted.sort(Comparator.comparing(DateRange::startDate));
        return sorted;
    }

    private static LocalDate toEndDate(LocalDate effectiveEnd) {
        return TemporalDates.MAX_DATE.equals(effectiveEnd) ? null : effectiveEnd;
    }
}
