package com.b4rrhh.payroll_engine.eligibility.application.service;

import com.b4rrhh.payroll_engine.eligibility.domain.exception.DuplicateConceptAssignmentException;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment;
import com.b4rrhh.payroll_engine.eligibility.domain.model.EmployeeAssignmentContext;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ResolvedConceptAssignment;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodelFixtures.metamodel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link DefaultConceptEligibilityResolver}.
 *
 * <p>The assignments are handed over in a metamodel built in memory, so no Spring context
 * and no database are needed.
 */
class DefaultConceptEligibilityResolverTest {

    private static final String RS = "ESP";
    private static final LocalDate REF = LocalDate.of(2025, 3, 1);

    private static final EmployeeAssignmentContext FULL_CONTEXT =
            new EmployeeAssignmentContext(RS, "EMP1", "METAL", "INDEFINIDO");

    // El resolvedor ya no tiene estado: resuelve contra el metamodelo que se le pasa.
    private static final DefaultConceptEligibilityResolver RESOLVER = new DefaultConceptEligibilityResolver();

    // ── test: global assignment applies ────────────────────────────────────

    @Test
    void globalAssignment_appliesWhenContextMatches() {
        ConceptAssignment global = assignment(RS, "SALARIO_BASE", null, null, null, 0);
        RuleSystemMetamodel metamodel = metamodelWith(List.of(global));

        List<ResolvedConceptAssignment> result = RESOLVER.resolve(FULL_CONTEXT, metamodel);

        assertEquals(1, result.size());
        assertEquals("SALARIO_BASE", result.get(0).getConceptCode());
        assertEquals(0, result.get(0).getWinningPriority());
        assertNull(result.get(0).getCompanyCode());
        assertNull(result.get(0).getAgreementCode());
        assertNull(result.get(0).getEmployeeTypeCode());
    }

    // ── test: more specific higher-priority assignment wins ────────────────

    @Test
    void higherPriorityAssignmentWins() {
        ConceptAssignment global = assignment(RS, "SALARIO_BASE", null, null, null, 0);
        ConceptAssignment specific = assignment(RS, "SALARIO_BASE", "EMP1", "METAL", "INDEFINIDO", 30);

        // ambas asignaciones casan con el contexto: el comodín lo resuelve el metamodelo
        RuleSystemMetamodel metamodel = metamodelWith(List.of(global, specific));

        List<ResolvedConceptAssignment> result = RESOLVER.resolve(FULL_CONTEXT, metamodel);

        assertEquals(1, result.size());
        ResolvedConceptAssignment winner = result.get(0);
        assertEquals("SALARIO_BASE", winner.getConceptCode());
        assertEquals(30, winner.getWinningPriority());
        assertEquals("EMP1", winner.getCompanyCode());
        assertEquals("METAL", winner.getAgreementCode());
        assertEquals("INDEFINIDO", winner.getEmployeeTypeCode());
    }

    // ── test: multiple concepts resolved correctly ─────────────────────────

    @Test
    void multipleConceptsResolved() {
        ConceptAssignment salario = assignment(RS, "SALARIO_BASE", null, null, null, 0);
        ConceptAssignment transporte = assignment(RS, "PLUS_TRANSPORTE", "EMP1", "METAL", null, 20);

        RuleSystemMetamodel metamodel = metamodelWith(List.of(salario, transporte));

        List<ResolvedConceptAssignment> result = RESOLVER.resolve(FULL_CONTEXT, metamodel);

        assertEquals(2, result.size());
        // sorted: priority desc (20 first), then conceptCode asc
        assertEquals("PLUS_TRANSPORTE", result.get(0).getConceptCode());
        assertEquals(20, result.get(0).getWinningPriority());
        assertEquals("SALARIO_BASE", result.get(1).getConceptCode());
        assertEquals(0, result.get(1).getWinningPriority());
    }

    // ── test: deterministic ordering by conceptCode when same priority ─────

    @Test
    void samePriorityDifferentConcepts_sortedByConceptCodeAscending() {
        ConceptAssignment a = assignment(RS, "CONCEPT_Z", null, null, null, 10);
        ConceptAssignment b = assignment(RS, "CONCEPT_A", null, null, null, 10);
        ConceptAssignment c = assignment(RS, "CONCEPT_M", null, null, null, 10);

        RuleSystemMetamodel metamodel = metamodelWith(List.of(a, b, c));

        List<ResolvedConceptAssignment> result = RESOLVER.resolve(FULL_CONTEXT, metamodel);

        assertEquals(3, result.size());
        assertEquals("CONCEPT_A", result.get(0).getConceptCode());
        assertEquals("CONCEPT_M", result.get(1).getConceptCode());
        assertEquals("CONCEPT_Z", result.get(2).getConceptCode());
    }

    // ── test: empty context returns empty list ─────────────────────────────

    @Test
    void noAssignmentsFound_returnsEmptyList() {
        RuleSystemMetamodel metamodel = metamodelWith(List.of());

        List<ResolvedConceptAssignment> result = RESOLVER.resolve(FULL_CONTEXT, metamodel);

        assertEquals(0, result.size());
    }

    // ── test: duplicate winning priority for same concept throws ──────────

    @Test
    void duplicatePriorityForSameConcept_throwsDuplicateConceptAssignmentException() {
        ConceptAssignment a = assignment(RS, "SALARIO_BASE", "EMP1", null, null, 20);
        ConceptAssignment b = assignment(RS, "SALARIO_BASE", null, "METAL", null, 20);

        RuleSystemMetamodel metamodel = metamodelWith(List.of(a, b));

        assertThrows(DuplicateConceptAssignmentException.class,
                () -> RESOLVER.resolve(FULL_CONTEXT, metamodel));
    }

    // ── test: lower priority loses even when more specific ─────────────────

    @Test
    void lowerPriorityAssignmentLosesRegardlessOfSpecificity() {
        ConceptAssignment lowSpecific = assignment(RS, "SALARIO_BASE", "EMP1", "METAL", "INDEFINIDO", 5);
        ConceptAssignment highGlobal = assignment(RS, "SALARIO_BASE", null, null, null, 15);

        RuleSystemMetamodel metamodel = metamodelWith(List.of(lowSpecific, highGlobal));

        List<ResolvedConceptAssignment> result = RESOLVER.resolve(FULL_CONTEXT, metamodel);

        assertEquals(1, result.size());
        assertEquals(15, result.get(0).getWinningPriority());
        assertNull(result.get(0).getCompanyCode());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static ConceptAssignment assignment(
            String rs, String concept, String company, String agreement, String employeeType, int priority
    ) {
        return new ConceptAssignment(
                null, rs, concept, company, agreement, employeeType,
                LocalDate.of(2025, 1, 1), null, priority,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    /** El metamodelo de la ejecución con las asignaciones del caso, ya vigentes. */
    private static RuleSystemMetamodel metamodelWith(List<ConceptAssignment> candidates) {
        return metamodel(RS, REF).withAssignments(candidates).build();
    }
}
