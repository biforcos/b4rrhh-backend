package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.support.TestPostgresInitializer;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
class PayrollLaunchPersistenceFlywayIntegrationTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        Path migrationDirectory = Files.createDirectories(tempDir.resolve("flyway-payroll-launch"));
        copyMigration(migrationDirectory, "V53__create_payroll_tables.sql");
        copyMigration(migrationDirectory, "V54__add_payroll_child_unique_constraints.sql");
        copyMigration(migrationDirectory, "V55__create_payroll_launch_persistence_model.sql");

        registry.add("spring.flyway.locations", () -> "filesystem:" + migrationDirectory.toAbsolutePath());
    }

    @Test
    void createsLaunchPersistenceTables() {
        assertEquals(1, tableCount("payroll", "calculation_run"));
        assertEquals(1, tableCount("payroll", "calculation_claim"));
        assertEquals(1, tableCount("payroll", "payroll_warning"));
        assertEquals(1, tableCount("payroll", "calculation_run_message"));
    }

    @Test
    void enforcesUniqueCalculationClaimBusinessKey() {
        Long firstRunId = insertCalculationRun("REQUESTED");
        Long secondRunId = insertCalculationRun("REQUESTED");

        insertCalculationClaim(firstRunId, "INTERNAL", "EMP001", "202501", "NORMAL", 1);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertCalculationClaim(secondRunId, "INTERNAL", "EMP001", "202501", "NORMAL", 1)
        );
    }

    @Test
    void cascadesPayrollWarningsWhenPayrollIsDeleted() {
        Long payrollId = insertPayroll();
        jdbcTemplate.update(
                "insert into payroll.payroll_warning (payroll_id, warning_code, severity_code, message, details_json) values (?, ?, ?, ?, cast(? as json))",
                payrollId,
                "MISSING_DATA",
                "ERROR",
                "Missing source data",
                "{\"field\":\"contract\"}"
        );

        jdbcTemplate.update("delete from payroll.payroll where id = ?", payrollId);

        assertEquals(0, countRows("payroll.payroll_warning"));
    }

    @Test
    void cascadesRunMessagesWhenRunIsDeleted() {
        Long runId = insertCalculationRun("REQUESTED");
        jdbcTemplate.update(
                "insert into payroll.calculation_run_message (run_id, message_code, severity_code, message, details_json) values (?, ?, ?, ?, cast(? as json))",
                runId,
                "RUN_INFO",
                "INFO",
                "Run queued",
                "{\"phase\":\"queue\"}"
        );

        jdbcTemplate.update("delete from payroll.calculation_run where id = ?", runId);

        assertEquals(0, countRows("payroll.calculation_run_message"));
    }

    @Test
    void cascadesClaimsWhenRunIsDeleted() {
        Long runId = insertCalculationRun("RUNNING");
        insertCalculationClaim(runId, "INTERNAL", "EMP001", "202501", "NORMAL", 1);

        jdbcTemplate.update("delete from payroll.calculation_run where id = ?", runId);

        assertEquals(0, countRows("payroll.calculation_claim"));
    }

    private Integer tableCount(String schemaName, String tableName) {
        // Sin toUpperCase: H2 pliega los identificadores sin comillas a
        // MAYUSCULAS y Postgres a minusculas. Preguntando en mayusculas este
        // test encontraba las tablas en H2 y no las encontraba en el motor de
        // produccion, donde se llaman en minuscula.
        return jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = ? and table_name = ?",
                Integer.class,
                schemaName,
                tableName
        );
    }

    private Integer countRows(String qualifiedTableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + qualifiedTableName, Integer.class);
    }

    private Long insertCalculationRun(String status) {
        jdbcTemplate.update(
                "insert into payroll.calculation_run (rule_system_code, payroll_period_code, payroll_type_code, calculation_engine_code, calculation_engine_version, requested_at, requested_by, status, target_selection_json, total_candidates, total_eligible, total_claimed, total_skipped_not_eligible, total_skipped_already_claimed, total_calculated, total_not_valid, total_errors, summary_json) values (?, ?, ?, ?, ?, ?, ?, ?, cast(? as json), ?, ?, ?, ?, ?, ?, ?, ?, cast(? as json))",
                "ESP",
                "202501",
                "NORMAL",
                "ENGINE",
                "1.0",
                Timestamp.valueOf(LocalDateTime.of(2026, 1, 31, 10, 15)),
                "bifor",
                status,
                "{\"population\":\"ALL\"}",
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                null
        );

        return jdbcTemplate.queryForObject("select max(id) from payroll.calculation_run", Long.class);
    }

    private void insertCalculationClaim(
            Long runId,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode,
            Integer presenceNumber
    ) {
        jdbcTemplate.update(
                "insert into payroll.calculation_claim (run_id, rule_system_code, employee_type_code, employee_number, payroll_period_code, payroll_type_code, presence_number, claimed_at, claimed_by) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                runId,
                "ESP",
                employeeTypeCode,
                employeeNumber,
                payrollPeriodCode,
                payrollTypeCode,
                presenceNumber,
                Timestamp.valueOf(LocalDateTime.of(2026, 1, 31, 10, 30)),
                "bifor"
        );
    }

    private Long insertPayroll() {
        jdbcTemplate.update(
                "insert into payroll.payroll (rule_system_code, employee_type_code, employee_number, payroll_period_code, payroll_type_code, presence_number, status, status_reason_code, calculated_at, calculation_engine_code, calculation_engine_version, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                1,
                "CALCULATED",
                null,
                Timestamp.valueOf(LocalDateTime.of(2026, 1, 31, 10, 15)),
                "ENGINE",
                "1.0",
                Timestamp.valueOf(LocalDateTime.of(2026, 1, 31, 10, 15)),
                Timestamp.valueOf(LocalDateTime.of(2026, 1, 31, 10, 15))
        );

        jdbcTemplate.update(
                "insert into payroll.payroll_concept (payroll_id, line_number, concept_code, concept_label, amount, quantity, rate, concept_nature_code, origin_period_code, display_order) values ((select max(id) from payroll.payroll), ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                1,
                "BASE",
                "Base salary",
                new BigDecimal("1000.00"),
                null,
                null,
                "EARNING",
                "202501",
                1
        );

        jdbcTemplate.update(
                "insert into payroll.payroll_context_snapshot (payroll_id, snapshot_type_code, source_vertical_code, source_business_key_json, snapshot_payload_json) values ((select max(id) from payroll.payroll), ?, ?, cast(? as json), cast(? as json))",
                "PRESENCE",
                "EMPLOYEE",
                "{\"presenceNumber\":1}",
                "{\"companyCode\":\"ES01\"}"
        );

        return jdbcTemplate.queryForObject("select max(id) from payroll.payroll", Long.class);
    }

    private static void copyMigration(Path migrationDirectory, String fileName) throws IOException {
        Path target = migrationDirectory.resolve(fileName);
        try (InputStream inputStream = PayrollLaunchPersistenceFlywayIntegrationTest.class
                .getClassLoader()
                .getResourceAsStream("db/migration/" + fileName)) {
            if (inputStream == null) {
                throw new IllegalStateException("Migration not found on classpath: " + fileName);
            }
            Files.copy(inputStream, target);
        }
    }
}