package com.b4rrhh.payroll_engine.object.infrastructure.persistence;

import com.b4rrhh.support.TestPostgresInitializer;
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
class PayrollObjectPersistenceAdapterTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private SpringDataPayrollObjectRepository repository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        Path migrationDirectory = Files.createDirectories(tempDir.resolve("flyway-payroll-engine"));
        copyMigration(migrationDirectory, "V56__create_payroll_engine_schema.sql");
        copyMigration(migrationDirectory, "V80__add_persist_to_concepts_to_payroll_concept.sql");
        copyMigration(migrationDirectory, "V81__add_summary_to_payroll_concept.sql");
        registry.add("spring.flyway.locations", () -> "filesystem:" + migrationDirectory.toAbsolutePath());
    }

    private static void copyMigration(Path directory, String filename) throws IOException {
        try (InputStream is = PayrollObjectPersistenceAdapterTest.class
                .getClassLoader()
                .getResourceAsStream("db/migration/" + filename)) {
            if (is == null) {
                throw new IllegalStateException("Migration not found in classpath: " + filename);
            }
            Files.copy(is, directory.resolve(filename));
        }
    }

    @Test
    void persistsPayrollObjectAndLoadsItByBusinessKey() {
        PayrollObjectEntity entity = buildEntity("ESP", "CONCEPT", "SALBASE");
        repository.saveAndFlush(entity);

        Optional<PayrollObjectEntity> found = repository
                .findByRuleSystemCodeAndObjectTypeCodeAndObjectCode("ESP", "CONCEPT", "SALBASE");

        assertTrue(found.isPresent());
        assertEquals("ESP", found.get().getRuleSystemCode());
        assertEquals("CONCEPT", found.get().getObjectTypeCode());
        assertEquals("SALBASE", found.get().getObjectCode());
    }

    @Test
    void enforcesUniqueBusinessKeyConstraint() {
        repository.saveAndFlush(buildEntity("ESP", "CONCEPT", "SALBASE"));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(buildEntity("ESP", "CONCEPT", "SALBASE")));
    }

    @Test
    void allowsDifferentObjectCodesUnderSameRuleSystem() {
        repository.saveAndFlush(buildEntity("ESP", "CONCEPT", "SALBASE"));
        repository.saveAndFlush(buildEntity("ESP", "CONCEPT", "IRPF"));

        assertTrue(repository.findByRuleSystemCodeAndObjectTypeCodeAndObjectCode("ESP", "CONCEPT", "SALBASE").isPresent());
        assertTrue(repository.findByRuleSystemCodeAndObjectTypeCodeAndObjectCode("ESP", "CONCEPT", "IRPF").isPresent());
    }

    @Test
    void allowsSameObjectCodeUnderDifferentObjectTypes() {
        repository.saveAndFlush(buildEntity("ESP", "CONCEPT", "BASE"));
        repository.saveAndFlush(buildEntity("ESP", "TABLE", "BASE"));

        assertTrue(repository.findByRuleSystemCodeAndObjectTypeCodeAndObjectCode("ESP", "CONCEPT", "BASE").isPresent());
        assertTrue(repository.findByRuleSystemCodeAndObjectTypeCodeAndObjectCode("ESP", "TABLE", "BASE").isPresent());
    }

    private PayrollObjectEntity buildEntity(String ruleSystemCode, String objectTypeCode, String objectCode) {
        PayrollObjectEntity entity = new PayrollObjectEntity();
        entity.setRuleSystemCode(ruleSystemCode);
        entity.setObjectTypeCode(objectTypeCode);
        entity.setObjectCode(objectCode);
        return entity;
    }
}
