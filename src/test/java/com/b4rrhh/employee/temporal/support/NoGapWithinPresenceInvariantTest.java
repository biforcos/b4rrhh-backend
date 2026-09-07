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

    /**
     * What separates the gap a change opens from the one it found
     * (backend#70). The whole rule of the mandatory coverage rests on this
     * telling: a write is judged by what it uncovers, not by what was
     * uncovered when it arrived.
     */
    @Test
    void aGapThatWasAlreadyThereIsNotOpenedByTheChange() {
        List<DateRange> before = List.of(range(LocalDate.of(2026, 6, 1), null));
        List<DateRange> after = List.of(range(LocalDate.of(2026, 6, 1), null));

        assertTrue(invariant.opened(before, after).isEmpty());
    }

    @Test
    void aSmallerGapInsideAnOldOneIsNotOpenedEither() {
        List<DateRange> before = List.of(range(LocalDate.of(2026, 1, 1), null));
        List<DateRange> after = List.of(range(LocalDate.of(2026, 6, 1), null));

        assertTrue(invariant.opened(before, after).isEmpty());
    }

    @Test
    void aGapThatReachesPastTheOldOneIsOpened() {
        List<DateRange> before = List.of(range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)));
        List<DateRange> after = List.of(range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 30)));

        assertEquals(
                List.of(range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 30))),
                invariant.opened(before, after)
        );
    }

    @Test
    void aGapOnASeriesThatHadNoneIsOpened() {
        List<DateRange> after = List.of(range(LocalDate.of(2026, 6, 1), null));

        assertEquals(after, invariant.opened(List.of(), after));
    }

    // What used to separate two gaps was covered, so joining them uncovers it.
    @Test
    void aGapSpanningTwoOldOnesIsOpened() {
        List<DateRange> before = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                range(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))
        );
        List<DateRange> after = List.of(range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)));

        assertEquals(after, invariant.opened(before, after));
    }

    @Test
    void filledGapsOpenNothing() {
        List<DateRange> before = List.of(range(LocalDate.of(2026, 1, 1), null));

        assertTrue(invariant.opened(before, List.of()).isEmpty());
    }

    // The other half of the rule: which gaps the series starts up again after
    // — those hold whoever left them — and which one is the trailing edge.
    @Test
    void aGapBetweenTwoOccurrencesHasOneAfterIt() {
        List<DateRange> series = List.of(
                range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                range(LocalDate.of(2026, 3, 1), null)
        );
        List<DateRange> gaps = List.of(range(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)));

        assertEquals(gaps, invariant.beforeAnOccurrence(gaps, series));
    }

    // The asymmetry that decides the whole rule: this one is a claim about a
    // past that is over, so it counts; the trailing one is a question still
    // open, so it does not.
    @Test
    void aGapBeforeTheFirstOccurrenceHasOneAfterItToo() {
        List<DateRange> series = List.of(range(LocalDate.of(2026, 6, 1), null));
        List<DateRange> gaps = List.of(range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31)));

        assertEquals(gaps, invariant.beforeAnOccurrence(gaps, series));
    }

    @Test
    void theGapAfterTheLastOccurrenceHasNothingAfterIt() {
        List<DateRange> series = List.of(range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31)));
        List<DateRange> gaps = List.of(range(LocalDate.of(2026, 6, 1), null));

        assertTrue(invariant.beforeAnOccurrence(gaps, series).isEmpty());
    }

    // It trails even when the presence ends: the series stops there and
    // nothing starts again.
    @Test
    void theGapToTheEndOfAClosedPresenceTrailsToo() {
        List<DateRange> series = List.of(range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));
        List<DateRange> gaps = List.of(range(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 31)));

        assertTrue(invariant.beforeAnOccurrence(gaps, series).isEmpty());
    }

    private DateRange range(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }
}
