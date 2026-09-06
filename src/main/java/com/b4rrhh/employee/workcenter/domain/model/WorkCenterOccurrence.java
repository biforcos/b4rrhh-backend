package com.b4rrhh.employee.workcenter.domain.model;

import java.time.LocalDate;

/**
 * A work center assignment as a plan names it: its number and its dates.
 * The number is {@code null} only for the assignment a plan would add, which
 * has not been numbered yet.
 */
public record WorkCenterOccurrence(
        Integer workCenterAssignmentNumber,
        LocalDate startDate,
        LocalDate endDate
) {

    public WorkCenterOccurrence {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }
    }
}
