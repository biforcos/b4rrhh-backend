package com.b4rrhh.authorization.infrastructure.persistence;

import com.b4rrhh.support.TestPostgresInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
// Estos tests levantan su propio esquema a mano en @BeforeEach, y hasta ahora
// lo hacian contra H2. El DDL es el mismo; el motor no. Comprobar una
// restriccion de integridad en una base que no es la de produccion solo
// demuestra que H2 la respeta.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = TestPostgresInitializer.class)
class SpringDataSubjectRoleAssignmentRepositoryIntegrationTest {

    @Autowired
    private SpringDataSubjectRoleAssignmentRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute("create schema if not exists authz");
        jdbcTemplate.execute("drop table if exists authz.subject_role_assignment");
        jdbcTemplate.execute("drop table if exists authz.role");
        jdbcTemplate.execute("""
                create table authz.role (
                    code varchar(50) not null primary key,
                    name varchar(100) not null,
                    description text,
                    active boolean not null default true,
                    created_at timestamp not null default now(),
                    updated_at timestamp not null default now()
                )
                """);
        jdbcTemplate.execute("""
                create table authz.subject_role_assignment (
                    subject_code varchar(100) not null,
                    role_code varchar(50) not null,
                    active boolean not null default true,
                    assignment_origin varchar(20) not null,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    constraint pk_subject_role_assignment primary key (subject_code, role_code),
                    constraint fk_subject_role_assignment_role foreign key (role_code) references authz.role(code),
                    constraint chk_subject_role_assignment_origin check (assignment_origin in ('INTERNAL', 'DEV', 'SYNC'))
                )
                """);

        jdbcTemplate.update("insert into authz.role (code, name) values (?, ?)", "ADMIN", "Administrator");
        jdbcTemplate.update("insert into authz.role (code, name) values (?, ?)", "AUDITOR", "Auditor");
    }

    @Test
    void findsOnlyActiveAssignmentsForRequestedSubject() {
        repository.saveAndFlush(assignment("bifor", "ADMIN", true, "DEV"));
        repository.saveAndFlush(assignment("bifor", "AUDITOR", false, "INTERNAL"));
        repository.saveAndFlush(assignment("other", "AUDITOR", true, "DEV"));

        List<SubjectRoleAssignmentEntity> assignments = repository.findBySubjectCodeAndActiveTrue("bifor");

        assertEquals(1, assignments.size());
        assertEquals("ADMIN", assignments.get(0).getRoleCode());
    }

    @Test
    void populatesTimestampsOnPersist() {
        SubjectRoleAssignmentEntity saved = repository.saveAndFlush(assignment("bifor", "ADMIN", true, "DEV"));

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertTrue(!saved.getUpdatedAt().isBefore(saved.getCreatedAt()));
    }

    private SubjectRoleAssignmentEntity assignment(String subjectCode, String roleCode, boolean active, String origin) {
        SubjectRoleAssignmentEntity entity = new SubjectRoleAssignmentEntity();
        entity.setSubjectCode(subjectCode);
        entity.setRoleCode(roleCode);
        entity.setActive(active);
        entity.setAssignmentOrigin(origin);
        entity.setCreatedAt((LocalDateTime) null);
        entity.setUpdatedAt((LocalDateTime) null);
        return entity;
    }
}