package com.b4rrhh.employee.temporal.support;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoOverlapInvariantTest {

    private final NoOverlapInvariant invariant = new NoOverlapInvariant();

    @Test
    void contiguousSeriesHoldsWithoutOverlaps() {
        List<DateRange> series = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)),
                range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)),
                range(LocalDate.of(2026, 7, 1), null)
        );

        assertTrue(invariant.holds(series));
        assertTrue(invariant.overlaps(series).isEmpty());
    }

    @Test
    void seriesWithGapsStillHoldsBecauseGapsAreNotOverlaps() {
        List<DateRange> series = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)),
                range(LocalDate.of(2026, 7, 1), null)
        );

        assertTrue(invariant.holds(series));
    }

    @Test
    void emptySeriesHolds() {
        assertTrue(invariant.holds(List.of()));
    }

    @Test
    void overlapBetweenConsecutiveOccurrencesIsReported() {
        List<DateRange> series = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 15)),
                range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30))
        );

        assertFalse(invariant.holds(series));
        assertEquals(
                List.of(range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15))),
                invariant.overlaps(series)
        );
    }

    @Test
    void openOccurrenceOverlapsEveryLaterOne() {
        List<DateRange> series = List.of(
                range(LocalDate.of(2026, 1, 1), null),
                range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)),
                range(LocalDate.of(2026, 7, 1), null)
        );

        assertEquals(
                List.of(
                        range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)),
                        range(LocalDate.of(2026, 7, 1), null)
                ),
                invariant.overlaps(series)
        );
    }

    @Test
    void sameStartDateIsAnOverlap() {
        List<DateRange> series = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10))
        );

        assertFalse(invariant.holds(series));
        assertEquals(
                List.of(range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10))),
                invariant.overlaps(series)
        );
    }

    @Test
    void orderOfInputDoesNotMatter() {
        List<DateRange> series = List.of(
                range(LocalDate.of(2026, 7, 1), null),
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)),
                range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30))
        );

        assertTrue(invariant.holds(series));
    }

    private DateRange range(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }
}
