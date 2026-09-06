package com.b4rrhh.employee.workcenter.infrastructure.web.dto;

/** The one existing assignment a plan would move on its own: only its end date changes. */
public record WorkCenterPlanAdjustmentResponse(
        Integer workCenterAssignmentNumber,
        WorkCenterPeriodResponse before,
        WorkCenterPeriodResponse after
) {
}
