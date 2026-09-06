package com.b4rrhh.employee.cost_center.application.usecase;

import java.time.LocalDate;
import java.util.List;

/**
 * A correction of the distribution window that starts on
 * {@code windowStartDate} (ADR-057, decision 3): its lines, and its dates
 * when {@code startDate} comes. With {@code startDate} null the window keeps
 * the dates it has and only the lines change: the typo in a percentage is
 * fixed where it was, without inventing a change in the history. With it,
 * the window takes {@code startDate} to {@code endDate}, null for one that
 * stays open, and the corrected dates are judged by the invariants.
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
