package com.b4rrhh.payroll_engine.eligibility.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ConceptAssignment} domain invariants.
 */
class ConceptAssignmentTest {

    private static final LocalDate JAN_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate DEC_31 = LocalDate.of(2025, 12, 31);

    @Test
    void constructsSuccessfully_withAllFields() {
        ConceptAssignment a = assignment("ESP", "SALARIO_BASE", "EMP1", "METAL", "INDEFINIDO",
                JAN_1, DEC_31, 20);
        assertEquals("ESP", a.getRuleSystemCode());
        assertEquals("SALARIO_BASE", a.getConceptCode());
        assertEquals("EMP1", a.getCompanyCode());
        assertEquals("METAL", a.getAgreementCode());
        assertEquals("INDEFINIDO", a.getEmployeeTypeCode());
        assertEquals(JAN_1, a.getValidFrom());
        assertEquals(DEC_31, a.getValidTo());
        assertEquals(20, a.getPriority());
    }

    @Test
    void constructsSuccessfully_withNullOptionalDimensions() {
        ConceptAssignment a = assignment("ESP", "SALARIO_BASE", null, null, null, JAN_1, null, 0);
        assertNull(a.getCompanyCode());
        assertNull(a.getAgreementCode());
        assertNull(a.getEmployeeTypeCode());
        assertNull(a.getValidTo());
    }

    @Test
    void normalizesBlankOptionalDimensionsToNull() {
        ConceptAssignment a = assignment("ESP", "SALARIO_BASE", "  ", "", "  ", JAN_1, null, 0);
        assertNull(a.getCompanyCode());
        assertNull(a.getAgreementCode());
        assertNull(a.getEmployeeTypeCode());
    }

    @Test
    void rejectsNullRuleSystemCode() {
        assertThrows(IllegalArgumentException.class,
                () -> assignment(null, "SALARIO_BASE", null, null, null, JAN_1, null, 0));
    }

    @Test
    void rejectsBlankRuleSystemCode() {
        assertThrows(IllegalArgumentException.class,
                () -> assignment("  ", "SALARIO_BASE", null, null, null, JAN_1, null, 0));
    }

    @Test
    void rejectsNullConceptCode() {
        assertThrows(IllegalArgumentException.class,
                () -> assignment("ESP", null, null, null, null, JAN_1, null, 0));
    }

    @Test
    void rejectsNullValidFrom() {
        assertThrows(IllegalArgumentException.class,
                () -> assignment("ESP", "SALARIO_BASE", null, null, null, null, null, 0));
    }

    @Test
    void rejectsValidToBeforeValidFrom() {
        assertThrows(IllegalArgumentException.class,
                () -> assignment("ESP", "SALARIO_BASE", null, null, null,
                        LocalDate.of(2025, 6, 1), LocalDate.of(2025, 5, 31), 0));
    }

    @Test
    void acceptsValidToEqualValidFrom() {
        ConceptAssignment a = assignment("ESP", "SALARIO_BASE", null, null, null,
                JAN_1, JAN_1, 0);
        assertNotNull(a);
        assertEquals(JAN_1, a.getValidTo());
    }

    @Test
    void isValidOn_trueWhenInsideRange() {
        ConceptAssignment a = assignment("ESP", "CONCEPT", null, null, null, JAN_1, DEC_31, 0);
        assertTrue(a.isValidOn(LocalDate.of(2025, 6, 15)));
    }

    @Test
    void isValidOn_trueOnValidFrom() {
        ConceptAssignment a = assignment("ESP", "CONCEPT", null, null, null, JAN_1, DEC_31, 0);
        assertTrue(a.isValidOn(JAN_1));
    }

    @Test
    void isValidOn_trueOnValidTo() {
        ConceptAssignment a = assignment("ESP", "CONCEPT", null, null, null, JAN_1, DEC_31, 0);
        assertTrue(a.isValidOn(DEC_31));
    }

    @Test
    void isValidOn_falseBeforeValidFrom() {
        ConceptAssignment a = assignment("ESP", "CONCEPT", null, null, null, JAN_1, DEC_31, 0);
        assertFalse(a.isValidOn(LocalDate.of(2024, 12, 31)));
    }

    @Test
    void isValidOn_falseAfterValidTo() {
        ConceptAssignment a = assignment("ESP", "CONCEPT", null, null, null, JAN_1, DEC_31, 0);
        assertFalse(a.isValidOn(LocalDate.of(2026, 1, 1)));
    }

    @Test
    void isValidOn_trueForOpenEndedAssignment() {
        ConceptAssignment a = assignment("ESP", "CONCEPT", null, null, null, JAN_1, null, 0);
        assertTrue(a.isValidOn(LocalDate.of(2030, 1, 1)));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static ConceptAssignment assignment(
            String ruleSystem, String concept,
            String company, String agreement, String employeeType,
            LocalDate validFrom, LocalDate validTo, int priority
    ) {
        return new ConceptAssignment(null, ruleSystem, concept, company, agreement, employeeType,
                validFrom, validTo, priority, LocalDateTime.now(), Instant.now());
    }
}
