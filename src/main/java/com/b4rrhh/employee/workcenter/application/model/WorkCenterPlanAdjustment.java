package com.b4rrhh.employee.workcenter.application.model;

import com.b4rrhh.employee.workcenter.domain.model.WorkCenterPeriod;

/**
 * The one existing work center assignment a plan moves on its own
 * (ADR-057): closed the day before a new one, or reopened when the one that
 * closed it is removed. Only its end date changes.
 */
public record WorkCenterPlanAdjustment(
        Integer workCenterAssignmentNumber,
        WorkCenterPeriod before,
        WorkCenterPeriod after
) {

    public WorkCenterPlanAdjustment {
        if (workCenterAssignmentNumber == null) {
            throw new IllegalArgumentException("workCenterAssignmentNumber is required");
        }
        if (before == null || after == null) {
            throw new IllegalArgumentException("before and after are required");
        }
        if (!before.startDate().equals(after.startDate())) {
            throw new IllegalArgumentException("an adjustment only moves the end date");
        }
    }
}
