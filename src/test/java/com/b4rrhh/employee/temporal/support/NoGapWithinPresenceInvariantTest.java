package com.b4rrhh.employee.temporal.support;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoGapWithinPresenceInvariantTest {

    private static final List<DateRange> OPEN_PRESENCE = List.of(
            new DateRange(LocalDate.of(2026, 1, 1), null)
    );

    private final NoGapWithinPresenceInvariant invariant = new NoGapWithinPresenceInvariant();

    @Test
    void contiguousSeriesCoveringOpenPresenceHolds() {
        List<DateRange> series = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)),
                range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)),
                range(LocalDate.of(2026, 7, 1), null)
        );

        assertTrue(invariant.holds(series, OPEN_PRESENCE));
        assertTrue(invariant.gaps(series, OPEN_PRESENCE).isEmpty());
    }

    @Test
    void gapBetweenOccurrencesIsReported() {
        List<DateRange> series = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)),
                range(LocalDate.of(2026, 7, 1), null)
        );

        assertFalse(invariant.holds(series, OPEN_PRESENCE));
        assertEquals(
                List.of(range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30))),
                invariant.gaps(series, OPEN_PRESENCE)
        );
    }

    @Test
    void gapAtTheStartOfPresenceIsReported() {
        List<DateRange> series = List.of(
                range(LocalDate.of(2026, 2, 1), null)
        );

        assertEquals(
                List.of(range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))),
                invariant.gaps(series, OPEN_PRESENCE)
        );
    }

    @Test
    void trailingGapInOpenPresenceIsOpenEnded() {
        List<DateRange> series = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))
        );

        assertEquals(
                List.of(range(LocalDate.of(2026, 4, 1), null)),
                invariant.gaps(series, OPEN_PRESENCE)
        );
    }

    @Test
    void trailingGapInClosedPresenceEndsWithPresence() {
        List<DateRange> presence = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        );
        List<DateRange> series = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))
        );

        assertEquals(
                List.of(range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 12, 31))),
                invariant.gaps(series, presence)
        );
    }

    @Test
    void emptySeriesLeavesTheWholePresenceAsAGap() {
        assertEquals(
                List.of(range(LocalDate.of(2026, 1, 1), null)),
                invariant.gaps(List.of(), OPEN_PRESENCE)
        );
    }

    @Test
    void emptyPresenceHasNoGaps() {
        List<DateRange> series = List.of(
                range(LocalDate.of(2026, 1, 1), null)
        );

        assertTrue(invariant.holds(series, List.of()));
    }

    @Test
    void gapsBetweenPresencePeriodsAreNotGaps() {
        List<DateRange> presence = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)),
                range(LocalDate.of(2026, 7, 1), null)
        );
        List<DateRange> series = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)),
                range(LocalDate.of(2026, 7, 1), null)
        );

        assertTrue(invariant.holds(series, presence));
    }

    @Test
    void gapsAreReportedPerPresencePeriod() {
        List<DateRange> presence = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)),
                range(LocalDate.of(2026, 7, 1), null)
        );
        List<DateRange> series = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28)),
                range(LocalDate.of(2026, 8, 1), null)
        );

        assertEquals(
                List.of(
                        range(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)),
                        range(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31))
                ),
                invariant.gaps(series, presence)
        );
    }

    @Test
    void occurrencesOutsidePresenceDoNotCoverAnything() {
        List<DateRange> presence = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        );
        List<DateRange> series = List.of(
                range(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        );

        assertTrue(invariant.holds(series, presence));
    }

    private DateRange range(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }
}
