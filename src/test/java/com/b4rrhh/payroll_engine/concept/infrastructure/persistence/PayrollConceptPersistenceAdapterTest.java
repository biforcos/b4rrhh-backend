package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import com.b4rrhh.support.TestPostgresInitializer;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.PayrollObjectEntity;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.SpringDataPayrollObjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
class PayrollConceptPersistenceAdapterTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private SpringDataPayrollObjectRepository objectRepository;

    @Autowired
    private SpringDataPayrollConceptRepository conceptRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        Path migrationDirectory = Files.createDirectories(tempDir.resolve("flyway-payroll-engine-concept"));
        copyMigration(migrationDirectory, "V56__create_payroll_engine_schema.sql");
        copyMigration(migrationDirectory, "V80__add_persist_to_concepts_to_payroll_concept.sql");
        copyMigration(migrationDirectory, "V81__add_summary_to_payroll_concept.sql");
        registry.add("spring.flyway.locations", () -> "filesystem:" + migrationDirectory.toAbsolutePath());
    }

    private static void copyMigration(Path directory, String filename) throws IOException {
        try (InputStream is = PayrollConceptPersistenceAdapterTest.class
                .getClassLoader()
                .getResourceAsStream("db/migration/" + filename)) {
            if (is == null) {
                throw new IllegalStateException("Migration not found in classpath: " + filename);
            }
            Files.copy(is, directory.resolve(filename));
        }
    }

    @Test
    void persistsConceptWithSemanticPropertiesAndLoadsIt() {
        PayrollObjectEntity objectEntity = saveObject("ESP", "CONCEPT", "SALBASE");

        PayrollConceptEntity conceptEntity = buildConceptEntity(
                objectEntity, "SAL_BASE", "DIRECT_AMOUNT", "EARNING", "REPLACE", null, "SEGMENT"
        );
        conceptRepository.saveAndFlush(conceptEntity);

        Optional<PayrollConceptEntity> found = conceptRepository
                .findByRuleSystemCodeAndConceptCode("ESP", "SALBASE");

        assertTrue(found.isPresent());
        assertEquals("SAL_BASE", found.get().getConceptMnemonic());
        assertEquals("DIRECT_AMOUNT", found.get().getCalculationType());
        assertEquals("EARNING", found.get().getFunctionalNature());
        assertEquals("REPLACE", found.get().getResultCompositionMode());
        assertEquals("SEGMENT", found.get().getExecutionScope());
    }

    @Test
    void persistsConceptWithPeriodExecutionScope() {
        PayrollObjectEntity objectEntity = saveObject("ESP", "CONCEPT", "IRPF");

        PayrollConceptEntity conceptEntity = buildConceptEntity(
                objectEntity, "IRPF_RET", "PERCENTAGE", "DEDUCTION", "REPLACE", "200", "PERIOD"
        );
        conceptRepository.saveAndFlush(conceptEntity);

        Optional<PayrollConceptEntity> found = conceptRepository
                .findByRuleSystemCodeAndConceptCode("ESP", "IRPF");

        assertTrue(found.isPresent());
        assertEquals("PERIOD", found.get().getExecutionScope());
        assertEquals("PERCENTAGE", found.get().getCalculationType());
        assertEquals("200", found.get().getPayslipOrderCode());
    }

    @Test
    void conceptWithoutBaseObjectFails() {
        PayrollConceptEntity orphanConcept = new PayrollConceptEntity();
        // objectId not set, no associated PayrollObjectEntity
        orphanConcept.setConceptMnemonic("ORPHAN");
        orphanConcept.setCalculationType("DIRECT_AMOUNT");
        orphanConcept.setFunctionalNature("EARNING");
        orphanConcept.setResultCompositionMode("REPLACE");
        orphanConcept.setExecutionScope("SEGMENT");

        assertThrows(Exception.class, () -> conceptRepository.saveAndFlush(orphanConcept));
    }

    private PayrollObjectEntity saveObject(String ruleSystemCode, String objectTypeCode, String objectCode) {
        PayrollObjectEntity entity = new PayrollObjectEntity();
        entity.setRuleSystemCode(ruleSystemCode);
        entity.setObjectTypeCode(objectTypeCode);
        entity.setObjectCode(objectCode);
        return objectRepository.saveAndFlush(entity);
    }

    private PayrollConceptEntity buildConceptEntity(
            PayrollObjectEntity objectEntity,
            String mnemonic,
            String calculationType,
            String functionalNature,
            String resultCompositionMode,
            String payslipOrderCode,
            String executionScope
    ) {
        PayrollConceptEntity entity = new PayrollConceptEntity();
        entity.setPayrollObject(objectEntity);
        entity.setConceptMnemonic(mnemonic);
        entity.setCalculationType(calculationType);
        entity.setFunctionalNature(functionalNature);
        entity.setResultCompositionMode(resultCompositionMode);
        entity.setPayslipOrderCode(payslipOrderCode);
        entity.setExecutionScope(executionScope);
        return entity;
    }
}
