package com.b4rrhh.employee.contract.application.usecase;

import com.b4rrhh.employee.contract.application.command.DeleteContractCommand;

public interface DeleteContractUseCase {

    void delete(DeleteContractCommand command);
}
