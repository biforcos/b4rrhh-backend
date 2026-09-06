package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;

public interface UpdateCostCenterDistributionUseCase {

    CostCenterDistributionWindow update(UpdateCostCenterDistributionCommand command);
}
