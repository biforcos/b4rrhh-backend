package com.b4rrhh.employee.working_time.infrastructure.persistence;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestSobreEsquemaReal
class SpringDataWorkingTimeRepositoryIntegrationTest {

    @Autowired
    private SpringDataWorkingTimeRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void enforcesUniqueConstraintPerEmployeeAndWorkingTimeNumber() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        repository.saveAndFlush(workingTimeEntity(empleado, 1, LocalDate.of(2026, 1, 1), null));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(workingTimeEntity(empleado, 1, LocalDate.of(2026, 2, 1), null))
        );
    }

    @Test
    void detectsOverlappingPeriodsUsingRepositoryQuery() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        repository.saveAndFlush(workingTimeEntity(empleado, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));

        boolean overlap = repository.existsOverlappingPeriod(
                empleado,
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 2, 15),
                LocalDate.of(9999, 12, 31)
        );

        assertTrue(overlap);
    }

    private WorkingTimeEntity workingTimeEntity(Long employeeId, Integer workingTimeNumber, LocalDate startDate, LocalDate endDate) {
        WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setEmployeeId(employeeId);
        entity.setWorkingTimeNumber(workingTimeNumber);
        entity.setStartDate(startDate);
        entity.setEndDate(endDate);
        entity.setWorkingTimePercentage(new BigDecimal("50.00"));
        entity.setWeeklyHours(new BigDecimal("20.00"));
        entity.setDailyHours(new BigDecimal("4.00"));
        entity.setMonthlyHours(new BigDecimal("83.33"));
        return entity;
    }
}