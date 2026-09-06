package com.b4rrhh.employee.cost_center.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.List;

/** {@code endDate} is omitted for a window that stays open (ADR-057). */
public record CreateCostCenterDistributionRequest(
        LocalDate startDate,
        LocalDate endDate,
        List<CostCenterDistributionItemRequest> items
) {
}
