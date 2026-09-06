package com.b4rrhh.employee.cost_center.application.model;

import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionPeriod;

/**
 * The one existing distribution window a plan moves on its own (ADR-057):
 * closed the day before a new one, or reopened when the one that closed it
 * is removed. Only its end date changes, and it changes for every line of
 * the window at once: the occurrence is the window.
 */
public record CostCenterDistributionPlanAdjustment(
        CostCenterDistributionPeriod before,
        CostCenterDistributionPeriod after
) {

    public CostCenterDistributionPlanAdjustment {
        if (before == null || after == null) {
            throw new IllegalArgumentException("before and after are required");
        }
        if (!before.startDate().equals(after.startDate())) {
            throw new IllegalArgumentException("an adjustment only moves the end date");
        }
    }
}
