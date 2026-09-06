package com.b4rrhh.employee.workcenter.application.model;

import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterOccurrence;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterPeriod;

import java.util.List;

/**
 * What would happen to the employee's work center series if an operation
 * were applied (ADR-057), with every assignment named by its number. It is
 * what the screen shows before confirming and what the write use cases
 * apply; it is never persisted.
 *
 * <ul>
 *   <li>{@link #occurrence()} is the one added, removed or corrected; on an
 *       add it has no number yet.</li>
 *   <li>{@link #correctedOccurrence()} is, on a correction, the assignment as
 *       it stands today, the one {@link #occurrence()} replaces. It is also
 *       how the plan tells the screen that an add on an existing start date
 *       is the correction of that assignment (backend#52). {@code null} on an
 *       add and on a removal.</li>
 *   <li>{@link #adjustedOccurrence()} is the only automatic consequence, or
 *       {@code null} when nothing else moves.</li>
 *   <li>{@link #stretchCandidates()} are the neighbours of the gaps: the
 *       assignments the user could stretch. They are named, never moved.</li>
 *   <li>{@link #projected()} is the series as it would be, accepted or not.</li>
 * </ul>
 */
public record WorkCenterPlan(
        TimelineOperation operation,
        TimelineRejection rejection,
        WorkCenterOccurrence occurrence,
        WorkCenterOccurrence correctedOccurrence,
        WorkCenterPlanAdjustment adjustedOccurrence,
        List<WorkCenterPeriod> overlaps,
        List<WorkCenterPeriod> gaps,
        List<WorkCenterOccurrence> stretchCandidates,
        List<WorkCenterOccurrence> projected
) {

    public WorkCenterPlan {
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
