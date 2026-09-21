package com.b4rrhh.payroll_engine.eligibility.application.service;

import com.b4rrhh.payroll_engine.concept.domain.exception.PayrollConceptNotFoundException;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import com.b4rrhh.payroll_engine.eligibility.application.usecase.CreateConceptAssignmentCommand;
import com.b4rrhh.payroll_engine.eligibility.application.usecase.CreateConceptAssignmentUseCase;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment;
import com.b4rrhh.payroll_engine.eligibility.domain.port.ConceptAssignmentRepository;
import com.b4rrhh.payroll_engine.metamodel.domain.model.MetamodelValidityWindow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Persists a new {@link ConceptAssignment} under the requested rule system after verifying
 * that the target concept exists. Domain-level invariants (required fields, validity
 * window, blank-to-null normalisation) are enforced by the aggregate constructor.
 */
@Service
public class CreateConceptAssignmentService implements CreateConceptAssignmentUseCase {

    private final ConceptAssignmentRepository assignmentRepository;
    private final PayrollConceptRepository conceptRepository;

    public CreateConceptAssignmentService(
            ConceptAssignmentRepository assignmentRepository,
            PayrollConceptRepository conceptRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.conceptRepository = conceptRepository;
    }

    @Override
    @Transactional
    public ConceptAssignment create(CreateConceptAssignmentCommand command) {
        if (!conceptRepository.existsByBusinessKey(command.ruleSystemCode(), command.conceptCode())) {
            throw new PayrollConceptNotFoundException(command.ruleSystemCode(), command.conceptCode());
        }

        MetamodelValidityWindow.requireWholePeriods(
                command.validFrom(),
                command.validTo(),
                "la asignacion del concepto " + command.conceptCode(),
                "validFrom",
                "validTo"
        );

        LocalDateTime now = LocalDateTime.now();
        // createdAt sigue sin zona y updatedAt la lleva desde el backend#116:
        // solo la segunda entra en la comparacion que decide si un recibo sigue vigente.
        Instant ahora = Instant.now();
        ConceptAssignment assignment = new ConceptAssignment(
                null,
                command.ruleSystemCode(),
                command.conceptCode(),
                command.companyCode(),
                command.agreementCode(),
                command.employeeTypeCode(),
                command.validFrom(),
                command.validTo(),
                command.priority(),
                now,
                ahora
        );
        return assignmentRepository.save(assignment);
    }
}
