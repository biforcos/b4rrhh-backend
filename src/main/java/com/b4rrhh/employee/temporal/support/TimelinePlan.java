package com.b4rrhh.employee.temporal.support;

import java.util.List;

/**
 * What would happen to a temporal series if an operation were applied
 * (ADR-057). It is a description, not an execution: the vertical that owns
 * the occurrences reads it and applies it, or shows it and waits.
 *
 * <ul>
 *   <li>{@link #intent()} is the operation the plan was asked for and
 *       {@link #operation()} the one it turned out to be. They differ in one
 *       case only: an add that lands on the start date of an existing
 *       occurrence is a correction of it. A plan that is not what was asked
 *       for is never accepted: it is rejected as
 *       {@link TimelineRejection#IS_A_CORRECTION}, and the record refuses to
 *       be built otherwise, so no vertical has to remember to check it
 *       (backend#58).</li>
 *   <li>{@link #occurrence()} is the one the operation is about: the added or
 *       removed one, or the corrected one under its new dates.</li>
 *   <li>{@link #correctedOccurrence()} is, on {@link TimelineOperation#CORRECT},
 *       the occurrence as it stands today, the one {@link #occurrence()}
 *       replaces. What identifies it is the day it starts: a plan asked for as
 *       an add whose start falls on the start of an existing occurrence comes
 *       back as the correction of that one, and this is where the plan names
 *       it. {@code null} on {@link TimelineOperation#ADD} and
 *       {@link TimelineOperation#REMOVE}.</li>
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
        TimelineOperation intent,
        TimelineOperation operation,
        DateRange occurrence,
        DateRange correctedOccurrence,
        TimelineRejection rejection,
        OccurrenceAdjustment adjustedOccurrence,
        List<DateRange> overlaps,
        List<DateRange> gaps,
        List<DateRange> stretchCandidates,
        List<DateRange> projected
) {

    public TimelinePlan {
        if (intent == null) {
            throw new IllegalArgumentException("intent is required");
        }
        if (operation == null) {
            throw new IllegalArgumentException("operation is required");
        }
        if ((intent != operation) != (rejection == TimelineRejection.IS_A_CORRECTION)) {
            throw new IllegalArgumentException("a plan that is not the operation asked for is rejected as IS_A_CORRECTION");
        }
        if (occurrence == null) {
            throw new IllegalArgumentException("occurrence is required");
        }
        if ((operation == TimelineOperation.CORRECT) != (correctedOccurrence != null)) {
            throw new IllegalArgumentException("correctedOccurrence is required on CORRECT and only there");
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
