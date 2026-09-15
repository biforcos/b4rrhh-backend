package com.b4rrhh.payroll_engine.object.application.service;

import com.b4rrhh.payroll_engine.object.application.usecase.CreateBindingRoleCommand;
import com.b4rrhh.payroll_engine.object.application.usecase.CreateBindingRoleUseCase;
import com.b4rrhh.payroll_engine.object.domain.exception.BindingRoleAlreadyExistsException;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import com.b4rrhh.payroll_engine.object.domain.port.PayrollObjectRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateBindingRoleService implements CreateBindingRoleUseCase {

    private final PayrollObjectRepository objectRepository;

    public CreateBindingRoleService(PayrollObjectRepository objectRepository) {
        this.objectRepository = objectRepository;
    }

    @Override
    public PayrollObject create(CreateBindingRoleCommand command) {
        if (objectRepository.existsByBusinessKey(
                command.ruleSystemCode(), PayrollObjectTypeCode.TABLE, command.bindingRoleCode())) {
            throw new BindingRoleAlreadyExistsException(command.ruleSystemCode(), command.bindingRoleCode());
        }
        return objectRepository.save(new PayrollObject(
                null, command.ruleSystemCode(), PayrollObjectTypeCode.TABLE, command.bindingRoleCode(), null, null
        ));
    }
}
