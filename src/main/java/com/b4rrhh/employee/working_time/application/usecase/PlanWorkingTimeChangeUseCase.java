package com.b4rrhh.employee.working_time.application.usecase;

import com.b4rrhh.employee.working_time.application.model.WorkingTimePlan;

public interface PlanWorkingTimeChangeUseCase {

    WorkingTimePlan plan(PlanWorkingTimeChangeCommand command);
}
