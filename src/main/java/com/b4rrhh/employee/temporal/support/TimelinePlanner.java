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
 * <p><b>Removing</b> the last occurrence reopens the previous one, up to where
 * the removed one ended, provided the previous one was closed right where the
 * removed one started. Removing any other occurrence leaves its dates
 * uncovered: with mandatory coverage the gap invariant rejects it and the
 * plan names the neighbours the user could stretch.
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

        boolean insidePresence = timelineCoverageValidator.isContained(List.of(occurrence), timeline.presence());

        OccurrenceAdjustment closing = insidePresence
                ? closeCoveringOccurrence(timeline.occurrences(), occurrence.startDate())
                : null;

        List<DateRange> projected = new ArrayList<>(timeline.occurrences());
        if (closing != null) {
            projected.set(projected.indexOf(closing.before()), closing.after());
        }
        projected.add(occurrence);
        projected.sort(Comparator.comparing(DateRange::startDate));

        return judge(TimelineOperation.ADD, occurrence, closing, projected, timeline, insidePresence);
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

        return judge(TimelineOperation.REMOVE, occurrence, reopening, projected, timeline, true);
    }

    private TimelinePlan judge(
            TimelineOperation operation,
            DateRange occurrence,
            OccurrenceAdjustment adjustment,
            List<DateRange> projected,
            Timeline timeline,
            boolean insidePresence
    ) {
        List<DateRange> overlaps = noOverlapInvariant.overlaps(projected);
        List<DateRange> gaps = noGapWithinPresenceInvariant.gaps(projected, timeline.presence());
        List<DateRange> stretchCandidates = neighboursOf(gaps, projected);

        TimelineRejection rejection = null;
        if (!insidePresence) {
            rejection = TimelineRejection.OUTSIDE_PRESENCE;
        } else if (!overlaps.isEmpty()) {
            rejection = TimelineRejection.OVERLAP;
        } else if (timeline.coverage().requiresNoGaps() && !gaps.isEmpty()) {
            rejection = TimelineRejection.GAP_NOT_ALLOWED;
        }

        return new TimelinePlan(
                operation,
                occurrence,
                rejection,
                adjustment,
                overlaps,
                gaps,
                stretchCandidates,
                projected
        );
    }

    /**
     * The occurrence that is in force on the new start date, closed the day
     * before it. An occurrence that starts on that very day cannot be closed
     * before its own start: it is left alone and the overlap rejects the plan.
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
