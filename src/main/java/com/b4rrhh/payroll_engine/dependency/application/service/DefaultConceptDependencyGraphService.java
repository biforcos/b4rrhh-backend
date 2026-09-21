package com.b4rrhh.payroll_engine.dependency.application.service;

import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptDependencyGraph;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptDependencyGraphBuilder;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptNodeIdentity;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ConceptDependencyGraphService}.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Add all input concepts as nodes.</li>
 *   <li>Build a set of known node identities for source filtering.</li>
 *   <li>For each concept (target), read its active feed relations from the metamodel by
 *       technical object ID. Only relations whose source is a CONCEPT within the known set
 *       are added as FEED_DEPENDENCY edges.</li>
 *   <li>For each RATE_BY_QUANTITY or PERCENTAGE concept, read its operand definitions
 *       and add OPERAND_DEPENDENCY edges for each source concept in the known set.
 *       This ensures topological ordering places operand sources before their dependents
 *       even when operand relationships are not encoded as feed relations.</li>
 *   <li>Build and validate the graph (cycle detection).</li>
 * </ol>
 */
@Service
public class DefaultConceptDependencyGraphService implements ConceptDependencyGraphService {

    private static final Logger log = LoggerFactory.getLogger(DefaultConceptDependencyGraphService.class);

    @Override
    public ConceptDependencyGraph build(List<PayrollConcept> concepts, RuleSystemMetamodel metamodel) {
        if (concepts == null) {
            throw new IllegalArgumentException("concepts must not be null");
        }
        if (metamodel == null) {
            throw new IllegalArgumentException("metamodel must not be null");
        }

        Set<ConceptNodeIdentity> knownIdentities = new HashSet<>();
        for (PayrollConcept concept : concepts) {
            knownIdentities.add(new ConceptNodeIdentity(concept.getRuleSystemCode(), concept.getConceptCode()));
        }

        Map<String, PayrollConcept> conceptByCode = concepts.stream()
                .collect(Collectors.toMap(PayrollConcept::getConceptCode, c -> c));

        log.debug("[ENGINE]   GRAFO | {} nodos", concepts.size());

        ConceptDependencyGraphBuilder builder = new ConceptDependencyGraphBuilder()
                .addNodes(concepts);

        int feedEdges = 0;
        int operandEdges = 0;

        for (PayrollConcept target : concepts) {
            Long targetId = target.getObject().getId();
            if (targetId != null) {
                List<PayrollConceptFeedRelation> relations = metamodel.activeFeedsOf(targetId);

                for (PayrollConceptFeedRelation relation : relations) {
                    ConceptNodeIdentity sourceIdentity = new ConceptNodeIdentity(
                            relation.getSourceObject().getRuleSystemCode(),
                            relation.getSourceObject().getObjectCode()
                    );
                    if (knownIdentities.contains(sourceIdentity)) {
                        builder.addFeedRelation(relation);
                        feedEdges++;
                        log.debug("[ENGINE]     FEED_DEPENDENCY: {} → {} (invertSign={})",
                                relation.getSourceObject().getObjectCode(),
                                target.getConceptCode(),
                                relation.isInvertSign());
                    }
                }
            }

            CalculationType calcType = target.getCalculationType();
            if (calcType == CalculationType.RATE_BY_QUANTITY || calcType == CalculationType.PERCENTAGE
                    || calcType == CalculationType.GREATEST || calcType == CalculationType.LEAST
                    || calcType == CalculationType.QUOTIENT) {
                metamodel.requireSameRuleSystem(target.getRuleSystemCode());
                List<PayrollConceptOperand> operands = metamodel.operandsOf(target.getConceptCode());

                for (PayrollConceptOperand operand : operands) {
                    String sourceCode = operand.getSourceObject().getObjectCode();
                    ConceptNodeIdentity sourceIdentity =
                            new ConceptNodeIdentity(target.getRuleSystemCode(), sourceCode);
                    if (knownIdentities.contains(sourceIdentity)) {
                        PayrollConcept sourceConcept = conceptByCode.get(sourceCode);
                        builder.addOperandDependency(target, sourceConcept);
                        operandEdges++;
                        log.debug("[ENGINE]     OPERAND_DEPENDENCY: {} → {} (rol={})",
                                sourceCode, target.getConceptCode(), operand.getOperandRole());
                    }
                }
            }
        }

        log.debug("[ENGINE]   GRAFO listo | {} aristas FEED + {} aristas OPERAND", feedEdges, operandEdges);
        return builder.build();
    }
}
