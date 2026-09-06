package com.b4rrhh.employee.cost_center.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * A correction of a distribution window (ADR-057, decision 3). Omit
 * {@code startDate} to keep the dates the window has and correct only its
 * lines; give it to move the window, with {@code endDate} absent for one that
 * stays open. {@code items} replace the current lines as a set.
 */
public record UpdateCostCenterDistributionRequest(
        LocalDate startDate,
        LocalDate endDate,
        List<CostCenterDistributionItemRequest> items
) {
}
