package com.b4rrhh.payroll_engine.planning.application.service;

import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.FeedMode;
import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptFeedRelationRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptOperandRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import com.b4rrhh.payroll_engine.dependency.application.service.DefaultConceptDependencyGraphService;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptDependencyGraph;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptNodeIdentity;
import com.b4rrhh.payroll_engine.eligibility.application.service.DefaultConceptEligibilityResolver;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment;
import com.b4rrhh.payroll_engine.eligibility.domain.model.EmployeeAssignmentContext;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ResolvedConceptAssignment;
import com.b4rrhh.payroll_engine.eligibility.domain.port.ConceptAssignmentRepository;
import com.b4rrhh.payroll_engine.execution.application.service.ExecutionPlanBuilder;
import com.b4rrhh.payroll_engine.execution.domain.model.ConceptExecutionPlanEntry;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import com.b4rrhh.payroll_engine.planning.domain.exception.MissingDependencyConceptDefinitionException;
import com.b4rrhh.payroll_engine.planning.domain.exception.MissingEligibleConceptDefinitionException;
import com.b4rrhh.payroll_engine.planning.domain.model.EligibleExecutionPlanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DefaultEligibleExecutionPlanBuilder}.
 *
 * <p>All collaborators are replaced with in-memory fakes — no Spring context is needed.
 * Tests validate that:
 * <ul>
 *   <li>applicable assignments are preserved in the result</li>
 *   <li>directly eligible concepts are correctly identified</li>
 *   <li>structural dependencies (including technical concepts) are pulled in during expansion</li>
 *   <li>technical dependencies are NOT present in eligibility assignments</li>
 *   <li>the execution plan is in topological order</li>
 *   <li>fail-fast exceptions are thrown for missing concept definitions</li>
 * </ul>
 */
class DefaultEligibleExecutionPlanBuilderTest {

    private static final String RS = "ESP";
    private static final LocalDate REF = LocalDate.of(2025, 3, 1);
    private static final LocalDateTime NOW = LocalDateTime.of(2025, 1, 1, 0, 0);

    private static final EmployeeAssignmentContext CONTEXT =
            new EmployeeAssignmentContext(RS, "EMP1", "METAL", "INDEFINIDO");

    // ── concept IDs (used to wire feed relations in the fake repo) ──────────

    private static final long ID_SALARIO_BASE            = 1L;
    private static final long ID_PLUS_TRANSPORTE         = 2L;
    private static final long ID_RETENCION_IRPF          = 3L;
    private static final long ID_TOTAL_DEVENGOS          = 4L;
    private static final long ID_T_DIAS                  = 5L;
    private static final long ID_T_PRECIO_DIA            = 6L;
    private static final long ID_T_PRECIO_TRANSPORTE     = 7L;
    private static final long ID_T_PCT_IRPF              = 8L;

    // ── concept definitions ─────────────────────────────────────────────────

    private PayrollConcept salarioBase;
    private PayrollConcept plusTransporte;
    private PayrollConcept retencionIrpf;
    private PayrollConcept totalDevengos;
    private PayrollConcept tDias;
    private PayrollConcept tPrecioDia;
    private PayrollConcept tPrecioTransporte;
    private PayrollConcept tPctIrpf;

    // ── fakes ───────────────────────────────────────────────────────────────

    private FakeConceptRepository conceptRepo;
    private FakeFeedRelationRepository feedRepo;
    private DefaultConceptDependencyGraphService graphService;

    @BeforeEach
    void setUp() {
        salarioBase        = concept(ID_SALARIO_BASE,        "SALARIO_BASE");
        plusTransporte     = concept(ID_PLUS_TRANSPORTE,     "PLUS_TRANSPORTE");
        retencionIrpf      = concept(ID_RETENCION_IRPF,      "RETENCION_IRPF_TRAMO");
        totalDevengos      = concept(ID_TOTAL_DEVENGOS,       "TOTAL_DEVENGOS_SEGMENTO");
        tDias              = concept(ID_T_DIAS,               "T_DIAS_PRESENCIA_SEGMENTO");
        tPrecioDia         = concept(ID_T_PRECIO_DIA,         "T_PRECIO_DIA");
        tPrecioTransporte  = concept(ID_T_PRECIO_TRANSPORTE,  "T_PRECIO_TRANSPORTE");
        tPctIrpf           = concept(ID_T_PCT_IRPF,           "T_PCT_IRPF");

        conceptRepo = new FakeConceptRepository(List.of(
                salarioBase, plusTransporte, retencionIrpf, totalDevengos,
                tDias, tPrecioDia, tPrecioTransporte, tPctIrpf
        ));

        // Feed relations (target ← source):
        //   SALARIO_BASE(1)            ← T_DIAS(5), T_PRECIO_DIA(6)
        //   PLUS_TRANSPORTE(2)         ← T_PRECIO_TRANSPORTE(7)
        //   RETENCION_IRPF_TRAMO(3)   ← TOTAL_DEVENGOS(4), T_PCT_IRPF(8)
        //   TOTAL_DEVENGOS_SEGMENTO(4) ← SALARIO_BASE(1), PLUS_TRANSPORTE(2)
        feedRepo = new FakeFeedRelationRepository(Map.of(
                ID_SALARIO_BASE,    List.of(feedRel(tDias, salarioBase), feedRel(tPrecioDia, salarioBase)),
                ID_PLUS_TRANSPORTE, List.of(feedRel(tPrecioTransporte, plusTransporte)),
                ID_RETENCION_IRPF,  List.of(feedRel(totalDevengos, retencionIrpf), feedRel(tPctIrpf, retencionIrpf)),
                ID_TOTAL_DEVENGOS,  List.of(feedRel(salarioBase, totalDevengos), feedRel(plusTransporte, totalDevengos))
        ));

        graphService = new DefaultConceptDependencyGraphService(feedRepo, emptyOperandRepo());
    }

    // ── test: integration — expansion and plan construction ─────────────────

    @Test
    void eligibleConcepts_expandedWithDependencies_planIsTopologicallyOrdered() {
        DefaultEligibleExecutionPlanBuilder builder = builderWith(
                eligibilityResolverWith("SALARIO_BASE", "PLUS_TRANSPORTE", "RETENCION_IRPF_TRAMO")
        );

        EligibleExecutionPlanResult result = builder.build(CONTEXT, REF);

        // 1. Applicable assignments: exactly the 3 directly eligible codes
        assertEquals(3, result.applicableAssignments().size());
        Set<String> assignedCodes = codeSetOf(result.applicableAssignments());
        assertTrue(assignedCodes.containsAll(
                Set.of("SALARIO_BASE", "PLUS_TRANSPORTE", "RETENCION_IRPF_TRAMO")));

        // 2. Eligible concepts: only the 3 directly assigned
        assertEquals(3, result.eligibleConcepts().size());
        Set<String> eligibleCodes = conceptCodes(result.eligibleConcepts());
        assertEquals(Set.of("SALARIO_BASE", "PLUS_TRANSPORTE", "RETENCION_IRPF_TRAMO"), eligibleCodes);

        // 3. Expanded concepts: all 8 (eligible + all dependencies)
        assertEquals(8, result.expandedConcepts().size());
        Set<String> expandedCodes = conceptCodes(result.expandedConcepts());
        assertTrue(expandedCodes.containsAll(Set.of(
                "SALARIO_BASE", "PLUS_TRANSPORTE", "RETENCION_IRPF_TRAMO",
                "TOTAL_DEVENGOS_SEGMENTO",
                "T_DIAS_PRESENCIA_SEGMENTO", "T_PRECIO_DIA",
                "T_PRECIO_TRANSPORTE", "T_PCT_IRPF"
        )));

        // 4. Technical dependencies are in the expanded set but NOT in eligibility assignments
        assertFalse(assignedCodes.contains("T_DIAS_PRESENCIA_SEGMENTO"),
                "Technical concepts must not appear as eligibility assignments");
        assertFalse(assignedCodes.contains("T_PRECIO_DIA"));
        assertFalse(assignedCodes.contains("T_PRECIO_TRANSPORTE"));
        assertFalse(assignedCodes.contains("T_PCT_IRPF"));
        assertFalse(assignedCodes.contains("TOTAL_DEVENGOS_SEGMENTO"));

        // 5. Execution plan: one entry per expanded concept
        assertEquals(8, result.executionPlan().size());

        // 6. Topological order: dependencies must appear before their dependents
        List<String> planCodes = result.executionPlan().stream()
                .map(e -> e.identity().getConceptCode())
                .collect(Collectors.toList());

        assertBefore(planCodes, "T_DIAS_PRESENCIA_SEGMENTO",  "SALARIO_BASE");
        assertBefore(planCodes, "T_PRECIO_DIA",               "SALARIO_BASE");
        assertBefore(planCodes, "T_PRECIO_TRANSPORTE",        "PLUS_TRANSPORTE");
        assertBefore(planCodes, "SALARIO_BASE",               "TOTAL_DEVENGOS_SEGMENTO");
        assertBefore(planCodes, "PLUS_TRANSPORTE",            "TOTAL_DEVENGOS_SEGMENTO");
        assertBefore(planCodes, "TOTAL_DEVENGOS_SEGMENTO",    "RETENCION_IRPF_TRAMO");
        assertBefore(planCodes, "T_PCT_IRPF",                 "RETENCION_IRPF_TRAMO");
    }

    // ── test: empty eligibility → empty result ───────────────────────────────

    @Test
    void noEligibleAssignments_returnsEmptyResult() {
        DefaultEligibleExecutionPlanBuilder builder = builderWith(
                eligibilityResolverWith()
        );

        EligibleExecutionPlanResult result = builder.build(CONTEXT, REF);

        assertTrue(result.applicableAssignments().isEmpty());
        assertTrue(result.eligibleConcepts().isEmpty());
        assertTrue(result.expandedConcepts().isEmpty());
        assertTrue(result.executionPlan().isEmpty());
    }

    // ── test: non-eligible concept that feeds an AGGREGATE must not be pulled in ─

    /**
     * Regression test: a concept that has a feed relation into an AGGREGATE target
     * but has NO eligibility assignment must NOT appear in the execution plan.
     *
     * <p>Before the fix, the BFS expansion followed feed sources of AGGREGATE concepts,
     * which caused non-eligible concepts (e.g. overtime hours) to be pulled into the
     * plan whenever they were wired to an eligible aggregate (e.g. BRUTO).
     */
    @Test
    void nonEligibleConceptFeedingAggregate_isNotPulledIntoExpansion() {
        long ID_HORAS_EXTRA = 10L;
        long ID_BRUTO       = 11L;

        PayrollConcept salario    = concept(ID_SALARIO_BASE, "SALARIO_BASE");
        PayrollConcept horasExtra = concept(ID_HORAS_EXTRA,  "HORAS_EXTRA_201");
        PayrollConcept bruto      = concept(ID_BRUTO,        "BRUTO", CalculationType.AGGREGATE);

        FakeConceptRepository repo = new FakeConceptRepository(List.of(salario, horasExtra, bruto));

        // Both SALARIO_BASE and HORAS_EXTRA_201 feed into BRUTO (the aggregate).
        // HORAS_EXTRA_201 has no eligibility assignment — it must not appear in the plan.
        FakeFeedRelationRepository feeds = new FakeFeedRelationRepository(Map.of(
                ID_BRUTO, List.of(feedRel(salario, bruto), feedRel(horasExtra, bruto))
        ));

        DefaultConceptDependencyGraphService gs = new DefaultConceptDependencyGraphService(feeds, emptyOperandRepo());
        DefaultEligibleConceptExpansionService es = new DefaultEligibleConceptExpansionService(repo, feeds, emptyOperandRepo());

        DefaultConceptEligibilityResolver eligibility = eligibilityResolverWith(repo, "SALARIO_BASE", "BRUTO");

        DefaultEligibleExecutionPlanBuilder builder = new DefaultEligibleExecutionPlanBuilder(
                eligibility, repo, es, gs, simplePlanBuilder());

        EligibleExecutionPlanResult result = builder.build(CONTEXT, REF);

        Set<String> expandedCodes = conceptCodes(result.expandedConcepts());
        assertFalse(expandedCodes.contains("HORAS_EXTRA_201"),
                "Non-eligible concept must not be pulled in via AGGREGATE feed relation");
        assertTrue(expandedCodes.contains("SALARIO_BASE"));
        assertTrue(expandedCodes.contains("BRUTO"));

        Set<String> planCodes = result.executionPlan().stream()
                .map(e -> e.identity().getConceptCode())
                .collect(Collectors.toSet());
        assertFalse(planCodes.contains("HORAS_EXTRA_201"),
                "Non-eligible concept must not appear in the execution plan");
        assertEquals(2, result.executionPlan().size());
    }

    // ── test: missing eligible concept definition → fail fast ────────────────

    @Test
    void missingEligibleConceptDefinition_throwsMissingEligibleConceptDefinitionException() {
        // Eligibility resolves GHOST_CONCEPT but no definition exists in the repo
        DefaultEligibleExecutionPlanBuilder builder = builderWith(
                eligibilityResolverWith("SALARIO_BASE", "GHOST_CONCEPT")
        );

        MissingEligibleConceptDefinitionException ex = assertThrows(
                MissingEligibleConceptDefinitionException.class,
                () -> builder.build(CONTEXT, REF)
        );
        assertEquals(RS, ex.getRuleSystemCode());
        assertEquals("GHOST_CONCEPT", ex.getConceptCode());
    }

    // ── test: missing dependency concept definition → fail fast ──────────────

    @Test
    void missingDependencyConceptDefinition_throwsMissingDependencyConceptDefinitionException() {
        // SALARIO_BASE has a feed relation pointing to a source not present in the concept repo
        PayrollConcept ghost = concept(99L, "GHOST_SOURCE");
        FakeFeedRelationRepository brokenFeedRepo = new FakeFeedRelationRepository(Map.of(
                ID_SALARIO_BASE, List.of(feedRel(ghost, salarioBase))
        ));
        // ghost is deliberately NOT added to conceptRepo

        DefaultConceptDependencyGraphService gs = new DefaultConceptDependencyGraphService(brokenFeedRepo, emptyOperandRepo());
        DefaultEligibleConceptExpansionService es = new DefaultEligibleConceptExpansionService(conceptRepo, brokenFeedRepo, emptyOperandRepo());
        DefaultEligibleExecutionPlanBuilder builder = new DefaultEligibleExecutionPlanBuilder(
                eligibilityResolverWith("SALARIO_BASE"),
                conceptRepo,
                es,
                gs,
                simplePlanBuilder()
        );

        MissingDependencyConceptDefinitionException ex = assertThrows(
                MissingDependencyConceptDefinitionException.class,
                () -> builder.build(CONTEXT, REF)
        );
        assertEquals(RS, ex.getRuleSystemCode());
        assertEquals("GHOST_SOURCE", ex.getConceptCode());
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private DefaultEligibleExecutionPlanBuilder builderWith(DefaultConceptEligibilityResolver eligibilityResolver) {
        DefaultEligibleConceptExpansionService expansionService =
                new DefaultEligibleConceptExpansionService(conceptRepo, feedRepo, emptyOperandRepo());
        return new DefaultEligibleExecutionPlanBuilder(
                eligibilityResolver,
                conceptRepo,
                expansionService,
                graphService,
                simplePlanBuilder()
        );
    }

    /**
     * Simple plan builder that returns entries in topological order.
     * Does not require operand configuration — suitable for plan-assembly tests.
     */
    private ExecutionPlanBuilder simplePlanBuilder() {
        return (graph, concepts, referenceDate) -> {
            Map<ConceptNodeIdentity, PayrollConcept> idx = concepts.stream()
                    .collect(Collectors.toMap(
                            c -> new ConceptNodeIdentity(c.getRuleSystemCode(), c.getConceptCode()),
                            c -> c
                    ));
            return graph.topologicalOrder().stream()
                    .map(id -> new ConceptExecutionPlanEntry(id, idx.get(id).getCalculationType()))
                    .collect(Collectors.toList());
        };
    }

    private DefaultConceptEligibilityResolver eligibilityResolverWith(
            FakeConceptRepository repo, String... conceptCodes
    ) {
        // identical logic, just ignored (repo is unused — assignments drive eligibility)
        return eligibilityResolverWith(conceptCodes);
    }

    private DefaultConceptEligibilityResolver eligibilityResolverWith(String... conceptCodes) {
        List<ConceptAssignment> assignments = new ArrayList<>();
        for (String code : conceptCodes) {
            assignments.add(new ConceptAssignment(
                    null, RS, code, null, null, null,
                    LocalDate.of(2025, 1, 1), null, 0, NOW, NOW
            ));
        }
        ConceptAssignmentRepository repo = new ConceptAssignmentRepository() {
            @Override
            public com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment save(
                    com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment a) {
                throw new UnsupportedOperationException();
            }
            @Override
            public List<ConceptAssignment> findApplicableAssignments(
                    EmployeeAssignmentContext ctx, LocalDate date) {
                return assignments;
            }
            @Override
            public List<ConceptAssignment> findAllByRuleSystemCode(String ruleSystemCode) {
                return assignments;
            }
            @Override
            public List<ConceptAssignment> findAllByRuleSystemCodeAndConceptCode(String ruleSystemCode, String conceptCode) {
                return assignments.stream()
                        .filter(a -> conceptCode.equals(a.getConceptCode()))
                        .toList();
            }
            @Override
            public void deleteById(Long id) {
                // no-op for test fake
            }
            @Override
            public boolean existsByIdAndRuleSystemCode(Long id, String ruleSystemCode) {
                return false;
            }
            @Override
            public Optional<ConceptAssignment> findByIdAndRuleSystemCode(Long id, String ruleSystemCode) {
                return Optional.empty();
            }
        };
        return new DefaultConceptEligibilityResolver(repo);
    }

    private PayrollConcept concept(long id, String code) {
        return concept(id, code, CalculationType.DIRECT_AMOUNT);
    }

    private PayrollConcept concept(long id, String code, CalculationType type) {
        PayrollObject obj = new PayrollObject(id, RS, PayrollObjectTypeCode.CONCEPT, code, NOW, NOW);
        return new PayrollConcept(obj, code, type,
                FunctionalNature.INFORMATIONAL,
                null, ExecutionScope.SEGMENT, NOW, NOW);
    }

    private PayrollConceptFeedRelation feedRel(PayrollConcept source, PayrollConcept target) {
        return new PayrollConceptFeedRelation(
                null,
                source.getObject(),
                target.getObject(),
                FeedMode.FEED_BY_SOURCE,
                null, false,
                LocalDate.of(2025, 1, 1),
                null,
                NOW,
                NOW
        );
    }

    private Set<String> codeSetOf(List<ResolvedConceptAssignment> assignments) {
        return assignments.stream()
                .map(ResolvedConceptAssignment::getConceptCode)
                .collect(Collectors.toSet());
    }

    private Set<String> conceptCodes(List<PayrollConcept> concepts) {
        return concepts.stream()
                .map(PayrollConcept::getConceptCode)
                .collect(Collectors.toSet());
    }

    /** Asserts that {@code before} appears earlier than {@code after} in the plan. */
    private void assertBefore(List<String> planCodes, String before, String after) {
        int indexBefore = planCodes.indexOf(before);
        int indexAfter  = planCodes.indexOf(after);
        assertTrue(indexBefore >= 0, "Expected '" + before + "' in plan but not found");
        assertTrue(indexAfter  >= 0, "Expected '" + after  + "' in plan but not found");
        assertTrue(indexBefore < indexAfter,
                "Expected '" + before + "' (index " + indexBefore + ") before '"
                        + after + "' (index " + indexAfter + ") in plan: " + planCodes);
    }

    // ── in-memory fakes ──────────────────────────────────────────────────────

    private static final class FakeConceptRepository implements PayrollConceptRepository {

        private final Map<String, PayrollConcept> byCode;

        FakeConceptRepository(List<PayrollConcept> concepts) {
            this.byCode = concepts.stream()
                    .collect(Collectors.toMap(PayrollConcept::getConceptCode, c -> c));
        }

        @Override
        public Optional<PayrollConcept> findByBusinessKey(String ruleSystemCode, String conceptCode) {
            return Optional.ofNullable(byCode.get(conceptCode));
        }

        @Override
        public List<PayrollConcept> findAllByCodes(String ruleSystemCode, Collection<String> conceptCodes) {
            return conceptCodes.stream()
                    .map(byCode::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        }

        @Override
        public List<PayrollConcept> findAllByRuleSystemCode(String ruleSystemCode) {
            return new java.util.ArrayList<>(byCode.values());
        }

        @Override
        public void deleteByBusinessKey(String ruleSystemCode, String conceptCode) {
            byCode.remove(conceptCode);
        }

        @Override
        public PayrollConcept save(PayrollConcept concept) {
            throw new UnsupportedOperationException("not needed in this test");
        }

        @Override
        public boolean existsByBusinessKey(String ruleSystemCode, String conceptCode) {
            return byCode.containsKey(conceptCode);
        }
    }

    private static PayrollConceptOperandRepository emptyOperandRepo() {
        return new PayrollConceptOperandRepository() {
            @Override
            public PayrollConceptOperand save(PayrollConceptOperand o) { throw new UnsupportedOperationException(); }
            @Override
            public List<PayrollConceptOperand> findByTarget(String rs, String code) { return List.of(); }
            @Override
            public List<PayrollConceptOperand> findByRuleSystemCodeAndConceptCode(String rs, String code) {
                throw new UnsupportedOperationException();
            }
            @Override
            public void deleteAllByRuleSystemCodeAndConceptCode(String rs, String code) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static final class FakeFeedRelationRepository implements PayrollConceptFeedRelationRepository {

        private final Map<Long, List<PayrollConceptFeedRelation>> byTargetId;

        FakeFeedRelationRepository(Map<Long, List<PayrollConceptFeedRelation>> byTargetId) {
            this.byTargetId = new HashMap<>(byTargetId);
        }

        @Override
        public PayrollConceptFeedRelation save(PayrollConceptFeedRelation feedRelation) {
            throw new UnsupportedOperationException("not needed in this test");
        }

        @Override
        public List<PayrollConceptFeedRelation> findActiveByTargetObjectId(
                Long targetObjectId, LocalDate referenceDate
        ) {
            return byTargetId.getOrDefault(targetObjectId, List.of());
        }

        @Override
        public List<PayrollConceptFeedRelation> findByRuleSystemCodeAndTargetConceptCode(
                String ruleSystemCode, String conceptCode
        ) {
            throw new UnsupportedOperationException("not needed in this test");
        }

        @Override
        public void deleteAllByRuleSystemCodeAndTargetConceptCode(
                String ruleSystemCode, String conceptCode
        ) {
            throw new UnsupportedOperationException("not needed in this test");
        }
    }
}
