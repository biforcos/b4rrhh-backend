package com.b4rrhh.employee.labor_classification.application.usecase;

import com.b4rrhh.employee.labor_classification.application.command.PlanLaborClassificationChangeCommand;
import com.b4rrhh.employee.labor_classification.application.model.LaborClassificationPlan;

public interface PlanLaborClassificationChangeUseCase {

    LaborClassificationPlan plan(PlanLaborClassificationChangeCommand command);
}
