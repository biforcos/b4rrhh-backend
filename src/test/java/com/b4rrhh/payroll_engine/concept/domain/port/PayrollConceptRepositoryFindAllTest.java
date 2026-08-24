package com.b4rrhh.payroll_engine.concept.domain.port;

import com.b4rrhh.support.TestPostgresInitializer;
import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.ResultCompositionMode;
import com.b4rrhh.payroll_engine.concept.infrastructure.persistence.PayrollConceptPersistenceAdapter;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.PayrollObjectEntity;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.SpringDataPayrollObjectRepository;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests for {@link PayrollConceptRepository} covering the
 * {@code findAllByRuleSystemCode} and {@code deleteByBusinessKey} operations.
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
@Import(PayrollConceptPersistenceAdapter.class)
class PayrollConceptRepositoryFindAllTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private PayrollConceptRepository repository;

    @Autowired
    private SpringDataPayrollObjectRepository objectRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        Path migrationDirectory = Files.createDirectories(
                tempDir.resolve("flyway-payroll-concept-find-all"));
        copyMigration(migrationDirectory, "V56__create_payroll_engine_schema.sql");
        copyMigration(migrationDirectory, "V80__add_persist_to_concepts_to_payroll_concept.sql");
        copyMigration(migrationDirectory, "V81__add_summary_to_payroll_concept.sql");
        registry.add("spring.flyway.locations", () -> "filesystem:" + migrationDirectory.toAbsolutePath());
    }

    private static void copyMigration(Path directory, String filename) throws IOException {
        try (InputStream is = PayrollConceptRepositoryFindAllTest.class
                .getClassLoader()
                .getResourceAsStream("db/migration/" + filename)) {
            if (is == null) {
                throw new IllegalStateException("Migration not found in classpath: " + filename);
            }
            Files.copy(is, directory.resolve(filename));
        }
    }

    @Test
    void findAllByRuleSystemCode_returnsAllConceptsForRuleSystem() {
        seedConcept("ESP", "101", "SALARIO_BASE");
        seedConcept("ESP", "D01", "DIAS_MES");
        seedConcept("OTHER", "X99", "OTHER_CONCEPT");

        List<PayrollConcept> concepts = repository.findAllByRuleSystemCode("ESP");

        assertThat(concepts).isNotEmpty();
        assertThat(concepts).allMatch(c -> c.getRuleSystemCode().equals("ESP"));
        assertThat(concepts).extracting(PayrollConcept::getConceptCode)
                .containsExactly("101", "D01");
    }

    @Test
    void findAllByRuleSystemCode_returnsEmptyForUnknownSystem() {
        seedConcept("ESP", "101", "SALARIO_BASE");

        List<PayrollConcept> concepts = repository.findAllByRuleSystemCode("UNKNOWN");

        assertThat(concepts).isEmpty();
    }

    @Test
    void deleteByBusinessKey_removesConceptFromRepository() {
        seedConcept("ESP", "101", "SALARIO_BASE");
        seedConcept("ESP", "D01", "DIAS_MES");

        List<PayrollConcept> before = repository.findAllByRuleSystemCode("ESP");
        assertThat(before).extracting(PayrollConcept::getConceptCode)
                .contains("101");

        repository.deleteByBusinessKey("ESP", "101");

        assertThat(repository.findAllByRuleSystemCode("ESP"))
                .noneMatch(c -> c.getConceptCode().equals("101"));
    }

    @Test
    void deleteByBusinessKey_noOpForNonExistentConcept() {
        seedConcept("ESP", "101", "SALARIO_BASE");

        assertThatNoException().isThrownBy(
                () -> repository.deleteByBusinessKey("ESP", "NONEXISTENT")
        );

        // Existing concept must still be present
        assertThat(repository.findAllByRuleSystemCode("ESP"))
                .extracting(PayrollConcept::getConceptCode)
                .contains("101");
    }

    private void seedConcept(String ruleSystemCode, String conceptCode, String mnemonic) {
        PayrollObjectEntity objectEntity = new PayrollObjectEntity();
        objectEntity.setRuleSystemCode(ruleSystemCode);
        objectEntity.setObjectTypeCode(PayrollObjectTypeCode.CONCEPT.name());
        objectEntity.setObjectCode(conceptCode);
        PayrollObjectEntity saved = objectRepository.saveAndFlush(objectEntity);

        PayrollObject domainObject = new PayrollObject(
                saved.getId(),
                saved.getRuleSystemCode(),
                PayrollObjectTypeCode.valueOf(saved.getObjectTypeCode()),
                saved.getObjectCode(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
        PayrollConcept concept = new PayrollConcept(
                domainObject,
                mnemonic,
                CalculationType.DIRECT_AMOUNT,
                FunctionalNature.EARNING,
                ResultCompositionMode.REPLACE,
                null,
                ExecutionScope.PERIOD,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        repository.save(concept);
    }
}
