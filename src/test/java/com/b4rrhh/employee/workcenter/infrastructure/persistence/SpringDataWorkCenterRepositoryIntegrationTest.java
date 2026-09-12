package com.b4rrhh.employee.workcenter.infrastructure.persistence;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

@TestSobreEsquemaReal
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