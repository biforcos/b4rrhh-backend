package com.b4rrhh.employee.cost_center.infrastructure.web.dto;

import java.time.LocalDate;

/** A stretch of dates as a plan or an error names it: a window, a gap or an overlap. Null end means onwards. */
public record CostCenterDistributionPeriodResponse(
        LocalDate startDate,
        LocalDate endDate
) {
}
