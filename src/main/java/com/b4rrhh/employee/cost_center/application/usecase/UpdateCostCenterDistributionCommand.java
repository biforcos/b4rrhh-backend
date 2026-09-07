package com.b4rrhh.employee.cost_center.application.usecase;

import java.time.LocalDate;
import java.util.List;

/**
 * A correction of the distribution window that starts on
 * {@code windowStartDate} (ADR-057, decision 3): its lines and its dates. The
 * window takes {@code startDate} to {@code endDate}, null for one that stays
 * open, and the corrected dates are judged by the invariants.
 * {@code startDate} is required: fixing the typo in a percentage without
 * inventing a change in the history is said by sending the window's own start
 * date again, not by leaving it out (backend#69).
 */
public record UpdateCostCenterDistributionCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        LocalDate windowStartDate,
        LocalDate startDate,
        LocalDate endDate,
        List<CostCenterDistributionItem> items
) {
}
