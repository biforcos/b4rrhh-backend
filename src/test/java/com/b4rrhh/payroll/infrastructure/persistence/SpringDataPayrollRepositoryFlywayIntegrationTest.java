package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.support.TestPostgresInitializer;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
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
class SpringDataPayrollRepositoryFlywayIntegrationTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private SpringDataPayrollRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        Path migrationDirectory = Files.createDirectories(tempDir.resolve("flyway-payroll"));
        copyMigration(migrationDirectory, "V53__create_payroll_tables.sql");
        copyMigration(migrationDirectory, "V54__add_payroll_child_unique_constraints.sql");
        copyMigration(migrationDirectory, "V55__create_payroll_launch_persistence_model.sql");
        copyMigration(migrationDirectory, "V76__create_payroll_segment_table.sql");

        registry.add("spring.flyway.locations", () -> "filesystem:" + migrationDirectory.toAbsolutePath());
    }

    @Test
    void appliesFlywayMigrationAndLoadsAggregateByBusinessKey() {
        repository.saveAndFlush(payrollEntity());

        Optional<PayrollEntity> result = repository
                .findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumberAndPayrollPeriodCodeAndPayrollTypeCodeAndPresenceNumber(
                        "ESP",
                        "INTERNAL",
                        "EMP001",
                        "202501",
                        "NORMAL",
                        1
                );

        assertTrue(result.isPresent());
        assertEquals(PayrollStatus.CALCULATED, result.get().getStatus());
        assertEquals(1, result.get().getConcepts().size());
        assertEquals(1, result.get().getContextSnapshots().size());
    }

    @Test
    void enforcesUniqueBusinessKeyConstraint() {
        repository.saveAndFlush(payrollEntity());

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(payrollEntity()));
    }

        @Test
        void enforcesUniqueConceptLineNumberPerPayroll() {
        PayrollEntity saved = repository.saveAndFlush(payrollEntity());

        assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "insert into payroll.payroll_concept (payroll_id, line_number, concept_code, concept_label, amount, concept_nature_code, display_order) values (?, ?, ?, ?, ?, ?, ?)",
                saved.getId(),
                1,
                "BONUS",
                "Duplicate line",
                new BigDecimal("10.00"),
                "EARNING",
                2
            )
        );
        }

        @Test
        void enforcesUniqueSnapshotTypePerPayroll() {
        PayrollEntity saved = repository.saveAndFlush(payrollEntity());

        assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "insert into payroll.payroll_context_snapshot (payroll_id, snapshot_type_code, source_vertical_code, source_business_key_json, snapshot_payload_json) values (?, ?, ?, cast(? as json), cast(? as json))",
                saved.getId(),
                "PRESENCE",
                "EMPLOYEE",
                "{\"presenceNumber\":1}",
                "{\"companyCode\":\"ES01\"}"
            )
        );
        }

    private PayrollEntity payrollEntity() {
        PayrollEntity payroll = new PayrollEntity();
        payroll.setRuleSystemCode("ESP");
        payroll.setEmployeeTypeCode("INTERNAL");
        payroll.setEmployeeNumber("EMP001");
        payroll.setPayrollPeriodCode("202501");
        payroll.setPayrollTypeCode("NORMAL");
        payroll.setPresenceNumber(1);
        payroll.setStatus(PayrollStatus.CALCULATED);
        payroll.setCalculatedAt(LocalDateTime.of(2026, 1, 31, 10, 15));
        payroll.setCalculationEngineCode("ENGINE");
        payroll.setCalculationEngineVersion("1.0");

        PayrollConceptEntity concept = new PayrollConceptEntity();
        concept.setLineNumber(1);
        concept.setConceptCode("BASE");
        concept.setConceptLabel("Base salary");
        concept.setAmount(new BigDecimal("1000.00"));
        concept.setConceptNatureCode("EARNING");
        concept.setOriginPeriodCode("202501");
        concept.setDisplayOrder(1);

        PayrollContextSnapshotEntity snapshot = new PayrollContextSnapshotEntity();
        snapshot.setSnapshotTypeCode("PRESENCE");
        snapshot.setSourceVerticalCode("EMPLOYEE");
        snapshot.setSourceBusinessKeyJson("{\"presenceNumber\":1}");
        snapshot.setSnapshotPayloadJson("{\"companyCode\":\"ES01\"}");

        payroll.replaceConcepts(List.of(concept));
        payroll.replaceContextSnapshots(List.of(snapshot));
        return payroll;
    }

    private static void copyMigration(Path migrationDirectory, String fileName) throws IOException {
        Path target = migrationDirectory.resolve(fileName);
        try (InputStream inputStream = SpringDataPayrollRepositoryFlywayIntegrationTest.class
                .getClassLoader()
                .getResourceAsStream("db/migration/" + fileName)) {
            if (inputStream == null) {
                throw new IllegalStateException("Migration not found on classpath: " + fileName);
            }
            Files.copy(inputStream, target);
        }
    }
}