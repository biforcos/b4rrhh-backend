package com.b4rrhh.employee.temporal.support;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Plans the two writes a temporal series admits (ADR-057) and judges the
 * resulting state against the invariants the series declares. It never
 * applies anything: both methods are pure and can be called to show the
 * effect before confirming.
 *
 * <p><b>Adding</b> an occurrence has one automatic consequence, and only one:
 * the occurrence the new one starts inside of is closed the day before the
 * new start. When that occurrence is the open last one, this is the ADR's
 * rule; when it is a closed one in the middle, this is the split the old
 * {@code ReplaceMode.SPLIT} already described. Anything else the new
 * occurrence would collide with is an overlap and the plan is rejected:
 * nothing else moves on its own.
 *
 * <p>There is one add that is not an add. An occurrence is identified by the
 * day it starts, so a new one that starts on the very start date of an
 * existing one is a correction of that one, whatever its end date: the plan
 * comes back as a {@link TimelineOperation#CORRECT} that names it, carrying
 * what {@link #planCorrect} would find, and rejected as
 * {@link TimelineRejection#IS_A_CORRECTION} because it is not the operation
 * that was asked for. The user reads what it is and asks for it as a
 * correction. This keeps what the old {@code ReplaceMode.EXACT_START} did
 * for the user without doing it silently, and without any vertical having to
 * notice on its own (backend#58).
 *
 * <p><b>Removing</b> the last occurrence reopens the previous one, up to where
 * the removed one ended, provided the previous one was closed right where the
 * removed one started. Removing any other occurrence leaves its dates
 * uncovered: with mandatory coverage the gap invariant rejects it and the
 * plan names the neighbours the user could stretch.
 *
 * <p><b>Correcting</b> an occurrence replaces its dates with the ones the
 * user gives and moves nothing else: whatever gap or overlap the new dates
 * leave is judged like any other resulting state.
 *
 * <p>Whether the added or corrected occurrence has to fall inside the presence
 * is what the series declares as its {@link TimelineContainment}: a series
 * that may outlive the presence is never rejected as
 * {@link TimelineRejection#OUTSIDE_PRESENCE}, and its presence periods only
 * frame the gap invariant (backend#53).
 */
public final class TimelinePlanner {

    private final NoOverlapInvariant noOverlapInvariant = new NoOverlapInvariant();
    private final NoGapWithinPresenceInvariant noGapWithinPresenceInvariant = new NoGapWithinPresenceInvariant();
    private final TimelineCoverageValidator timelineCoverageValidator = new TimelineCoverageValidator();

    public TimelinePlan planAdd(Timeline timeline, DateRange occurrence) {
        requireTimeline(timeline);
        if (occurrence == null) {
            throw new IllegalArgumentException("occurrence is required");
        }

        DateRange startingOnTheSameDay = occurrenceStartingOn(timeline.occurrences(), occurrence.startDate());
        if (startingOnTheSameDay != null) {
            return correct(TimelineOperation.ADD, timeline, startingOnTheSameDay, occurrence);
        }

        boolean insidePresence = containmentHolds(timeline, occurrence);

        OccurrenceAdjustment closing = insidePresence
                ? closeCoveringOccurrence(timeline.occurrences(), occurrence.startDate())
                : null;

        List<DateRange> projected = new ArrayList<>(timeline.occurrences());
        if (closing != null) {
            projected.set(projected.indexOf(closing.before()), closing.after());
        }
        projected.add(occurrence);
        projected.sort(Comparator.comparing(DateRange::startDate));

        return judge(TimelineOperation.ADD, TimelineOperation.ADD, occurrence, null, closing, projected, timeline, insidePresence);
    }

    public TimelinePlan planRemove(Timeline timeline, DateRange occurrence) {
        requireTimeline(timeline);
        if (occurrence == null) {
            throw new IllegalArgumentException("occurrence is required");
        }

        int index = timeline.occurrences().indexOf(occurrence);
        if (index < 0) {
            throw new IllegalArgumentException("occurrence is not in the series: " + occurrence);
        }

        List<DateRange> projected = new ArrayList<>(timeline.occurrences());
        projected.remove(index);

        boolean wasLast = index == timeline.occurrences().size() - 1;
        OccurrenceAdjustment reopening = wasLast && index > 0
                ? reopenPreviousOccurrence(timeline.occurrences().get(index - 1), occurrence)
                : null;
        if (reopening != null) {
            projected.set(index - 1, reopening.after());
        }

        return judge(TimelineOperation.REMOVE, TimelineOperation.REMOVE, occurrence, null, reopening, projected, timeline, true);
    }

    public TimelinePlan planCorrect(Timeline timeline, DateRange existing, DateRange corrected) {
        requireTimeline(timeline);
        if (existing == null) {
            throw new IllegalArgumentException("existing is required");
        }
        if (corrected == null) {
            throw new IllegalArgumentException("corrected is required");
        }

        return correct(TimelineOperation.CORRECT, timeline, existing, corrected);
    }

    private TimelinePlan correct(TimelineOperation intent, Timeline timeline, DateRange existing, DateRange corrected) {
        int index = timeline.occurrences().indexOf(existing);
        if (index < 0) {
            throw new IllegalArgumentException("occurrence is not in the series: " + existing);
        }

        boolean insidePresence = containmentHolds(timeline, corrected);

        List<DateRange> projected = new ArrayList<>(timeline.occurrences());
        projected.set(index, corrected);
        projected.sort(Comparator.comparing(DateRange::startDate));

        return judge(intent, TimelineOperation.CORRECT, corrected, existing, null, projected, timeline, insidePresence);
    }

    /**
     * Judges the projected series against the invariants. A plan that is not
     * the operation it was asked for is rejected before anything else: what it
     * would run into is still reported, but the first thing the user has to
     * know is what the plan is.
     */
    private TimelinePlan judge(
            TimelineOperation intent,
            TimelineOperation operation,
            DateRange occurrence,
            DateRange correctedOccurrence,
            OccurrenceAdjustment adjustment,
            List<DateRange> projected,
            Timeline timeline,
            boolean insidePresence
    ) {
        List<DateRange> overlaps = noOverlapInvariant.overlaps(projected);
        List<DateRange> gaps = noGapWithinPresenceInvariant.gaps(projected, timeline.presence());
        List<DateRange> stretchCandidates = neighboursOf(gaps, projected);

        TimelineRejection rejection = null;
        if (operation != intent) {
            rejection = TimelineRejection.IS_A_CORRECTION;
        } else if (!insidePresence) {
            rejection = TimelineRejection.OUTSIDE_PRESENCE;
        } else if (!overlaps.isEmpty()) {
            rejection = TimelineRejection.OVERLAP;
        } else if (timeline.coverage().requiresNoGaps() && !gaps.isEmpty()) {
            rejection = TimelineRejection.GAP_NOT_ALLOWED;
        }

        return new TimelinePlan(
                intent,
                operation,
                occurrence,
                correctedOccurrence,
                rejection,
                adjustment,
                overlaps,
                gaps,
                stretchCandidates,
                projected
        );
    }

    /**
     * Whether the occurrence being written satisfies the containment the series
     * declares. A series that may outlive the presence always passes: for it,
     * the presence frames the gap invariant and nothing else.
     */
    private boolean containmentHolds(Timeline timeline, DateRange occurrence) {
        if (!timeline.containment().requiresContainment()) {
            return true;
        }

        return timelineCoverageValidator.isContained(List.of(occurrence), timeline.presence());
    }

    /** The occurrence that starts exactly on that day, if there is one. */
    private static DateRange occurrenceStartingOn(List<DateRange> occurrences, LocalDate startDate) {
        for (DateRange existing : occurrences) {
            if (existing.startDate().equals(startDate)) {
                return existing;
            }
        }

        return null;
    }

    /**
     * The occurrence that is in force on the new start date, closed the day
     * before it. An occurrence that starts on that very day never gets here:
     * adding on its start date is planned as its correction.
     */
    private static OccurrenceAdjustment closeCoveringOccurrence(List<DateRange> occurrences, LocalDate newStart) {
        for (DateRange existing : occurrences) {
            boolean startsBefore = existing.startDate().isBefore(newStart);
            boolean reachesNewStart = !existing.effectiveEnd(TemporalDates.MAX_DATE).isBefore(newStart);

            if (startsBefore && reachesNewStart) {
                return new OccurrenceAdjustment(
                        existing,
                        new DateRange(existing.startDate(), TemporalDates.previousDay(newStart))
                );
            }
        }

        return null;
    }

    /**
     * The previous occurrence takes the end date of the removed one, but only
     * when it ended exactly the day before the removed one started: that is
     * the closing this component made, and the only one it undoes.
     */
    private static OccurrenceAdjustment reopenPreviousOccurrence(DateRange previous, DateRange removed) {
        if (previous.endDate() == null) {
            return null;
        }
        if (!TemporalDates.nextDay(previous.endDate()).equals(removed.startDate())) {
            return null;
        }

        return new OccurrenceAdjustment(previous, new DateRange(previous.startDate(), removed.endDate()));
    }

    private static List<DateRange> neighboursOf(List<DateRange> gaps, List<DateRange> occurrences) {
        List<DateRange> neighbours = new ArrayList<>();

        for (DateRange gap : gaps) {
            for (DateRange occurrence : occurrences) {
                boolean endsRightBefore = occurrence.endDate() != null
                        && TemporalDates.nextDay(occurrence.endDate()).equals(gap.startDate());
                boolean startsRightAfter = gap.endDate() != null
                        && occurrence.startDate().equals(TemporalDates.nextDay(gap.endDate()));

                if ((endsRightBefore || startsRightAfter) && !neighbours.contains(occurrence)) {
                    neighbours.add(occurrence);
                }
            }
        }

        return neighbours;
    }

    private static void requireTimeline(Timeline timeline) {
        if (timeline == null) {
            throw new IllegalArgumentException("timeline is required");
        }
    }
}
