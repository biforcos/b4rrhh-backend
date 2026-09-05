package com.b4rrhh.employee.labor_classification.application.model;

import com.b4rrhh.employee.labor_classification.domain.model.LaborClassificationPeriod;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;

import java.util.List;

/**
 * What would happen to the employee's labor classification series if an
 * operation were applied (ADR-057). A labor classification is identified by
 * the day it starts, so every occurrence here is named by its dates alone.
 * It is what the screen shows before confirming and what the write use
 * cases apply; it is never persisted.
 *
 * <ul>
 *   <li>{@link #occurrence()} is the one added, removed or corrected (under
 *       its new dates).</li>
 *   <li>{@link #correctedOccurrence()} is, on a correction, the occurrence as
 *       it stands today, the one {@link #occurrence()} replaces. It is also
 *       how the plan tells the screen that an add on an existing start date
 *       is the correction of that occurrence (backend#52). {@code null} on an
 *       add and on a removal.</li>
 *   <li>{@link #adjustedOccurrence()} is the only automatic consequence, or
 *       {@code null} when nothing else moves.</li>
 *   <li>{@link #stretchCandidates()} are the neighbours of the gaps: the
 *       occurrences the user could stretch. They are named, never moved.</li>
 *   <li>{@link #projected()} is the series as it would be, accepted or not.</li>
 * </ul>
 */
public record LaborClassificationPlan(
        TimelineOperation operation,
        TimelineRejection rejection,
        LaborClassificationPeriod occurrence,
        LaborClassificationPeriod correctedOccurrence,
        LaborClassificationPlanAdjustment adjustedOccurrence,
        List<LaborClassificationPeriod> overlaps,
        List<LaborClassificationPeriod> gaps,
        List<LaborClassificationPeriod> stretchCandidates,
        List<LaborClassificationPeriod> projected
) {

    public LaborClassificationPlan {
        if (operation == null) {
            throw new IllegalArgumentException("operation is required");
        }
        if (occurrence == null) {
            throw new IllegalArgumentException("occurrence is required");
        }
        overlaps = List.copyOf(overlaps);
        gaps = List.copyOf(gaps);
        stretchCandidates = List.copyOf(stretchCandidates);
        projected = List.copyOf(projected);
    }

    public boolean isAccepted() {
        return rejection == null;
    }

    public boolean adjustsAnOccurrence() {
        return adjustedOccurrence != null;
    }
}
