package com.b4rrhh.payroll_engine.eligibility.application.service;

import com.b4rrhh.payroll_engine.eligibility.application.usecase.UpdateConceptAssignmentCommand;
import com.b4rrhh.payroll_engine.eligibility.application.usecase.UpdateConceptAssignmentUseCase;
import com.b4rrhh.payroll_engine.eligibility.domain.exception.ConceptAssignmentNotFoundException;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment;
import com.b4rrhh.payroll_engine.eligibility.domain.port.ConceptAssignmentRepository;
import com.b4rrhh.payroll_engine.metamodel.domain.model.MetamodelValidityWindow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateConceptAssignmentService implements UpdateConceptAssignmentUseCase {

    private final ConceptAssignmentRepository assignmentRepository;

    public UpdateConceptAssignmentService(ConceptAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    @Transactional
    public ConceptAssignment update(UpdateConceptAssignmentCommand command) {
        Long id = parseAssignmentCode(command.ruleSystemCode(), command.assignmentCode());
        ConceptAssignment existing = assignmentRepository
                .findByIdAndRuleSystemCode(id, command.ruleSystemCode())
                .orElseThrow(() -> new ConceptAssignmentNotFoundException(
                        command.ruleSystemCode(), command.assignmentCode()));

        MetamodelValidityWindow.requireWholePeriods(
                command.validFrom(),
                command.validTo(),
                "la asignacion " + command.assignmentCode(),
                "validFrom",
                "validTo"
        );

        ConceptAssignment updated = new ConceptAssignment(
                existing.getId(),
                existing.getRuleSystemCode(),
                existing.getConceptCode(),
                command.companyCode(),
                command.agreementCode(),
                command.employeeTypeCode(),
                command.validFrom(),
                command.validTo(),
                command.priority(),
                existing.getCreatedAt(),
                null
        );
        return assignmentRepository.save(updated);
    }

    private Long parseAssignmentCode(String ruleSystemCode, String assignmentCode) {
        if (assignmentCode == null || assignmentCode.isBlank()) {
            throw new ConceptAssignmentNotFoundException(ruleSystemCode, assignmentCode);
        }
        try {
            return Long.valueOf(assignmentCode.trim());
        } catch (NumberFormatException ex) {
            throw new ConceptAssignmentNotFoundException(ruleSystemCode, assignmentCode);
        }
    }
}
