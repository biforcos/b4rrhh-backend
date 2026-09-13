package com.b4rrhh.payroll_engine.metamodel.domain.model;

import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.FeedMode;
import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;
import com.b4rrhh.payroll_engine.concept.domain.model.OperandRole;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment;
import com.b4rrhh.payroll_engine.eligibility.domain.model.EmployeeAssignmentContext;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodelFixtures.metamodel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La reglamentación cargada contesta lo mismo que contestaban los repositorios, pero sin
 * ir a la base (backend#87). Lo que se prueba aquí son esas respuestas, y sobre todo la
 * semántica de comodín de las asignaciones, que antes vivía en el SQL.
 */
class RuleSystemMetamodelTest {

    private static final String RS = "ESP";
    private static final LocalDate REF = LocalDate.of(2025, 3, 31);

    // ── conceptos, operandos y alimentaciones ────────────────────────────────

    @Test
    void findsConceptsByCodeAndIgnoresTheOnesItDoesNotHave() {
        RuleSystemMetamodel metamodel = metamodel(RS, REF)
                .withConcepts(concept(1L, "SALARIO_BASE"), concept(2L, "T_PRECIO_DIA"))
                .build();

        assertTrue(metamodel.findConcept("SALARIO_BASE").isPresent());
        assertTrue(metamodel.findConcept("NO_EXISTE").isEmpty());
        assertEquals(2, metamodel.conceptCount());
    }

    @Test
    void returnsTheRequestedConceptsInTheOrderTheyWereAskedFor() {
        RuleSystemMetamodel metamodel = metamodel(RS, REF)
                .withConcepts(concept(1L, "SALARIO_BASE"), concept(2L, "T_PRECIO_DIA"))
                .build();

        List<String> codes = metamodel.findConcepts(List.of("T_PRECIO_DIA", "FANTASMA", "SALARIO_BASE"))
                .stream().map(PayrollConcept::getConceptCode).toList();

        assertEquals(List.of("T_PRECIO_DIA", "SALARIO_BASE"), codes);
    }

    @Test
    void groupsOperandsByTheirTargetConcept() {
        RuleSystemMetamodel metamodel = metamodel(RS, REF)
                .withOperands(
                        operand(1L, "SALARIO_BASE", OperandRole.QUANTITY, 2L, "T_DIAS"),
                        operand(1L, "SALARIO_BASE", OperandRole.RATE, 3L, "T_PRECIO_DIA"))
                .build();

        assertEquals(2, metamodel.operandsOf("SALARIO_BASE").size());
        assertEquals(List.of(), metamodel.operandsOf("T_DIAS"));
        assertEquals(2, metamodel.operandCount());
    }

    @Test
    void groupsFeedsByTheirTargetObjectId() {
        PayrollConcept salario = concept(1L, "SALARIO_BASE");
        PayrollConcept dias = concept(2L, "T_DIAS");
        RuleSystemMetamodel metamodel = metamodel(RS, REF)
                .withFeeds(feed(dias, salario))
                .build();

        assertEquals(1, metamodel.activeFeedsOf(1L).size());
        assertEquals(List.of(), metamodel.activeFeedsOf(2L));
        assertEquals(List.of(), metamodel.activeFeedsOf(null));
    }

    // ── asignaciones: la semántica de comodín, antes en el SQL ───────────────

    @Test
    void anAssignmentWithNullDimensionsMatchesAnyContext() {
        RuleSystemMetamodel metamodel = metamodel(RS, REF)
                .withAssignments(assignment("SALARIO_BASE", null, null, null))
                .build();

        assertEquals(1, metamodel.applicableAssignments(
                new EmployeeAssignmentContext(RS, "EMP1", "METAL", "INDEFINIDO")).size());
        assertEquals(1, metamodel.applicableAssignments(
                new EmployeeAssignmentContext(RS, null, null, null)).size());
    }

    @Test
    void anAssignmentWithAValueOnlyMatchesThatValue() {
        RuleSystemMetamodel metamodel = metamodel(RS, REF)
                .withAssignments(assignment("SALARIO_BASE", "EMP1", null, null))
                .build();

        assertEquals(1, metamodel.applicableAssignments(
                new EmployeeAssignmentContext(RS, "EMP1", "METAL", "INDEFINIDO")).size());
        assertEquals(0, metamodel.applicableAssignments(
                new EmployeeAssignmentContext(RS, "EMP2", "METAL", "INDEFINIDO")).size());
    }

    /**
     * Una dimensión nula en el contexto significa «desconocida», no «cualquiera»: solo casa
     * con el comodín. Es la misma regla que tenía el SQL y perderla cambiaría qué conceptos
     * son elegibles.
     */
    @Test
    void anUnknownContextDimensionOnlyMatchesTheWildcard() {
        RuleSystemMetamodel metamodel = metamodel(RS, REF)
                .withAssignments(assignment("SALARIO_BASE", "EMP1", null, null))
                .build();

        assertEquals(0, metamodel.applicableAssignments(
                new EmployeeAssignmentContext(RS, null, "METAL", "INDEFINIDO")).size());
    }

    // ── el borde: la reglamentación es de un sistema de reglas ───────────────

    @Test
    void refusesToAnswerForAnotherRuleSystem() {
        RuleSystemMetamodel metamodel = metamodel(RS, REF).build();

        assertThrows(IllegalArgumentException.class, () -> metamodel.requireSameRuleSystem("FRA"));
        assertThrows(IllegalArgumentException.class, () -> metamodel.applicableAssignments(
                new EmployeeAssignmentContext("FRA", "EMP1", "METAL", "INDEFINIDO")));
    }

    @Test
    void carriesTheDateItsValiditiesWereResolvedOn() {
        assertEquals(REF, metamodel(RS, REF).build().referenceDate());
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static PayrollConcept concept(Long objectId, String code) {
        return new PayrollConcept(
                object(objectId, code), code,
                CalculationType.DIRECT_AMOUNT, FunctionalNature.INFORMATIONAL,
                null, ExecutionScope.SEGMENT, null, null);
    }

    private static PayrollObject object(Long id, String code) {
        return new PayrollObject(id, RS, PayrollObjectTypeCode.CONCEPT, code, null, null);
    }

    private static PayrollConceptOperand operand(
            Long targetId, String targetCode, OperandRole role, Long sourceId, String sourceCode) {
        return new PayrollConceptOperand(
                null, object(targetId, targetCode), role, object(sourceId, sourceCode), null, null);
    }

    private static PayrollConceptFeedRelation feed(PayrollConcept source, PayrollConcept target) {
        return new PayrollConceptFeedRelation(
                null, source.getObject(), target.getObject(),
                FeedMode.FEED_BY_SOURCE, null, false,
                LocalDate.of(2025, 1, 1), null, null, null);
    }

    private static ConceptAssignment assignment(
            String conceptCode, String companyCode, String agreementCode, String employeeTypeCode) {
        return new ConceptAssignment(
                null, RS, conceptCode, companyCode, agreementCode, employeeTypeCode,
                LocalDate.of(2025, 1, 1), null, 0, null, null);
    }
}
