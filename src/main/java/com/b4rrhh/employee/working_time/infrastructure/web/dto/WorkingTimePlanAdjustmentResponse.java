package com.b4rrhh.employee.working_time.infrastructure.web.dto;

/** The one existing working time a plan would move on its own: only its end date changes. */
public record WorkingTimePlanAdjustmentResponse(
        Integer workingTimeNumber,
        WorkingTimePeriodResponse before,
        WorkingTimePeriodResponse after
) {
}
