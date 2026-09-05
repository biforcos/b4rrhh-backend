package com.b4rrhh.payroll_engine.dependency.application.service;

import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.FeedMode;
import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptFeedRelationRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptOperandRepository;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptDependencyGraph;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptNodeIdentity;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptDependencyGraphServiceTest {

    private static final String RULE_SYS = "ESP";
    private static final LocalDate REF = LocalDate.of(2026, 4, 1);

    // ── concepts with persisted IDs ──────────────────────────────────────────

    private static PayrollConcept diasPresencia() {
        return concept(1L, RULE_SYS, "T_DIAS_PRESENCIA_SEGMENTO",
                CalculationType.DIRECT_AMOUNT, FunctionalNature.INFORMATIONAL);
    }

    private static PayrollConcept precioDia() {
        return concept(2L, RULE_SYS, "T_PRECIO_DIA",
                CalculationType.DIRECT_AMOUNT, FunctionalNature.INFORMATIONAL);
    }

    private static PayrollConcept salarioBase() {
        return concept(3L, RULE_SYS, "SALARIO_BASE",
                CalculationType.RATE_BY_QUANTITY, FunctionalNature.EARNING);
    }

    private static PayrollConcept externalConcept() {
        return concept(99L, RULE_SYS, "EXTERNAL_CONCEPT",
                CalculationType.DIRECT_AMOUNT, FunctionalNature.INFORMATIONAL);
    }

    // ── tests ────────────────────────────────────────────────────────────────

    @Test
    void graphContainsAllInputConceptsAsNodes() {
        List<PayrollConcept> concepts = List.of(diasPresencia(), precioDia(), salarioBase());
        ConceptDependencyGraphService service = new DefaultConceptDependencyGraphService(emptyRepo(), emptyOperandRepo());

        ConceptDependencyGraph graph = service.build(concepts, REF);

        assertTrue(graph.getNodes().contains(new ConceptNodeIdentity(RULE_SYS, "T_DIAS_PRESENCIA_SEGMENTO")));
        assertTrue(graph.getNodes().contains(new ConceptNodeIdentity(RULE_SYS, "T_PRECIO_DIA")));
        assertTrue(graph.getNodes().contains(new ConceptNodeIdentity(RULE_SYS, "SALARIO_BASE")));
    }

    @Test
    void salarioBaseDependsOnBothTechnicalConceptsWhenRelationsPersisted() {
        List<PayrollConcept> concepts = List.of(diasPresencia(), precioDia(), salarioBase());
        ConceptDependencyGraphService service =
                new DefaultConceptDependencyGraphService(pocFeedRelationRepo(diasPresencia(), precioDia(), salarioBase()), emptyOperandRepo());

        ConceptDependencyGraph graph = service.build(concepts, REF);

        // SALARIO_BASE must come after both technical concepts in topological order
        List<ConceptNodeIdentity> order = graph.topologicalOrder();
        int idxSalarioBase   = indexOf(order, RULE_SYS, "SALARIO_BASE");
        int idxDiasPresencia = indexOf(order, RULE_SYS, "T_DIAS_PRESENCIA_SEGMENTO");
        int idxPrecioDia     = indexOf(order, RULE_SYS, "T_PRECIO_DIA");

        assertTrue(idxDiasPresencia < idxSalarioBase,
                "T_DIAS_PRESENCIA_SEGMENTO must be before SALARIO_BASE");
        assertTrue(idxPrecioDia < idxSalarioBase,
                "T_PRECIO_DIA must be before SALARIO_BASE");
    }

    @Test
    void relationsWhoseSourceIsNotInInputListAreIgnoredAndNoDependencyEdgeIsCreated() {
        // externalConcept feeds salarioBase in persistence, but is NOT in the concept list.
        PayrollConcept external = externalConcept();
        PayrollConcept salarioBase = salarioBase();

        // Build a repo that returns: salarioBase(id=3) is fed by external(id=99)
        Map<Long, List<PayrollConceptFeedRelation>> relsByTarget = new HashMap<>();
        relsByTarget.put(3L, List.of(feedRelation(external, salarioBase)));

        ConceptDependencyGraphService service =
                new DefaultConceptDependencyGraphService(repoFromMap(relsByTarget), emptyOperandRepo());

        // Input list does NOT include externalConcept
        List<PayrollConcept> concepts = List.of(diasPresencia(), precioDia(), salarioBase);
        ConceptDependencyGraph graph = service.build(concepts, REF);

        // External node must not appear in the graph
        assertFalse(graph.getNodes().contains(new ConceptNodeIdentity(RULE_SYS, "EXTERNAL_CONCEPT")),
                "external concept must not be added as a node");

        // No dependency edge must have been created from the external source toward SALARIO_BASE
        ConceptNodeIdentity salarioBaseIdentity = new ConceptNodeIdentity(RULE_SYS, "SALARIO_BASE");
        ConceptNodeIdentity externalIdentity    = new ConceptNodeIdentity(RULE_SYS, "EXTERNAL_CONCEPT");
        assertFalse(
                graph.getDependenciesOf(salarioBaseIdentity).contains(externalIdentity),
                "SALARIO_BASE must not have a dependency edge toward the ignored external concept");
    }

    @Test
    void conceptsWithNullIdAreSkippedDuringRelationLookupButStillPresentAsNodes() {
        // A concept without a persisted ID must still appear as a graph node,
        // but must never trigger a relation lookup (which would require a non-null technical ID).
        PayrollConcept unpersisted = concept(null, RULE_SYS, "T_DIAS_PRESENCIA_SEGMENTO",
                CalculationType.DIRECT_AMOUNT, FunctionalNature.INFORMATIONAL);
        List<PayrollConcept> concepts = List.of(unpersisted);

        // Repo stub that would record calls if invoked; we use an empty repo that never returns data.
        ConceptDependencyGraphService service = new DefaultConceptDependencyGraphService(emptyRepo(), emptyOperandRepo());

        ConceptDependencyGraph graph = service.build(concepts, REF);

        // The concept must still be a node.
        assertTrue(graph.getNodes().contains(
                new ConceptNodeIdentity(RULE_SYS, "T_DIAS_PRESENCIA_SEGMENTO")),
                "null-ID concept must still be present as a graph node");

        // No dependency edges must have been created for it.
        assertTrue(
                graph.getDependenciesOf(new ConceptNodeIdentity(RULE_SYS, "T_DIAS_PRESENCIA_SEGMENTO")).isEmpty(),
                "null-ID concept must have no dependency edges");
    }

    @Test
    void nullConceptListIsRejected() {
        ConceptDependencyGraphService service = new DefaultConceptDependencyGraphService(emptyRepo(), emptyOperandRepo());
        assertThrows(IllegalArgumentException.class, () -> service.build(null, REF));
    }

    @Test
    void nullReferenceDateIsRejected() {
        ConceptDependencyGraphService service = new DefaultConceptDependencyGraphService(emptyRepo(), emptyOperandRepo());
        assertThrows(IllegalArgumentException.class, () -> service.build(List.of(), null));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static int indexOf(List<ConceptNodeIdentity> order, String ruleSystem, String code) {
        ConceptNodeIdentity target = new ConceptNodeIdentity(ruleSystem, code);
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i).equals(target)) return i;
        }
        throw new AssertionError("Node not found in topological order: " + ruleSystem + "/" + code);
    }

    private static PayrollConcept concept(Long id, String ruleSystemCode, String conceptCode,
                                          CalculationType calculationType,
                                          FunctionalNature nature) {
        PayrollObject object = new PayrollObject(id, ruleSystemCode, PayrollObjectTypeCode.CONCEPT,
                conceptCode, null, null);
        return new PayrollConcept(object, conceptCode, calculationType, nature,
                null, ExecutionScope.SEGMENT, null, null);
    }

    private static PayrollConceptFeedRelation feedRelation(PayrollConcept source, PayrollConcept target) {
        return new PayrollConceptFeedRelation(
                null,
                source.getObject(),
                target.getObject(),
                FeedMode.FEED_BY_SOURCE,
                null,
                false,
                LocalDate.of(2020, 1, 1),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    /**
     * Repo stub seeded with the 2 PoC feed relations:
     * T_DIAS_PRESENCIA_SEGMENTO → SALARIO_BASE
     * T_PRECIO_DIA              → SALARIO_BASE
     */
    private static PayrollConceptFeedRelationRepository pocFeedRelationRepo(
            PayrollConcept diasPresencia,
            PayrollConcept precioDia,
            PayrollConcept salarioBase
    ) {
        Map<Long, List<PayrollConceptFeedRelation>> relsByTarget = new HashMap<>();
        relsByTarget.put(salarioBase.getObject().getId(), List.of(
                feedRelation(diasPresencia, salarioBase),
                feedRelation(precioDia, salarioBase)
        ));
        return repoFromMap(relsByTarget);
    }

    private static PayrollConceptFeedRelationRepository repoFromMap(
            Map<Long, List<PayrollConceptFeedRelation>> relsByTarget
    ) {
        return new PayrollConceptFeedRelationRepository() {
            @Override
            public PayrollConceptFeedRelation save(PayrollConceptFeedRelation r) {
                throw new UnsupportedOperationException();
            }
            @Override
            public List<PayrollConceptFeedRelation> findActiveByTargetObjectId(Long id, LocalDate date) {
                return relsByTarget.getOrDefault(id, Collections.emptyList());
            }
            @Override
            public List<PayrollConceptFeedRelation> findByRuleSystemCodeAndTargetConceptCode(String rs, String code) {
                return Collections.emptyList();
            }
            @Override
            public void deleteAllByRuleSystemCodeAndTargetConceptCode(String rs, String code) {
                /* no-op */
            }
        };
    }

    private static PayrollConceptFeedRelationRepository emptyRepo() {
        return repoFromMap(Collections.emptyMap());
    }

    private static PayrollConceptOperandRepository emptyOperandRepo() {
        return new PayrollConceptOperandRepository() {
            @Override
            public PayrollConceptOperand save(PayrollConceptOperand o) { throw new UnsupportedOperationException(); }
            @Override
            public List<PayrollConceptOperand> findByTarget(String rs, String code) { return Collections.emptyList(); }
            @Override
            public List<PayrollConceptOperand> findByRuleSystemCodeAndConceptCode(String rs, String code) { return Collections.emptyList(); }
            @Override
            public void deleteAllByRuleSystemCodeAndConceptCode(String rs, String code) { /* no-op */ }
        };
    }
}
