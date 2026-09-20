package com.b4rrhh.payroll_engine.planning.application.service;

import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import com.b4rrhh.payroll_engine.planning.domain.exception.MissingDependencyConceptDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Default implementation of {@link EligibleConceptExpansionService}.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Seed the working queue with all input {@code eligibleConcepts}.</li>
 *   <li>For each concept dequeued:
 *     <ul>
 *       <li><strong>Operand discovery:</strong> read the operand definitions from the
 *           metamodel and enqueue each source concept that has not yet been seen.</li>
 *       <li><strong>Feed-relation discovery:</strong> read the active inbound feed relations
 *           from the metamodel. Only CONCEPT-typed sources within the same rule system are
 *           followed; CONSTANT and TABLE sources are silently skipped because they are not
 *           concept definitions.</li>
 *     </ul>
 *   </li>
 *   <li>Continue until the queue is empty. Return all loaded concepts in discovery order.</li>
 * </ol>
 */
@Service
public class DefaultEligibleConceptExpansionService implements EligibleConceptExpansionService {

    private static final Logger log = LoggerFactory.getLogger(DefaultEligibleConceptExpansionService.class);

    @Override
    public List<PayrollConcept> expand(List<PayrollConcept> eligibleConcepts, RuleSystemMetamodel metamodel) {
        Map<String, PayrollConcept> loaded = new LinkedHashMap<>();
        Queue<PayrollConcept> toProcess = new ArrayDeque<>();

        for (PayrollConcept concept : eligibleConcepts) {
            if (!loaded.containsKey(concept.getConceptCode())) {
                loaded.put(concept.getConceptCode(), concept);
                toProcess.add(concept);
            }
        }
        log.debug("[ENGINE]   BFS init | {} conceptos elegibles en cola → [{}]",
                loaded.size(),
                loaded.keySet().stream().collect(java.util.stream.Collectors.joining(", ")));

        while (!toProcess.isEmpty()) {
            PayrollConcept current = toProcess.poll();
            String ruleSystemCode = current.getRuleSystemCode();
            metamodel.requireSameRuleSystem(ruleSystemCode);
            String currentCode = current.getConceptCode();
            log.debug("[ENGINE]   BFS procesando {} ({})", currentCode, current.getCalculationType());

            List<PayrollConceptOperand> operands = metamodel.operandsOf(currentCode);
            for (PayrollConceptOperand operand : operands) {
                String sourceCode = operand.getSourceObject().getObjectCode();
                if (!loaded.containsKey(sourceCode)) {
                    PayrollConcept sourceConcept = metamodel
                            .findConcept(sourceCode)
                            .orElseThrow(() -> new MissingDependencyConceptDefinitionException(
                                    ruleSystemCode, sourceCode));
                    loaded.put(sourceCode, sourceConcept);
                    toProcess.add(sourceConcept);
                    log.debug("[ENGINE]     operando {} → descubierto {} ({})",
                            operand.getOperandRole(), sourceCode, sourceConcept.getCalculationType());
                } else {
                    log.debug("[ENGINE]     operando {} → {} (ya cargado)", operand.getOperandRole(), sourceCode);
                }
            }

            // AGGREGATE sources are optional contributors, not structural dependencies.
            // Pulling them in here would include non-eligible concepts whenever they have
            // a feed relation to an eligible aggregate — defeating the eligibility gate.
            //
            // Esto es deliberado y se queda (backend#110). Un agregado suma lo que se le ha
            // asignado; ir a buscar sus fuentes y ejecutarlas por su cuenta seria que el catalogo
            // decidiera que se calcula, y eso lo decide la asignacion.
            //
            // La consecuencia hay que saberla, porque no es evidente y costo medio dia:
            //
            //   Un concepto que SOLO existe como fuente de un AGGREGATE, y que no esta asignado a
            //   nadie por su cuenta, NO SE EJECUTA NUNCA. No falla nada: la corrida termina
            //   COMPLETED y los recibos salen identicos a los de antes.
            //
            // Y un recibo identico no se distingue de «este cambio no tenia que mover nada», que
            // es un caso legitimo. Por eso el silencio no se queda: UnreachableConceptFinder hace
            // esa pregunta sobre la reglamentacion entera y el lanzamiento la avisa. Lo que no
            // hace —ni debe— es expandir aqui para taparlo.
            if (current.getCalculationType() == CalculationType.AGGREGATE) {
                log.debug("[ENGINE]     {} AGGREGATE, omitiendo expansión de fuentes", currentCode);
                continue;
            }

            Long objectId = current.getObject().getId();
            if (objectId == null) {
                log.debug("[ENGINE]     sin objectId, sin feed relations");
                continue;
            }

            List<PayrollConceptFeedRelation> relations = metamodel.activeFeedsOf(objectId);
            for (PayrollConceptFeedRelation relation : relations) {
                PayrollObject source = relation.getSourceObject();
                if (source.getObjectTypeCode() != PayrollObjectTypeCode.CONCEPT) {
                    log.debug("[ENGINE]     feed source {} omitido (tipo={})",
                            source.getObjectCode(), source.getObjectTypeCode());
                    continue;
                }
                if (!ruleSystemCode.equals(source.getRuleSystemCode())) {
                    continue;
                }
                String sourceCode = source.getObjectCode();
                if (!loaded.containsKey(sourceCode)) {
                    PayrollConcept sourceConcept = metamodel
                            .findConcept(sourceCode)
                            .orElseThrow(() -> new MissingDependencyConceptDefinitionException(
                                    ruleSystemCode, sourceCode));
                    loaded.put(sourceCode, sourceConcept);
                    toProcess.add(sourceConcept);
                    log.debug("[ENGINE]     feed source → descubierto {} ({})",
                            sourceCode, sourceConcept.getCalculationType());
                } else {
                    log.debug("[ENGINE]     feed source → {} (ya cargado)", sourceCode);
                }
            }
        }

        log.debug("[ENGINE]   BFS completo | {} conceptos expandidos → [{}]",
                loaded.size(),
                loaded.keySet().stream().collect(java.util.stream.Collectors.joining(", ")));
        return new ArrayList<>(loaded.values());
    }
}
