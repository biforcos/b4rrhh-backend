package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestSobreEsquemaReal
// El codigo del sistema no puede ser ESP: ESP, FRA y PRT ya vienen sembrados
// por las migraciones y rule_system.code es unico.
class SpringDataRuleSystemRepositoryIntegrationTest {

    @Autowired
    private SpringDataRuleSystemRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createSetsCreatedAtAndUpdatedAt() {
        RuleSystemEntity entity = new RuleSystemEntity();
        entity.setCode("TST");
        entity.setName("Sistema de Reglas de Prueba");
        entity.setCountryCode("ESP");
        entity.setActive(true);

        RuleSystemEntity saved = repository.saveAndFlush(entity);

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertTrue(!saved.getUpdatedAt().isBefore(saved.getCreatedAt()));
    }

    @Test
    void updatePreservesCreatedAtRefreshesUpdatedAtAndMutatesBusinessFields() throws InterruptedException {
        RuleSystemEntity entity = new RuleSystemEntity();
        entity.setCode("TST");
        entity.setName("Sistema de Reglas de Prueba");
        entity.setCountryCode("ESP");
        entity.setActive(true);

        RuleSystemEntity created = repository.saveAndFlush(entity);
        LocalDateTime createdAt = created.getCreatedAt();
        LocalDateTime updatedAtBeforeUpdate = created.getUpdatedAt();

        Thread.sleep(5L);

        RuleSystemEntity managed = repository.findByCode("TST").orElseThrow();
        managed.setName("Sistema de Reglas de Prueba Actualizado");
        managed.setCountryCode("FRA");
        managed.setActive(false);

        RuleSystemEntity updated = repository.saveAndFlush(managed);

        assertEquals(createdAt, updated.getCreatedAt());
        assertTrue(updated.getUpdatedAt().isAfter(updatedAtBeforeUpdate));
        assertEquals("Sistema de Reglas de Prueba Actualizado", updated.getName());
        assertEquals("FRA", updated.getCountryCode());
        assertEquals(false, updated.isActive());
    }
}
