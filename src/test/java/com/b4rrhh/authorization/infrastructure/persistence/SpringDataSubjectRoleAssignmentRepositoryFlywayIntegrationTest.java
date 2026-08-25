package com.b4rrhh.authorization.infrastructure.persistence;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestSobreEsquemaReal
// El ADMIN de bifor que espera el assert viene sembrado por las migraciones
// (V47/V48), igual que antes.
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