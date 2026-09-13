package com.b4rrhh.payroll_engine.metamodel.infrastructure.persistence;

import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptFeedRelationRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptOperandRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment;
import com.b4rrhh.payroll_engine.eligibility.domain.port.ConceptAssignmentRepository;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import com.b4rrhh.payroll_engine.metamodel.domain.port.RuleSystemMetamodelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Lee la reglamentación de un sistema de reglas en cuatro consultas.
 *
 * <p><b>Cuatro, y no una por operando.</b> Las cuatro se lanzan una vez por ejecución, no
 * una vez por unidad. Lo que antes eran ~349 lecturas por unidad —el mismo grafo de 36
 * conceptos preguntado 873 veces seguidas— pasa a ser un dato que se carga al empezar y
 * se lee de memoria. Ver {@link RuleSystemMetamodel} para por qué la carga única es una
 * regla del modelo y no solo una optimización.
 *
 * <p>Las cuatro consultas traen sus {@code payroll_object} con {@code join fetch}: sin eso
 * el mapeo a dominio volvería a pedir un select por objeto y la carga única no ahorraría
 * nada.
 */
@Component
public class RuleSystemMetamodelPersistenceAdapter implements RuleSystemMetamodelRepository {

    private static final Logger log = LoggerFactory.getLogger(RuleSystemMetamodelPersistenceAdapter.class);

    private final PayrollConceptRepository conceptRepository;
    private final PayrollConceptOperandRepository operandRepository;
    private final PayrollConceptFeedRelationRepository feedRelationRepository;
    private final ConceptAssignmentRepository conceptAssignmentRepository;

    public RuleSystemMetamodelPersistenceAdapter(
            PayrollConceptRepository conceptRepository,
            PayrollConceptOperandRepository operandRepository,
            PayrollConceptFeedRelationRepository feedRelationRepository,
            ConceptAssignmentRepository conceptAssignmentRepository
    ) {
        this.conceptRepository = conceptRepository;
        this.operandRepository = operandRepository;
        this.feedRelationRepository = feedRelationRepository;
        this.conceptAssignmentRepository = conceptAssignmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RuleSystemMetamodel load(String ruleSystemCode, LocalDate referenceDate) {
        if (ruleSystemCode == null || ruleSystemCode.isBlank()) {
            throw new IllegalArgumentException("ruleSystemCode must not be blank");
        }
        if (referenceDate == null) {
            throw new IllegalArgumentException("referenceDate must not be null");
        }

        List<PayrollConcept> concepts = conceptRepository.findAllByRuleSystemCode(ruleSystemCode);
        List<PayrollConceptOperand> operands = operandRepository.findAllByRuleSystemCode(ruleSystemCode);
        List<PayrollConceptFeedRelation> feedRelations =
                feedRelationRepository.findAllActiveByRuleSystemCode(ruleSystemCode, referenceDate);
        List<ConceptAssignment> assignments =
                conceptAssignmentRepository.findAllValidByRuleSystemCode(ruleSystemCode, referenceDate);

        RuleSystemMetamodel metamodel = new RuleSystemMetamodel(
                ruleSystemCode, referenceDate, concepts, operands, feedRelations, assignments);

        log.info("[ENGINE] Reglamentación cargada para la ejecución | {}", metamodel);
        return metamodel;
    }
}
