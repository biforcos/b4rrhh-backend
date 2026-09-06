package com.b4rrhh.employee.workcenter.application.usecase;

import com.b4rrhh.employee.workcenter.application.model.WorkCenterPlan;

/** What a change would do to the employee's work center series, without applying it (ADR-057). */
public interface PlanWorkCenterChangeUseCase {

    WorkCenterPlan plan(PlanWorkCenterChangeCommand command);
}
