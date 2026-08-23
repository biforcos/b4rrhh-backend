package com.b4rrhh.employee.working_time.infrastructure.persistence;

import com.b4rrhh.support.TestPostgres;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AgreementAnnualHoursLookupAdapter.class, AgreementCatalogLookupAdapter.class})
class AgreementAnnualHoursLookupRealSeedFlywayIntegrationTest {

    private static final String REAL_AGREEMENT_CODE = "99002405011982";
    private static final String POSTGRES_HOST = TestPostgres.host();
    private static final int POSTGRES_PORT = TestPostgres.port();
    private static final String POSTGRES_ADMIN_DATABASE = "postgres";
    private static final String POSTGRES_USERNAME = "b4rrhh";
    private static final String POSTGRES_PASSWORD = "b4rrhh";
    private static final String TEST_DATABASE = "agreementseed" + UUID.randomUUID().toString().replace("-", "");

    @TempDir
    static Path tempDir;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AgreementAnnualHoursLookupAdapter adapter;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        Path migrationDirectory = Files.createDirectories(tempDir.resolve("flyway-agreement-real-seed"));
        copyMigration(migrationDirectory, "V1__initial_personnel_model.sql");
        copyMigration(migrationDirectory, "V22__create_rulesystem_agreement_category_relation_table.sql");
        copyMigration(migrationDirectory, "V24__seed_rule_entity_type_for_employee_labor_classification.sql");
        copyMigration(migrationDirectory, "V49__seed_esp_baseline_rule_system.sql");
        copyMigration(migrationDirectory, "V59__create_agreement_profile_table.sql");
        copyMigration(migrationDirectory, "V61__seed_esp_real_agreement_boe_a_2023_13740.sql");

        recreateDatabase();

        registry.add("spring.datasource.url", AgreementAnnualHoursLookupRealSeedFlywayIntegrationTest::testDatabaseJdbcUrl);
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
    void flywaySeedCreatesRealAgreementProfileAndLookupResolvesAnnualHours() {
        Integer agreementCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from rulesystem.rule_entity
                where rule_system_code = ?
                  and rule_entity_type_code = 'AGREEMENT'
                  and code = ?
                """,
                Integer.class,
                "ESP",
                REAL_AGREEMENT_CODE
        );
        Integer profileCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from rulesystem.agreement_profile profile
                join rulesystem.rule_entity agreement on agreement.id = profile.agreement_rule_entity_id
                where agreement.rule_system_code = ?
                  and agreement.rule_entity_type_code = 'AGREEMENT'
                  and agreement.code = ?
                """,
                Integer.class,
                "ESP",
                REAL_AGREEMENT_CODE
        );
        BigDecimal annualHours = jdbcTemplate.queryForObject(
                """
                select profile.annual_hours
                from rulesystem.agreement_profile profile
                join rulesystem.rule_entity agreement on agreement.id = profile.agreement_rule_entity_id
                where agreement.rule_system_code = ?
                  and agreement.rule_entity_type_code = 'AGREEMENT'
                  and agreement.code = ?
                """,
                BigDecimal.class,
                "ESP",
                REAL_AGREEMENT_CODE
        );

        BigDecimal resolvedAnnualHours = adapter.resolveAnnualHours("ESP", REAL_AGREEMENT_CODE);

        assertEquals(1, agreementCount);
        assertEquals(1, profileCount);
        assertEquals(0, new BigDecimal("1736.00").compareTo(annualHours));
        assertEquals(0, new BigDecimal("1736.00").compareTo(resolvedAnnualHours));
    }

    private static void copyMigration(Path migrationDirectory, String fileName) throws IOException {
        Path target = migrationDirectory.resolve(fileName);
        try (InputStream inputStream = AgreementAnnualHoursLookupRealSeedFlywayIntegrationTest.class
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