package com.b4rrhh.employee.workcenter.infrastructure.persistence;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.EsquemaRealInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
class SpringDataWorkCenterRepositoryIntegrationTest {

    @Autowired
    private SpringDataWorkCenterRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void enforcesUniqueConstraintPerEmployeeAndAssignmentNumber() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        repository.saveAndFlush(workCenterEntity(empleado, 1, LocalDate.of(2026, 1, 1), null));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(workCenterEntity(empleado, 1, LocalDate.of(2026, 2, 1), null))
        );
    }

    @Test
    void detectsOverlappingPeriodsUsingRepositoryQuery() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        repository.saveAndFlush(workCenterEntity(empleado, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));

        boolean overlap = repository.existsOverlappingPeriod(
                empleado,
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 2, 15),
                LocalDate.of(9999, 12, 31)
        );

        assertTrue(overlap);
    }

    private WorkCenterEntity workCenterEntity(Long employeeId, Integer assignmentNumber, LocalDate startDate, LocalDate endDate) {
        WorkCenterEntity entity = new WorkCenterEntity();
        entity.setEmployeeId(employeeId);
        entity.setWorkCenterAssignmentNumber(assignmentNumber);
        entity.setWorkCenterCode("MADRID_HQ");
        entity.setStartDate(startDate);
        entity.setEndDate(endDate);
        return entity;
    }
}