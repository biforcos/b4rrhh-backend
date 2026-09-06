package com.b4rrhh.employee.cost_center.domain.model;

import java.time.LocalDate;

/**
 * A stretch of dates in the cost center series (ADR-057): a distribution
 * window as a plan or an error names it, a gap the series leaves inside the
 * presence, or the dates two windows would share. A window has no number:
 * what identifies it is the day it starts, so its dates are all a plan needs
 * to name it. An open end date means "onwards".
 */
public record CostCenterDistributionPeriod(
        LocalDate startDate,
        LocalDate endDate
) {

    public CostCenterDistributionPeriod {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }
    }
}
