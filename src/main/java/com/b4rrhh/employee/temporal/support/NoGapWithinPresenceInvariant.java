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

    /**
     * Of the gaps a change would leave, the ones it opens: the stretches that
     * something covered before and nothing covers after.
     *
     * <p>Together with {@link #beforeAnOccurrence}, this is what a write is
     * judged by, and not the plain absence of gaps (ADR-057 §7, backend#70).
     * A state that is already uncovered cannot always be reached in one
     * write. An employee who moved house has two addresses that are the two
     * halves of the move: written alone, the first leaves the tail of the
     * presence uncovered and the second leaves its head, so with the plain
     * rule neither can go first and the valid state — both — is unreachable
     * one write at a time. The loader's run showed it as 220 rejected
     * addresses over the 110 employees who moved, 81 of whom ended with no
     * address at all: not because a bad write got through, but because every
     * good one was turned away.
     *
     * <p>Written in the order they happened, the head goes in leaving only
     * the trailing gap and the tail then closes it. So at the trailing edge a
     * change may leave a gap it found; it may not open one. Shrinking the
     * only address of a present employee still fails, because that uncovers a
     * stretch that was covered a moment ago.
     *
     * <p>A gap that spans two old ones is opened, not inherited: what used to
     * separate them was covered.
     */
    public List<DateRange> opened(List<DateRange> gapsBefore, List<DateRange> gapsAfter) {
        List<DateRange> before = sortedByStartDate(gapsBefore, "gapsBefore");
        List<DateRange> after = sortedByStartDate(gapsAfter, "gapsAfter");
        List<DateRange> opened = new ArrayList<>();

        for (DateRange gap : after) {
            if (!isInheritedFrom(gap, before)) {
                opened.add(gap);
            }
        }

        return opened;
    }

    /**
     * Of those gaps, the ones the series reaches past: an occurrence starts
     * again after them. A hole between two occurrences is one; so is the
     * stretch between the start of the presence and a first occurrence that
     * begins later.
     *
     * <p>These are rejected whoever left them there, found or opened (ADR-057
     * §7). Only the <b>trailing</b> gap gets the leeway the move needs — the
     * one that runs from the last occurrence to the end of the presence —
     * because that is the only one that means «nobody has told me yet», which
     * on an ongoing hire is a normal state, not an error.
     *
     * <p>The asymmetry is the point and it is not a rounding of the rule.
     * Not knowing where someone lives <i>now</i> is a pending question. Not
     * knowing where they lived <i>when they were hired</i> is not: that day
     * is in the past and its data should have been complete from the start,
     * so a first occurrence that begins after the hire is still rejected, and
     * so is a hole in the middle. What the series says about the past has to
     * hold; what it has not said about the present may still be coming.
     */
    public List<DateRange> beforeAnOccurrence(List<DateRange> gaps, List<DateRange> series) {
        List<DateRange> occurrences = sortedByStartDate(series, "series");
        List<DateRange> reached = new ArrayList<>();

        for (DateRange gap : sortedByStartDate(gaps, "gaps")) {
            if (hasOccurrenceRightAfter(gap, occurrences)) {
                reached.add(gap);
            }
        }

        return reached;
    }

    private static boolean hasOccurrenceRightAfter(DateRange gap, List<DateRange> occurrences) {
        if (gap.endDate() == null) {
            return false;
        }

        for (DateRange occurrence : occurrences) {
            if (occurrence.startDate().equals(TemporalDates.nextDay(gap.endDate()))) {
                return true;
            }
        }

        return false;
    }

    private static boolean isInheritedFrom(DateRange gap, List<DateRange> gapsBefore) {
        for (DateRange existing : gapsBefore) {
            if (existing.contains(gap, TemporalDates.MAX_DATE)) {
                return true;
            }
        }

        return false;
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
