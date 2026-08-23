package com.b4rrhh.employee.working_time.application.usecase;

import com.b4rrhh.support.TestPostgres;

import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeBusinessKeyLookupSupport;
import com.b4rrhh.employee.working_time.application.service.DefaultWorkingTimePresenceConsistencyValidator;
import com.b4rrhh.employee.working_time.application.service.StandardWorkingTimeDerivationPolicy;
import com.b4rrhh.employee.working_time.infrastructure.persistence.AgreementAnnualHoursLookupAdapter;
import com.b4rrhh.employee.working_time.infrastructure.persistence.EmployeeAgreementContextLookupAdapter;
import com.b4rrhh.employee.working_time.infrastructure.persistence.EmployeeWorkingTimeLookupAdapter;
import com.b4rrhh.employee.working_time.infrastructure.persistence.WorkingTimePersistenceAdapter;
import com.b4rrhh.employee.working_time.infrastructure.persistence.WorkingTimePresenceConsistencyAdapter;
import com.b4rrhh.rulesystem.agreementprofile.infrastructure.persistence.AgreementCatalogLookupAdapter;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
    "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        CreateWorkingTimeService.class,
        WorkingTimePersistenceAdapter.class,
        EmployeeWorkingTimeLookupAdapter.class,
        EmployeeBusinessKeyLookupSupport.class,
        EmployeeAgreementContextLookupAdapter.class,
        AgreementAnnualHoursLookupAdapter.class,
        AgreementCatalogLookupAdapter.class,
        DefaultWorkingTimePresenceConsistencyValidator.class,
        WorkingTimePresenceConsistencyAdapter.class,
        StandardWorkingTimeDerivationPolicy.class
})
class CreateWorkingTimeRealAgreementFlywayIntegrationTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String REAL_AGREEMENT_CODE = "99002405011982";
    private static final String REAL_AGREEMENT_CATEGORY_CODE = "99002405-G2";
    private static final String POSTGRES_HOST = TestPostgres.host();
    private static final int POSTGRES_PORT = TestPostgres.port();
    private static final String POSTGRES_ADMIN_DATABASE = "postgres";
    private static final String POSTGRES_USERNAME = "b4rrhh";
    private static final String POSTGRES_PASSWORD = "b4rrhh";
    private static final String TEST_DATABASE = "workingtime" + UUID.randomUUID().toString().replace("-", "");

    @TempDir
    static Path tempDir;

    @Autowired
    private CreateWorkingTimeService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        Path migrationDirectory = Files.createDirectories(tempDir.resolve("flyway-create-working-time-real-agreement"));
        copyMigration(migrationDirectory, "V1__initial_personnel_model.sql");
        copyMigration(migrationDirectory, "V8__add_employee_type_code_to_employee_business_key.sql");
        copyMigration(migrationDirectory, "V22__create_rulesystem_agreement_category_relation_table.sql");
        copyMigration(migrationDirectory, "V23__create_employee_labor_classification_table.sql");
        copyMigration(migrationDirectory, "V24__seed_rule_entity_type_for_employee_labor_classification.sql");
        copyMigration(migrationDirectory, "V42__create_employee_working_time_table.sql");
        copyMigration(migrationDirectory, "V49__seed_esp_baseline_rule_system.sql");
        copyMigration(migrationDirectory, "V59__create_agreement_profile_table.sql");
        copyMigration(migrationDirectory, "V61__seed_esp_real_agreement_boe_a_2023_13740.sql");
        copyMigration(migrationDirectory, "V83__add_employee_photo_url.sql");

        recreateDatabase();

        registry.add("spring.datasource.url", CreateWorkingTimeRealAgreementFlywayIntegrationTest::testDatabaseJdbcUrl);
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
    void createPersistsWorkingTimeUsingAnnualHoursFromRealAgreementProfile() {
        String employeeNumber = "WT" + (System.nanoTime() % 1_000_000_000L);
        LocalDate startDate = LocalDate.of(2026, 1, 10);

        long employeeId = insertEmployee(employeeNumber);
        insertPresence(employeeId, startDate);
        insertLaborClassification(employeeId);

        var created = service.create(new CreateWorkingTimeCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                employeeNumber,
                startDate,
                new BigDecimal("50")
        ));

        BigDecimal persistedWeeklyHours = jdbcTemplate.queryForObject(
                "select weekly_hours from employee.working_time where employee_id = ? and working_time_number = 1",
                BigDecimal.class,
                employeeId
        );
        BigDecimal persistedDailyHours = jdbcTemplate.queryForObject(
                "select daily_hours from employee.working_time where employee_id = ? and working_time_number = 1",
                BigDecimal.class,
                employeeId
        );
        BigDecimal persistedMonthlyHours = jdbcTemplate.queryForObject(
                "select monthly_hours from employee.working_time where employee_id = ? and working_time_number = 1",
                BigDecimal.class,
                employeeId
        );

        assertNotNull(created.getId());
        assertEquals(1, created.getWorkingTimeNumber());
        assertEquals(0, new BigDecimal("16.69").compareTo(created.getWeeklyHours()));
        assertEquals(0, new BigDecimal("3.34").compareTo(created.getDailyHours()));
        assertEquals(0, new BigDecimal("72.33").compareTo(created.getMonthlyHours()));
        assertEquals(0, new BigDecimal("16.69").compareTo(persistedWeeklyHours));
        assertEquals(0, new BigDecimal("3.34").compareTo(persistedDailyHours));
        assertEquals(0, new BigDecimal("72.33").compareTo(persistedMonthlyHours));
        assertNotEquals(0, new BigDecimal("20.00").compareTo(created.getWeeklyHours()));
    }

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
                "Real",
                "Agreement",
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

    private void insertPresence(long employeeId, LocalDate startDate) {
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
                startDate,
                null
        );
    }

    private void insertLaborClassification(long employeeId) {
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
                REAL_AGREEMENT_CATEGORY_CODE,
                LocalDate.of(2024, 1, 1),
                null
        );
    }

    private static void copyMigration(Path migrationDirectory, String fileName) throws IOException {
        Path target = migrationDirectory.resolve(fileName);
        try (InputStream inputStream = CreateWorkingTimeRealAgreementFlywayIntegrationTest.class
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