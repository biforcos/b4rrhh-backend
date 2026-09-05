package com.b4rrhh.employee.contract.application.model;

import com.b4rrhh.employee.contract.domain.model.ContractPeriod;

/**
 * The one existing contract a plan moves on its own (ADR-057): closed the
 * day before a new one, or reopened when the one that closed it is removed.
 * Only its end date changes; the start date, which identifies it, stays.
 */
public record ContractPlanAdjustment(
        ContractPeriod before,
        ContractPeriod after
) {

    public ContractPlanAdjustment {
        if (before == null || after == null) {
            throw new IllegalArgumentException("before and after are required");
        }
        if (!before.startDate().equals(after.startDate())) {
            throw new IllegalArgumentException("an adjustment only moves the end date");
        }
    }
}
