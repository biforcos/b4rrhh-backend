package com.b4rrhh.employee.working_time.application.model;

import com.b4rrhh.employee.working_time.domain.model.WorkingTimePeriod;

/**
 * The one existing occurrence a plan moves on its own (ADR-057): closed the
 * day before a new one, or reopened when the one that closed it is removed.
 * Only its end date changes.
 */
public record WorkingTimePlanAdjustment(
        Integer workingTimeNumber,
        WorkingTimePeriod before,
        WorkingTimePeriod after
) {

    public WorkingTimePlanAdjustment {
        if (workingTimeNumber == null) {
            throw new IllegalArgumentException("workingTimeNumber is required");
        }
        if (before == null || after == null) {
            throw new IllegalArgumentException("before and after are required");
        }
    }
}
