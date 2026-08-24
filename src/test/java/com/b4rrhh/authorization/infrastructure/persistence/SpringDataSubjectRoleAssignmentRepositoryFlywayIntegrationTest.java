package com.b4rrhh.authorization.infrastructure.persistence;

import com.b4rrhh.support.TestPostgresInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
class SpringDataSubjectRoleAssignmentRepositoryFlywayIntegrationTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private SpringDataSubjectRoleAssignmentRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        Path migrationDirectory = Files.createDirectories(tempDir.resolve("flyway-authz"));
        copyMigration(migrationDirectory, "V44__create_authorization_schema.sql");
        copyMigration(migrationDirectory, "V45__seed_authorization_initial_data.sql");
        copyMigration(migrationDirectory, "V46__add_policy_effect_to_role_resource_policy.sql");
        copyMigration(migrationDirectory, "V47__create_subject_role_assignment.sql");
        copyMigration(migrationDirectory, "V48__align_subject_role_assignment_subject_seeds_with_opaque_subjects.sql");

        registry.add("spring.flyway.locations", () -> "filesystem:" + migrationDirectory.toAbsolutePath());
    }

    @Test
    void usesRealFlywayMigrationsAndReturnsOnlyActiveAssignmentsForRequestedSubject() {
        jdbcTemplate.update(
                "insert into authz.subject_role_assignment (subject_code, role_code, active, assignment_origin) values (?, ?, ?, ?)",
            "bifor",
                "READONLY",
                true,
                "INTERNAL"
        );
        jdbcTemplate.update(
                "insert into authz.subject_role_assignment (subject_code, role_code, active, assignment_origin) values (?, ?, ?, ?)",
            "bifor",
                "AUDITOR",
                false,
                "DEV"
        );
        jdbcTemplate.update(
                "insert into authz.subject_role_assignment (subject_code, role_code, active, assignment_origin) values (?, ?, ?, ?)",
                "OTHER.SUBJECT",
                "AUDITOR",
                true,
                "SYNC"
        );

            List<SubjectRoleAssignmentEntity> assignments = repository.findBySubjectCodeAndActiveTrue("bifor");

        assertThat(assignments)
                .extracting(SubjectRoleAssignmentEntity::getRoleCode)
                .containsExactlyInAnyOrder("ADMIN", "READONLY");
    }

    private static void copyMigration(Path migrationDirectory, String fileName) throws IOException {
        Path target = migrationDirectory.resolve(fileName);
        try (InputStream inputStream = SpringDataSubjectRoleAssignmentRepositoryFlywayIntegrationTest.class
                .getClassLoader()
                .getResourceAsStream("db/migration/" + fileName)) {
            if (inputStream == null) {
                throw new IllegalStateException("Migration not found on classpath: " + fileName);
            }
            Files.copy(inputStream, target);
        }
    }
}