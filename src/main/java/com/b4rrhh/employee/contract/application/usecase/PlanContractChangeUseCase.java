package com.b4rrhh.employee.contract.application.usecase;

import com.b4rrhh.employee.contract.application.command.PlanContractChangeCommand;
import com.b4rrhh.employee.contract.application.model.ContractPlan;

public interface PlanContractChangeUseCase {

    ContractPlan plan(PlanContractChangeCommand command);
}
