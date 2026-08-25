package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.support.EsquemaRealInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
// El esquema no lo declara el test: es el de produccion, aplicado por Flyway
// una vez y clonado para este contexto (ver EsquemaReal). El DDL a mano que
// habia aqui llevaba tiempo divergiendo del real.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
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
