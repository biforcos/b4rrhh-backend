package com.b4rrhh.payroll_engine.dependency.domain.model;

import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.FeedMode;
import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;
import com.b4rrhh.payroll_engine.dependency.domain.exception.ConceptDependencyCycleException;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptDependencyGraphTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private static PayrollObject object(long id, String code) {
        return new PayrollObject(id, "ESP", PayrollObjectTypeCode.CONCEPT, code, null, null);
    }

    private static PayrollConcept concept(long id, String code) {
        return new PayrollConcept(
                object(id, code),
                code + "_MNE",
                CalculationType.DIRECT_AMOUNT,
                FunctionalNature.EARNING,
                null,
                ExecutionScope.SEGMENT,
                null, null
        );
    }

    private static ConceptNodeIdentity id(String code) {
        return new ConceptNodeIdentity("ESP", code);
    }

    private static PayrollConceptFeedRelation feedRelation(PayrollConcept source, PayrollConcept target) {
        return new PayrollConceptFeedRelation(
                null,
                source.getObject(),
                target.getObject(),
                FeedMode.FEED_BY_SOURCE,
                null,
                false,
                LocalDate.of(2025, 1, 1),
                null,
                null, null
        );
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void buildsSimpleValidGraph() {
        PayrollConcept salbase = concept(1L, "SALBASE");
        PayrollConcept irpf = concept(2L, "IRPF");

        ConceptDependencyGraph graph = new ConceptDependencyGraphBuilder()
                .addNodes(List.of(salbase, irpf))
                .addOperandDependency(irpf, salbase)
                .build();

        assertTrue(graph.getNodes().contains(id("SALBASE")));
        assertTrue(graph.getNodes().contains(id("IRPF")));
        assertTrue(graph.getDependenciesOf(id("IRPF")).contains(id("SALBASE")));
        assertEquals(1, graph.getEdges().size());
        assertEquals(DependencyType.OPERAND_DEPENDENCY, graph.getEdges().get(0).getType());
    }

    @Test
    void detectsDirectCycle() {
        PayrollConcept a = concept(1L, "A");
        PayrollConcept b = concept(2L, "B");

        ConceptDependencyGraphBuilder builder = new ConceptDependencyGraphBuilder()
                .addOperandDependency(a, b)  // A depends on B
                .addOperandDependency(b, a); // B depends on A — cycle

        assertThrows(ConceptDependencyCycleException.class, builder::build);
    }

    @Test
    void detectsIndirectCycle() {
        PayrollConcept a = concept(1L, "A");
        PayrollConcept b = concept(2L, "B");
        PayrollConcept c = concept(3L, "C");

        ConceptDependencyGraphBuilder builder = new ConceptDependencyGraphBuilder()
                .addOperandDependency(a, b)  // A → B
                .addOperandDependency(b, c)  // B → C
                .addOperandDependency(c, a); // C → A — indirect cycle A→B→C→A

        assertThrows(ConceptDependencyCycleException.class, builder::build);
    }

    @Test
    void buildsGraphWithFeedDependency() {
        PayrollConcept salbase = concept(1L, "SALBASE");
        PayrollConcept aggregate = concept(2L, "TOTAL_BRUTO");

        PayrollConceptFeedRelation feed = feedRelation(salbase, aggregate);

        ConceptDependencyGraph graph = new ConceptDependencyGraphBuilder()
                .addFeedRelation(feed)
                .build();

        // aggregate depends on salbase (salbase feeds into aggregate)
        assertTrue(graph.getDependenciesOf(id("TOTAL_BRUTO")).contains(id("SALBASE")));
        assertEquals(DependencyType.FEED_DEPENDENCY, graph.getEdges().get(0).getType());
    }

    @Test
    void returnsValidTopologicalOrder() {
        PayrollConcept salbase = concept(1L, "SALBASE");
        PayrollConcept irpf = concept(2L, "IRPF");
        PayrollConcept total = concept(3L, "TOTAL");

        // total depends on salbase and irpf; irpf depends on salbase
        ConceptDependencyGraph graph = new ConceptDependencyGraphBuilder()
                .addOperandDependency(irpf, salbase)   // irpf → salbase
                .addOperandDependency(total, salbase)  // total → salbase
                .addOperandDependency(total, irpf)     // total → irpf
                .build();

        List<ConceptNodeIdentity> order = graph.topologicalOrder();

        int salbaseIdx = order.indexOf(id("SALBASE"));
        int irpfIdx    = order.indexOf(id("IRPF"));
        int totalIdx   = order.indexOf(id("TOTAL"));

        // salbase must appear before irpf and total; irpf must appear before total
        assertTrue(salbaseIdx < irpfIdx,  "SALBASE must precede IRPF");
        assertTrue(salbaseIdx < totalIdx, "SALBASE must precede TOTAL");
        assertTrue(irpfIdx    < totalIdx, "IRPF must precede TOTAL");
    }

    @Test
    void graphWithIsolatedNodesIsValidAndContainsAllNodes() {
        PayrollConcept a = concept(1L, "ALPHA");
        PayrollConcept b = concept(2L, "BETA");
        PayrollConcept c = concept(3L, "GAMMA");

        ConceptDependencyGraph graph = new ConceptDependencyGraphBuilder()
                .addNodes(List.of(a, b, c))
                .build();

        List<ConceptNodeIdentity> order = graph.topologicalOrder();

        assertEquals(3, order.size());
        assertTrue(order.contains(id("ALPHA")));
        assertTrue(order.contains(id("BETA")));
        assertTrue(order.contains(id("GAMMA")));
        assertTrue(graph.getEdges().isEmpty());
    }

    @Test
    void feedRelationWithDifferentRuleSystemsIsRejected() {
        PayrollObject sourceObj = new PayrollObject(1L, "ESP", PayrollObjectTypeCode.CONCEPT, "SALBASE", null, null);
        PayrollObject targetObj = new PayrollObject(2L, "FRA", PayrollObjectTypeCode.CONCEPT, "TOTAL", null, null);

        PayrollConceptFeedRelation crossSystemFeed = new PayrollConceptFeedRelation(
                null,
                sourceObj,
                targetObj,
                FeedMode.FEED_BY_SOURCE,
                null,
                false,
                LocalDate.of(2025, 1, 1),
                null,
                null, null
        );

        assertThrows(IllegalArgumentException.class,
                () -> new ConceptDependencyGraphBuilder().addFeedRelation(crossSystemFeed));
    }

    @Test
    void selfDependencyIsRejectedAtEdgeConstruction() {
        PayrollConcept a = concept(1L, "A");
        ConceptNodeIdentity nodeA = id("A");

        assertThrows(IllegalArgumentException.class,
                () -> new ConceptDependency(nodeA, nodeA, DependencyType.OPERAND_DEPENDENCY));
    }

    @Test
    void cycleExceptionMessageContainsCycleNodes() {
        PayrollConcept a = concept(1L, "ALPHA");
        PayrollConcept b = concept(2L, "BETA");

        ConceptDependencyGraphBuilder builder = new ConceptDependencyGraphBuilder()
                .addOperandDependency(a, b)
                .addOperandDependency(b, a);

        ConceptDependencyCycleException ex = assertThrows(
                ConceptDependencyCycleException.class, builder::build
        );
        assertTrue(ex.getMessage().contains("ALPHA") || ex.getMessage().contains("BETA"));
    }
}
