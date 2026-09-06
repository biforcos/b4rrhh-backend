package com.b4rrhh.employee.cost_center.application.usecase;

import java.time.LocalDate;
import java.util.List;

/**
 * A new distribution window: the lines that will share {@code startDate},
 * and the {@code endDate} they share too, null for a window that stays open
 * (ADR-057, decision 2). Adding one closes the window in force the day
 * before; anything else the new window collides with is the invariant's call.
 */
public record CreateCostCenterDistributionCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        LocalDate startDate,
        LocalDate endDate,
        List<CostCenterDistributionItem> items
) {
}
