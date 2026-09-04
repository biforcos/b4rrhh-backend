package com.b4rrhh.employee.temporal.support;

import java.util.List;

/**
 * What would happen to a temporal series if an operation were applied
 * (ADR-057). It is a description, not an execution: the vertical that owns
 * the occurrences reads it and applies it, or shows it and waits.
 *
 * <ul>
 *   <li>{@link #adjustedOccurrence()} is the one automatic consequence: on
 *       {@link TimelineOperation#ADD} the occurrence the new one starts inside
 *       of, closed the day before; on {@link TimelineOperation#REMOVE} the
 *       previous occurrence, reopened up to where the removed one ended.
 *       {@code null} when nothing else moves, and always on
 *       {@link TimelineOperation#CORRECT}.</li>
 *   <li>{@link #gaps()} are the stretches inside the presence that the
 *       resulting series leaves uncovered. With optional coverage they are
 *       legal and the plan is still accepted.</li>
 *   <li>{@link #stretchCandidates()} are the neighbours of those gaps: the
 *       occurrences the user could stretch to cover them. The component names
 *       them and never stretches them.</li>
 *   <li>{@link #projected()} is the series as it would be, accepted or not.</li>
 * </ul>
 */
public record TimelinePlan(
        TimelineOperation operation,
        DateRange occurrence,
        TimelineRejection rejection,
        OccurrenceAdjustment adjustedOccurrence,
        List<DateRange> overlaps,
        List<DateRange> gaps,
        List<DateRange> stretchCandidates,
        List<DateRange> projected
) {

    public TimelinePlan {
        if (operation == null) {
            throw new IllegalArgumentException("operation is required");
        }
        if (occurrence == null) {
            throw new IllegalArgumentException("occurrence is required");
        }
        overlaps = requiredList(overlaps, "overlaps");
        gaps = requiredList(gaps, "gaps");
        stretchCandidates = requiredList(stretchCandidates, "stretchCandidates");
        projected = requiredList(projected, "projected");
    }

    public boolean isAccepted() {
        return rejection == null;
    }

    public boolean adjustsAnOccurrence() {
        return adjustedOccurrence != null;
    }

    private static List<DateRange> requiredList(List<DateRange> list, String name) {
        if (list == null) {
            throw new IllegalArgumentException(name + " is required");
        }

        return List.copyOf(list);
    }
}
