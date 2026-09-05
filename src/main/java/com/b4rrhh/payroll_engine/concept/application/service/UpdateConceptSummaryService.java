package com.b4rrhh.payroll_engine.concept.application.service;

import com.b4rrhh.payroll_engine.concept.application.usecase.UpdateConceptSummaryCommand;
import com.b4rrhh.payroll_engine.concept.application.usecase.UpdateConceptSummaryUseCase;
import com.b4rrhh.payroll_engine.concept.domain.exception.PayrollConceptNotFoundException;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateConceptSummaryService implements UpdateConceptSummaryUseCase {

    private final PayrollConceptRepository conceptRepository;

    public UpdateConceptSummaryService(PayrollConceptRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    @Override
    @Transactional
    public PayrollConcept update(UpdateConceptSummaryCommand command) {
        PayrollConcept existing = conceptRepository
                .findByBusinessKey(command.ruleSystemCode(), command.conceptCode())
                .orElseThrow(() -> new PayrollConceptNotFoundException(
                        command.ruleSystemCode(), command.conceptCode()));

        PayrollConcept updated = new PayrollConcept(
                existing.getObject(),
                existing.getConceptMnemonic(),
                existing.getCalculationType(),
                existing.getFunctionalNature(),
                existing.getPayslipOrderCode(),
                existing.getExecutionScope(),
                command.summary(),
                existing.getCreatedAt(),
                null
        );
        return conceptRepository.save(updated);
    }
}
