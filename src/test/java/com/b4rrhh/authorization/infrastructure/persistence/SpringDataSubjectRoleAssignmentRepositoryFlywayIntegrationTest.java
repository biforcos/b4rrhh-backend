package com.b4rrhh.authorization.infrastructure.persistence;

import com.b4rrhh.support.EsquemaRealInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
// El esquema no lo declara el test: es el de produccion, aplicado por Flyway
// una vez y clonado para este contexto (ver EsquemaReal). El subconjunto de
// migraciones que se copiaba aqui estaba congelado: una migracion futura que
// tocara estas tablas no se veia. El ADMIN de bifor que espera el assert
// viene sembrado por las migraciones (V47/V48), igual que antes.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
class SpringDataSubjectRoleAssignmentRepositoryFlywayIntegrationTest {

    @Autowired
    private SpringDataSubjectRoleAssignmentRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
}