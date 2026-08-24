package com.b4rrhh.payroll_engine.eligibility.domain.port;

import com.b4rrhh.support.TestPostgresInitializer;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment;
import com.b4rrhh.payroll_engine.eligibility.infrastructure.persistence.ConceptAssignmentPersistenceAdapter;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests for {@link ConceptAssignmentRepository} covering the
 * {@code findAllByRuleSystemCode}, {@code findAllByRuleSystemCodeAndConceptCode}
 * and {@code deleteById} operations.
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
class ConceptAssignmentRepositoryFindAllTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private ConceptAssignmentRepository repository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        Path migrationDir = Files.createDirectories(
                tempDir.resolve("flyway-concept-assignment-find-all"));
        copyMigration(migrationDir, "V56__create_payroll_engine_schema.sql");
        copyMigration(migrationDir, "V58__add_concept_assignment.sql");
        copyMigration(migrationDir, "V80__add_persist_to_concepts_to_payroll_concept.sql");
        copyMigration(migrationDir, "V81__add_summary_to_payroll_concept.sql");
        registry.add("spring.flyway.locations", () -> "filesystem:" + migrationDir.toAbsolutePath());
    }

    private static void copyMigration(Path directory, String filename) throws IOException {
        try (InputStream is = ConceptAssignmentRepositoryFindAllTest.class
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

    @Test
    void findAllByRuleSystemCode_returnsAllAssignmentsForRuleSystem() {
        repository.save(assignment("ESP", "101", null, "AGR1", null, 10));
        repository.save(assignment("ESP", "970", null, "AGR1", null, 20));
        repository.save(assignment("FRA", "X99", null, null, null, 30));

        List<ConceptAssignment> result = repository.findAllByRuleSystemCode("ESP");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(a -> a.getRuleSystemCode().equals("ESP"));
        assertThat(result).extracting(ConceptAssignment::getConceptCode)
                .containsExactlyInAnyOrder("101", "970");
    }

    @Test
    void findAllByRuleSystemCode_returnsEmptyForUnknownSystem() {
        repository.save(assignment("ESP", "101", null, "AGR1", null, 10));

        List<ConceptAssignment> result = repository.findAllByRuleSystemCode("UNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByRuleSystemCodeAndConceptCode_returnsOnlyMatchingConcept() {
        repository.save(assignment("ESP", "101", null, "AGR1", null, 10));
        repository.save(assignment("ESP", "101", "EMP1", "AGR1", null, 50));
        repository.save(assignment("ESP", "970", null, "AGR1", null, 20));

        List<ConceptAssignment> result = repository.findAllByRuleSystemCodeAndConceptCode("ESP", "101");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(a -> a.getConceptCode().equals("101"));
        // ordered by priority DESC (50 first, then 10)
        assertThat(result.get(0).getPriority()).isEqualTo(50);
        assertThat(result.get(1).getPriority()).isEqualTo(10);
    }

    @Test
    void findAllByRuleSystemCodeAndConceptCode_returnsEmptyWhenConceptNotFound() {
        repository.save(assignment("ESP", "101", null, "AGR1", null, 10));

        List<ConceptAssignment> result =
                repository.findAllByRuleSystemCodeAndConceptCode("ESP", "NONEXISTENT");

        assertThat(result).isEmpty();
    }

    @Test
    void deleteById_removesAssignmentFromRepository() {
        ConceptAssignment saved = repository.save(assignment("ESP", "101", null, "AGR1", null, 10));
        ConceptAssignment kept = repository.save(assignment("ESP", "970", null, "AGR1", null, 20));

        assertThat(saved.getId()).isNotNull();

        repository.deleteById(saved.getId());

        List<ConceptAssignment> remaining = repository.findAllByRuleSystemCode("ESP");
        assertThat(remaining).extracting(ConceptAssignment::getId)
                .containsExactly(kept.getId());
    }

    @Test
    void deleteById_isNoOpForNonExistentId() {
        repository.save(assignment("ESP", "101", null, "AGR1", null, 10));

        assertThatNoException().isThrownBy(() -> repository.deleteById(999_999L));
        assertThatNoException().isThrownBy(() -> repository.deleteById(null));

        assertThat(repository.findAllByRuleSystemCode("ESP")).hasSize(1);
    }

    private ConceptAssignment assignment(
            String ruleSystemCode, String conceptCode,
            String companyCode, String agreementCode, String employeeTypeCode,
            int priority
    ) {
        return new ConceptAssignment(
                null, ruleSystemCode, conceptCode,
                companyCode, agreementCode, employeeTypeCode,
                JAN_1, DEC_31, priority,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
