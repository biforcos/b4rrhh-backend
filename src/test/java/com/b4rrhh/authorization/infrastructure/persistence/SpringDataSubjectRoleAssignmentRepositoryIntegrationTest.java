package com.b4rrhh.authorization.infrastructure.persistence;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestSobreEsquemaReal
// Los roles ADMIN y AUDITOR vienen sembrados por las migraciones, igual que la
// asignacion bifor -> ADMIN (V47/V48). Por eso estos tests usan sujetos
// propios: con 'bifor' chocarian contra la clave primaria de la semilla.
class SpringDataSubjectRoleAssignmentRepositoryIntegrationTest {

    @Autowired
    private SpringDataSubjectRoleAssignmentRepository repository;

    @Test
    void findsOnlyActiveAssignmentsForRequestedSubject() {
        repository.saveAndFlush(assignment("test.subject", "ADMIN", true, "DEV"));
        repository.saveAndFlush(assignment("test.subject", "AUDITOR", false, "INTERNAL"));
        repository.saveAndFlush(assignment("other.subject", "AUDITOR", true, "DEV"));

        List<SubjectRoleAssignmentEntity> assignments = repository.findBySubjectCodeAndActiveTrue("test.subject");

        assertEquals(1, assignments.size());
        assertEquals("ADMIN", assignments.get(0).getRoleCode());
    }

    @Test
    void populatesTimestampsOnPersist() {
        SubjectRoleAssignmentEntity saved = repository.saveAndFlush(assignment("test.timestamps", "ADMIN", true, "DEV"));

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