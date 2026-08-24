package com.b4rrhh.payroll_engine.eligibility.infrastructure.persistence;

import com.b4rrhh.support.TestPostgresInitializer;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment;
import com.b4rrhh.payroll_engine.eligibility.domain.model.EmployeeAssignmentContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for {@link ConceptAssignmentPersistenceAdapter}.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Valid-on-date filtering: assignments outside range are excluded</li>
 *   <li>Wildcard matching: null dimensions in assignment match any context value</li>
 *   <li>Non-matching dimensions are excluded</li>
 *   <li>Save round-trip preserves all fields</li>
 * </ul>
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
// Postgres de verdad en vez del H2 que Spring pone por defecto. Este test
// aplica un subconjunto de las migraciones reales sobre una base virgen que le
// prepara el initializer, asi que hasta ahora estaba ejecutando SQL de
// produccion contra un motor que no es el de produccion.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = TestPostgresInitializer.class)
@Import(ConceptAssignmentPersistenceAdapter.class)
class ConceptAssignmentPersistenceAdapterTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private ConceptAssignmentPersistenceAdapter adapter;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        Path migrationDir = Files.createDirectories(tempDir.resolve("flyway-concept-assignment"));
        copyMigration(migrationDir, "V56__create_payroll_engine_schema.sql");
        copyMigration(migrationDir, "V58__add_concept_assignment.sql");
        copyMigration(migrationDir, "V80__add_persist_to_concepts_to_payroll_concept.sql");
        copyMigration(migrationDir, "V81__add_summary_to_payroll_concept.sql");
        registry.add("spring.flyway.locations", () -> "filesystem:" + migrationDir.toAbsolutePath());
    }

    private static void copyMigration(Path directory, String filename) throws IOException {
        try (InputStream is = ConceptAssignmentPersistenceAdapterTest.class
                .getClassLoader()
                .getResourceAsStream("db/migration/" + filename)) {
            if (is == null) {
                throw new IllegalStateException("Migration not found: " + filename);
            }
            Files.copy(is, directory.resolve(filename));
        }
    }

    private static final LocalDate JAN_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate DEC_31 = LocalDate.of(2025, 12, 31);
    private static final LocalDate REF = LocalDate.of(2025, 6, 1);

    // ── save round-trip ───────────────────────────────────────────────────

    @Test
    void saveAndRetrieve_preservesAllFields() {
        ConceptAssignment saved = adapter.save(fullAssignment("SALARIO_BASE", "EMP1", "METAL", "INDEFINIDO", 20));

        assertNotNull(saved.getId());
        assertEquals("ESP", saved.getRuleSystemCode());
        assertEquals("SALARIO_BASE", saved.getConceptCode());
        assertEquals("EMP1", saved.getCompanyCode());
        assertEquals("METAL", saved.getAgreementCode());
        assertEquals("INDEFINIDO", saved.getEmployeeTypeCode());
        assertEquals(JAN_1, saved.getValidFrom());
        assertEquals(DEC_31, saved.getValidTo());
        assertEquals(20, saved.getPriority());
    }

    // ── validity date filtering ───────────────────────────────────────────

    @Test
    void findsAssignment_validOnReferenceDate() {
        adapter.save(fullAssignment("SALARIO_BASE", null, null, null, 0));
        EmployeeAssignmentContext ctx = new EmployeeAssignmentContext("ESP", null, null, null);

        List<ConceptAssignment> result = adapter.findApplicableAssignments(ctx, REF);

        assertEquals(1, result.size());
        assertEquals("SALARIO_BASE", result.get(0).getConceptCode());
    }

    @Test
    void excludesAssignment_expiredBeforeReferenceDate() {
        // valid_to = 2024-12-31, reference date = 2025-06-01 → excluded
        ConceptAssignment expired = new ConceptAssignment(null, "ESP", "SALARIO_BASE",
                null, null, null,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), 0,
                LocalDateTime.now(), LocalDateTime.now());
        adapter.save(expired);

        EmployeeAssignmentContext ctx = new EmployeeAssignmentContext("ESP", null, null, null);
        List<ConceptAssignment> result = adapter.findApplicableAssignments(ctx, REF);

        assertEquals(0, result.size());
    }

    @Test
    void excludesAssignment_notYetValidOnReferenceDate() {
        // valid_from = 2025-07-01, reference date = 2025-06-01 → excluded
        ConceptAssignment future = new ConceptAssignment(null, "ESP", "SALARIO_BASE",
                null, null, null,
                LocalDate.of(2025, 7, 1), null, 0,
                LocalDateTime.now(), LocalDateTime.now());
        adapter.save(future);

        EmployeeAssignmentContext ctx = new EmployeeAssignmentContext("ESP", null, null, null);
        List<ConceptAssignment> result = adapter.findApplicableAssignments(ctx, REF);

        assertEquals(0, result.size());
    }

    @Test
    void includesOpenEndedAssignment() {
        // valid_to = null → open-ended
        ConceptAssignment openEnded = new ConceptAssignment(null, "ESP", "SALARIO_BASE",
                null, null, null,
                JAN_1, null, 0,
                LocalDateTime.now(), LocalDateTime.now());
        adapter.save(openEnded);

        EmployeeAssignmentContext ctx = new EmployeeAssignmentContext("ESP", null, null, null);
        List<ConceptAssignment> result = adapter.findApplicableAssignments(ctx, LocalDate.of(2030, 1, 1));

        assertEquals(1, result.size());
    }

    // ── wildcard matching ─────────────────────────────────────────────────

    @Test
    void wildcardAssignment_matchesContextWithValues() {
        // assignment has all null dimensions → matches any context
        adapter.save(wildcardAssignment("SALARIO_BASE", 0));
        EmployeeAssignmentContext ctx = new EmployeeAssignmentContext("ESP", "EMP1", "METAL", "INDEFINIDO");

        List<ConceptAssignment> result = adapter.findApplicableAssignments(ctx, REF);

        assertEquals(1, result.size());
    }

    @Test
    void wildcardAssignment_matchesContextWithNullValues() {
        adapter.save(wildcardAssignment("SALARIO_BASE", 0));
        EmployeeAssignmentContext ctx = new EmployeeAssignmentContext("ESP", null, null, null);

        List<ConceptAssignment> result = adapter.findApplicableAssignments(ctx, REF);

        assertEquals(1, result.size());
    }

    @Test
    void specificAssignment_excludedWhenCompanyDoesNotMatch() {
        // assignment has companyCode=EMP1, context has companyCode=EMP2 → excluded
        adapter.save(fullAssignment("SALARIO_BASE", "EMP1", null, null, 10));
        EmployeeAssignmentContext ctx = new EmployeeAssignmentContext("ESP", "EMP2", null, null);

        List<ConceptAssignment> result = adapter.findApplicableAssignments(ctx, REF);

        assertEquals(0, result.size());
    }

    @Test
    void specificAssignment_excludedWhenAgreementDoesNotMatch() {
        adapter.save(fullAssignment("SALARIO_BASE", null, "METAL", null, 10));
        EmployeeAssignmentContext ctx = new EmployeeAssignmentContext("ESP", null, "CONSTRUCTION", null);

        List<ConceptAssignment> result = adapter.findApplicableAssignments(ctx, REF);

        assertEquals(0, result.size());
    }

    @Test
    void specificAssignment_excludedWhenEmployeeTypeDoesNotMatch() {
        adapter.save(fullAssignment("SALARIO_BASE", null, null, "INDEFINIDO", 10));
        EmployeeAssignmentContext ctx = new EmployeeAssignmentContext("ESP", null, null, "TEMPORAL");

        List<ConceptAssignment> result = adapter.findApplicableAssignments(ctx, REF);

        assertEquals(0, result.size());
    }

    @Test
    void returnsBothWildcardAndSpecificWhenContextMatches() {
        adapter.save(wildcardAssignment("SALARIO_BASE", 0));
        adapter.save(fullAssignment("SALARIO_BASE", "EMP1", "METAL", "INDEFINIDO", 30));
        EmployeeAssignmentContext ctx = new EmployeeAssignmentContext("ESP", "EMP1", "METAL", "INDEFINIDO");

        List<ConceptAssignment> result = adapter.findApplicableAssignments(ctx, REF);

        // Both match: wildcard (all-null) + specific (exact match) — precedence resolved in service
        assertEquals(2, result.size());
    }

    // ── null-context semantics ────────────────────────────────────────────────
    // NULL in context = unknown/unspecified.
    // Only wildcard assignments (assignment dimension = null) may match.
    // Assignments with a specific non-null dimension must be excluded.

    @Test
    void specificCompanyAssignment_excludedWhenContextCompanyIsNull() {
        // assignment.companyCode = 'EMP1' (specific), context.companyCode = null (unknown)
        // expected: excluded — the caller does not know the company, so we cannot confirm
        // this assignment's specific company requirement is satisfied
        adapter.save(fullAssignment("SALARIO_BASE", "EMP1", null, null, 10));
        EmployeeAssignmentContext ctx = new EmployeeAssignmentContext("ESP", null, null, null);

        List<ConceptAssignment> result = adapter.findApplicableAssignments(ctx, REF);

        assertEquals(0, result.size());
    }

    @Test
    void specificAgreementAssignment_excludedWhenContextAgreementIsNull() {
        // assignment.agreementCode = 'METAL' (specific), context.agreementCode = null (unknown)
        // expected: excluded — same rationale as companyCode above
        adapter.save(fullAssignment("SALARIO_BASE", null, "METAL", null, 10));
        EmployeeAssignmentContext ctx = new EmployeeAssignmentContext("ESP", null, null, null);

        List<ConceptAssignment> result = adapter.findApplicableAssignments(ctx, REF);

        assertEquals(0, result.size());
    }

    @Test
    void wildcardAssignment_stillMatchesWhenContextDimensionsAreNull() {
        // assignment has all-null dimensions (full wildcard), context dimensions are all null
        // expected: included — wildcard always matches, regardless of context nulls
        adapter.save(wildcardAssignment("SALARIO_BASE", 0));
        EmployeeAssignmentContext ctx = new EmployeeAssignmentContext("ESP", null, null, null);

        List<ConceptAssignment> result = adapter.findApplicableAssignments(ctx, REF);

        assertEquals(1, result.size());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ConceptAssignment fullAssignment(
            String concept, String company, String agreement, String employeeType, int priority
    ) {
        return new ConceptAssignment(null, "ESP", concept, company, agreement, employeeType,
                JAN_1, DEC_31, priority, LocalDateTime.now(), LocalDateTime.now());
    }

    private ConceptAssignment wildcardAssignment(String concept, int priority) {
        return new ConceptAssignment(null, "ESP", concept, null, null, null,
                JAN_1, DEC_31, priority, LocalDateTime.now(), LocalDateTime.now());
    }
}
