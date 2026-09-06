package com.b4rrhh.employee.workcenter.domain.model;

import java.time.LocalDate;

/**
 * A stretch of dates in the work center series: a gap the series leaves
 * inside the presence, or the dates two assignments would share. An open end
 * date means "onwards".
 */
public record WorkCenterPeriod(
        LocalDate startDate,
        LocalDate endDate
) {

    public WorkCenterPeriod {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }
    }
}
