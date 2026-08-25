package com.b4rrhh.employee.working_time.infrastructure.persistence;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.EsquemaRealInitializer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
class WorkingTimeConsistencyAdaptersIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private WorkingTimePresenceConsistencyAdapter presenceConsistencyAdapter;

    @BeforeEach
    void setUp() {
        presenceConsistencyAdapter = new WorkingTimePresenceConsistencyAdapter(entityManager);
    }

    @Test
    void openEndedWorkingTimeIsInsideOpenEndedPresence() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        DatosDePrueba.presencia(jdbcTemplate, empleado, 1, LocalDate.of(2020, 1, 5), null);

        boolean inside = presenceConsistencyAdapter.existsPresenceContainingPeriod(
                empleado,
                LocalDate.of(2020, 1, 5),
                null
        );

        assertTrue(inside);
    }

    @Test
    void openEndedWorkingTimeIsOutsideClosedPresence() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        DatosDePrueba.presencia(jdbcTemplate, empleado, 1, LocalDate.of(2020, 1, 5), LocalDate.of(2020, 12, 31));

        boolean outside = presenceConsistencyAdapter.existsPresenceContainingPeriod(
                empleado,
                LocalDate.of(2020, 1, 5),
                null
        );

        assertFalse(outside);
    }
}