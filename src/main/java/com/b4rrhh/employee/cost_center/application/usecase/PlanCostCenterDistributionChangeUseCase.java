package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.model.CostCenterDistributionPlan;

public interface PlanCostCenterDistributionChangeUseCase {

    CostCenterDistributionPlan plan(PlanCostCenterDistributionChangeCommand command);
}
