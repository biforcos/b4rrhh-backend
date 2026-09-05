package com.b4rrhh.payroll_engine.concept.application.service;

import com.b4rrhh.payroll_engine.concept.application.usecase.CreatePayrollConceptCommand;
import com.b4rrhh.payroll_engine.concept.application.usecase.CreatePayrollConceptUseCase;
import com.b4rrhh.payroll_engine.concept.domain.exception.PayrollConceptAlreadyExistsException;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import com.b4rrhh.payroll_engine.object.domain.port.PayrollObjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CreatePayrollConceptService implements CreatePayrollConceptUseCase {

    private final PayrollConceptRepository conceptRepository;
    private final PayrollObjectRepository objectRepository;

    public CreatePayrollConceptService(
            PayrollConceptRepository conceptRepository,
            PayrollObjectRepository objectRepository
    ) {
        this.conceptRepository = conceptRepository;
        this.objectRepository = objectRepository;
    }

    @Override
    @Transactional
    public PayrollConcept create(CreatePayrollConceptCommand command) {
        if (conceptRepository.existsByBusinessKey(command.ruleSystemCode(), command.conceptCode())) {
            throw new PayrollConceptAlreadyExistsException(
                    command.ruleSystemCode(),
                    command.conceptCode()
            );
        }

        LocalDateTime now = LocalDateTime.now();

        PayrollObject newObject = new PayrollObject(
                null,
                command.ruleSystemCode(),
                PayrollObjectTypeCode.CONCEPT,
                command.conceptCode(),
                now,
                now
        );
        PayrollObject persistedObject = objectRepository.save(newObject);

        PayrollConcept concept = new PayrollConcept(
                persistedObject,
                command.conceptMnemonic(),
                command.calculationType(),
                command.functionalNature(),
                command.payslipOrderCode(),
                command.executionScope(),
                command.summary(),
                now,
                now
        );

        return conceptRepository.save(concept);
    }
}
