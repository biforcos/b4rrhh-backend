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
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
class PayrollConceptFeedRelationPersistenceTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private SpringDataPayrollObjectRepository objectRepository;

    @Autowired
    private SpringDataPayrollConceptFeedRelationRepository feedRelationRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        Path migrationDirectory = Files.createDirectories(tempDir.resolve("flyway-payroll-engine-feed"));
        copyMigration(migrationDirectory, "V56__create_payroll_engine_schema.sql");
        copyMigration(migrationDirectory, "V80__add_persist_to_concepts_to_payroll_concept.sql");
        copyMigration(migrationDirectory, "V81__add_summary_to_payroll_concept.sql");
        registry.add("spring.flyway.locations", () -> "filesystem:" + migrationDirectory.toAbsolutePath());
    }

    private static void copyMigration(Path directory, String filename) throws IOException {
        try (InputStream is = PayrollConceptFeedRelationPersistenceTest.class
                .getClassLoader()
                .getResourceAsStream("db/migration/" + filename)) {
            if (is == null) {
                throw new IllegalStateException("Migration not found in classpath: " + filename);
            }
            Files.copy(is, directory.resolve(filename));
        }
    }

    @Test
    void persistsFeedRelationWithNullableEffectiveTo() {
        PayrollObjectEntity source = saveObject("ESP", "CONCEPT", "SALBASE");
        PayrollObjectEntity target = saveObject("ESP", "CONCEPT", "IRPF");

        PayrollConceptFeedRelationEntity relation = buildRelation(
                source, target, "FEED_BY_SOURCE", null,
                LocalDate.of(2025, 1, 1), null
        );
        feedRelationRepository.saveAndFlush(relation);

        List<PayrollConceptFeedRelationEntity> results = feedRelationRepository
                .findActiveByTargetObjectId(target.getId(), LocalDate.of(2025, 6, 1));

        assertEquals(1, results.size());
        assertEquals("FEED_BY_SOURCE", results.get(0).getFeedMode());
        assertNull(results.get(0).getEffectiveTo());
    }

    @Test
    void persistsFeedRelationWithFeedValue() {
        PayrollObjectEntity source = saveObject("ESP", "CONCEPT", "SALBASE2");
        PayrollObjectEntity target = saveObject("ESP", "CONCEPT", "IRPF2");

        PayrollConceptFeedRelationEntity relation = buildRelation(
                source, target, "FEED_BY_SOURCE", new BigDecimal("0.35"),
                LocalDate.of(2025, 1, 1), null
        );
        feedRelationRepository.saveAndFlush(relation);

        List<PayrollConceptFeedRelationEntity> results = feedRelationRepository
                .findActiveByTargetObjectId(target.getId(), LocalDate.of(2025, 3, 1));

        assertEquals(1, results.size());
        assertEquals(0, new BigDecimal("0.35").compareTo(results.get(0).getFeedValue()));
    }

    @Test
    void doesNotReturnExpiredRelations() {
        PayrollObjectEntity source = saveObject("ESP", "CONCEPT", "SALBASE3");
        PayrollObjectEntity target = saveObject("ESP", "CONCEPT", "IRPF3");

        PayrollConceptFeedRelationEntity expired = buildRelation(
                source, target, "FEED_BY_SOURCE", null,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)
        );
        feedRelationRepository.saveAndFlush(expired);

        List<PayrollConceptFeedRelationEntity> results = feedRelationRepository
                .findActiveByTargetObjectId(target.getId(), LocalDate.of(2025, 6, 1));

        assertTrue(results.isEmpty());
    }

    @Test
    void feedRelationWithMissingSourceFails() {
        PayrollObjectEntity target = saveObject("ESP", "CONCEPT", "TARGET_ONLY");

        PayrollConceptFeedRelationEntity relation = new PayrollConceptFeedRelationEntity();
        relation.setSourceObject(null);
        relation.setTargetObject(target);
        relation.setFeedMode("FEED_BY_SOURCE");
        relation.setEffectiveFrom(LocalDate.of(2025, 1, 1));

        assertThrows(Exception.class, () -> feedRelationRepository.saveAndFlush(relation));
    }

    private PayrollObjectEntity saveObject(String ruleSystemCode, String objectTypeCode, String objectCode) {
        PayrollObjectEntity entity = new PayrollObjectEntity();
        entity.setRuleSystemCode(ruleSystemCode);
        entity.setObjectTypeCode(objectTypeCode);
        entity.setObjectCode(objectCode);
        return objectRepository.saveAndFlush(entity);
    }

    private PayrollConceptFeedRelationEntity buildRelation(
            PayrollObjectEntity source,
            PayrollObjectEntity target,
            String feedMode,
            BigDecimal feedValue,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        PayrollConceptFeedRelationEntity entity = new PayrollConceptFeedRelationEntity();
        entity.setSourceObject(source);
        entity.setTargetObject(target);
        entity.setFeedMode(feedMode);
        entity.setFeedValue(feedValue);
        entity.setEffectiveFrom(effectiveFrom);
        entity.setEffectiveTo(effectiveTo);
        return entity;
    }
}
