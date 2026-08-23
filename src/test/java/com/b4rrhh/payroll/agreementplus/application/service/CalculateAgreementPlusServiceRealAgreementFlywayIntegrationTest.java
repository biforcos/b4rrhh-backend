package com.b4rrhh.payroll.agreementplus.application.service;

import com.b4rrhh.support.TestPostgres;

import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeBusinessKeyLookupSupport;
import com.b4rrhh.employee.working_time.infrastructure.persistence.EmployeeAgreementContextLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.EmployeeAgreementCategoryLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.EmployeeByBusinessKeyLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.PayrollObjectActivationLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.PayrollObjectBindingLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.PayrollTableRowLookupAdapter;
import com.b4rrhh.payroll.domain.model.PayrollConceptNotApplicableException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        CalculateAgreementPlusService.class,
        PayrollObjectBindingLookupAdapter.class,
        PayrollTableRowLookupAdapter.class,
        PayrollObjectActivationLookupAdapter.class,
        EmployeeAgreementCategoryLookupAdapter.class,
        EmployeeByBusinessKeyLookupAdapter.class,
        EmployeeAgreementContextLookupAdapter.class,
        EmployeeBusinessKeyLookupSupport.class
})
class CalculateAgreementPlusServiceRealAgreementFlywayIntegrationTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String REAL_AGREEMENT_CODE = "99002405011982";
    private static final String POSTGRES_HOST = TestPostgres.host();
    private static final int POSTGRES_PORT = TestPostgres.port();
    private static final String POSTGRES_ADMIN_DATABASE = "postgres";
    private static final String POSTGRES_USERNAME = "b4rrhh";
    private static final String POSTGRES_PASSWORD = "b4rrhh";
    private static final String TEST_DATABASE = "agreementplus" + UUID.randomUUID().toString().replace("-", "");
    private static int empCounter = 0;

    @TempDir
    static Path tempDir;

    @Autowired
    private CalculateAgreementPlusService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        Path migrationDirectory = Files.createDirectories(tempDir.resolve("flyway-agreement-plus"));
        copyMigration(migrationDirectory, "V1__initial_personnel_model.sql");
        copyMigration(migrationDirectory, "V8__add_employee_type_code_to_employee_business_key.sql");
        copyMigration(migrationDirectory, "V22__create_rulesystem_agreement_category_relation_table.sql");
        copyMigration(migrationDirectory, "V23__create_employee_labor_classification_table.sql");
        copyMigration(migrationDirectory, "V24__seed_rule_entity_type_for_employee_labor_classification.sql");
        copyMigration(migrationDirectory, "V42__create_employee_working_time_table.sql");
        copyMigration(migrationDirectory, "V49__seed_esp_baseline_rule_system.sql");
        copyMigration(migrationDirectory, "V59__create_agreement_profile_table.sql");
        copyMigration(migrationDirectory, "V61__seed_esp_real_agreement_boe_a_2023_13740.sql");
        copyMigration(migrationDirectory, "V62__create_payroll_object_activation_table.sql");
        copyMigration(migrationDirectory, "V63__create_payroll_object_binding_table.sql");
        copyMigration(migrationDirectory, "V64__create_payroll_table_row_table.sql");
        copyMigration(migrationDirectory, "V68__seed_payroll_object_activation_for_plus_convenio_99002405011982.sql");
        copyMigration(migrationDirectory, "V69__seed_payroll_object_binding_for_plus_convenio_99002405011982.sql");
        copyMigration(migrationDirectory, "V70__seed_payroll_table_row_for_plus_convenio_pc_99002405011982.sql");
        copyMigration(migrationDirectory, "V83__add_employee_photo_url.sql");

        recreateDatabase();

        registry.add("spring.datasource.url", CalculateAgreementPlusServiceRealAgreementFlywayIntegrationTest::testDatabaseJdbcUrl);
        registry.add("spring.datasource.username", () -> POSTGRES_USERNAME);
        registry.add("spring.datasource.password", () -> POSTGRES_PASSWORD);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.locations", () -> "filesystem:" + migrationDirectory.toAbsolutePath());
    }

    @AfterAll
    static void cleanUpDatabase() throws SQLException {
        dropDatabase();
    }

    @Test
    void calculateAgreementPlusResolvesMonthlyValueForGrupoII() throws PayrollConceptNotApplicableException {
        LocalDate effectiveDate = LocalDate.of(2026, 1, 15);
        String employeeNumber = "EMP" + (empCounter++);

        long employeeId = insertEmployee(employeeNumber);
        insertPresence(employeeId, effectiveDate);
        insertLaborClassification(employeeId, "99002405-G2", effectiveDate);

        BigDecimal result = service.calculateAgreementPlus(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                employeeNumber,
                effectiveDate
        );

        assertNotNull(result);
        assertEquals(0, new BigDecimal("180.00").compareTo(result),
                "Grupo II agreement plus should be 180.00");
    }

    @Test
    void calculateAgreementPlusResolvesCorrectAmountForAllThreeGroups() throws PayrollConceptNotApplicableException {
        LocalDate effectiveDate = LocalDate.of(2026, 1, 15);

        // Grupo I
        {
            String employeeNumber = "EMP" + (empCounter++);
            long employeeId = insertEmployee(employeeNumber);
            insertPresence(employeeId, effectiveDate);
            insertLaborClassification(employeeId, "99002405-G1", effectiveDate);

            BigDecimal result = service.calculateAgreementPlus(
                    RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, effectiveDate);
            assertEquals(0, new BigDecimal("250.00").compareTo(result),
                    "Grupo I agreement plus should be 250.00");
        }

        // Grupo III
        {
            String employeeNumber = "EMP" + (empCounter++);
            long employeeId = insertEmployee(employeeNumber);
            insertPresence(employeeId, effectiveDate);
            insertLaborClassification(employeeId, "99002405-G3", effectiveDate);

            BigDecimal result = service.calculateAgreementPlus(
                    RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, effectiveDate);
            assertEquals(0, new BigDecimal("120.00").compareTo(result),
                    "Grupo III agreement plus should be 120.00");
        }
    }

    @Test
    void calculateAgreementPlusFailsWhenNoLaborClassificationExists() {
        LocalDate effectiveDate = LocalDate.of(2026, 1, 15);
        String employeeNumber = "EMP" + (empCounter++);

        insertEmployee(employeeNumber);
        // No labor classification inserted

        assertThrows(
                IllegalStateException.class,
                () -> service.calculateAgreementPlus(
                        RULE_SYSTEM_CODE,
                        EMPLOYEE_TYPE_CODE,
                        employeeNumber,
                        effectiveDate
                ),
                "Should fail when no labor classification exists"
        );
    }

    @Test
    void calculateAgreementPlusFailsWhenPlusConvenioActivationIsInactive() {
        LocalDate effectiveDate = LocalDate.of(2026, 1, 15);
        String employeeNumber = "EMP" + (empCounter++);

        long employeeId = insertEmployee(employeeNumber);
        insertPresence(employeeId, effectiveDate);
        insertLaborClassification(employeeId, "99002405-G2", effectiveDate);
        deactivatePlusConvenioActivation();

        assertThrows(
                PayrollConceptNotApplicableException.class,
                () -> service.calculateAgreementPlus(
                        RULE_SYSTEM_CODE,
                        EMPLOYEE_TYPE_CODE,
                        employeeNumber,
                        effectiveDate
                ),
                "Should fail when PLUS_CONVENIO activation is inactive"
        );
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private long insertEmployee(String employeeNumber) {
        jdbcTemplate.update(
                """
                insert into employee.employee (
                    rule_system_code,
                    employee_type_code,
                    employee_number,
                    first_name,
                    last_name_1,
                    status,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                employeeNumber,
                "Test",
                "AgreementPlus",
                "ACTIVE"
        );

        return jdbcTemplate.queryForObject(
                "select id from employee.employee where rule_system_code = ? and employee_type_code = ? and employee_number = ?",
                Long.class,
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                employeeNumber
        );
    }

    private void insertPresence(long employeeId, LocalDate effectiveDate) {
        jdbcTemplate.update(
                """
                insert into employee.presence (
                    employee_id,
                    presence_number,
                    company_code,
                    entry_reason_code,
                    start_date,
                    end_date,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                employeeId,
                1,
                "ES01",
                "HIRE",
                effectiveDate,
                null
        );
    }

    private void insertLaborClassification(long employeeId, String categoryCode, LocalDate startDate) {
        jdbcTemplate.update(
                """
                insert into employee.labor_classification (
                    employee_id,
                    agreement_code,
                    agreement_category_code,
                    start_date,
                    end_date,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                employeeId,
                REAL_AGREEMENT_CODE,
                categoryCode,
                startDate,
                null
        );
    }

    private void deactivatePlusConvenioActivation() {
        jdbcTemplate.update(
                """
                update payroll.payroll_object_activation
                   set active = false,
                       updated_at = current_timestamp
                 where rule_system_code = ?
                   and owner_type_code = 'AGREEMENT'
                   and owner_code = ?
                   and target_object_type_code = 'PAYROLL_CONCEPT'
                   and target_object_code = 'PLUS_CONVENIO'
                """,
                RULE_SYSTEM_CODE,
                REAL_AGREEMENT_CODE
        );
    }

    private static void copyMigration(Path migrationDirectory, String fileName) throws IOException {
        Path target = migrationDirectory.resolve(fileName);
        try (InputStream inputStream = CalculateAgreementPlusServiceRealAgreementFlywayIntegrationTest.class
                .getClassLoader()
                .getResourceAsStream("db/migration/" + fileName)) {
            if (inputStream == null) {
                throw new IllegalStateException("Migration not found on classpath: " + fileName);
            }
            Files.copy(inputStream, target);
        }
    }

    private static void recreateDatabase() {
        try (Connection connection = adminConnection()) {
            connection.setAutoCommit(true);
            try (var statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS " + TEST_DATABASE + " WITH (FORCE)");
                statement.execute("CREATE DATABASE " + TEST_DATABASE);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to prepare PostgreSQL database for Flyway integration test", exception);
        }
    }

    private static void dropDatabase() throws SQLException {
        try (Connection connection = adminConnection()) {
            connection.setAutoCommit(true);
            try (var statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS " + TEST_DATABASE + " WITH (FORCE)");
            }
        }
    }

    private static Connection adminConnection() throws SQLException {
        return DriverManager.getConnection(adminDatabaseJdbcUrl(), POSTGRES_USERNAME, POSTGRES_PASSWORD);
    }

    private static String adminDatabaseJdbcUrl() {
        return "jdbc:postgresql://" + POSTGRES_HOST + ":" + POSTGRES_PORT + "/" + POSTGRES_ADMIN_DATABASE;
    }

    private static String testDatabaseJdbcUrl() {
        return "jdbc:postgresql://" + POSTGRES_HOST + ":" + POSTGRES_PORT + "/" + TEST_DATABASE;
    }
}
