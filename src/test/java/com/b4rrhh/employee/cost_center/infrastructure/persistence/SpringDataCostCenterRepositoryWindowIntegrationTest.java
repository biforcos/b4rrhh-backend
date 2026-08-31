package com.b4rrhh.employee.cost_center.infrastructure.persistence;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestSobreEsquemaReal
// El esquema es el de produccion: el empleado tiene que existir de verdad
// (clave ajena) y su id lo asigna la base ('generated always').
class SpringDataCostCenterRepositoryWindowIntegrationTest {

    @Autowired
    private SpringDataCostCenterRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findActiveAtDateReturnsLinesWhosePeriodIncludesDate() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        LocalDate start = LocalDate.of(2026, 1, 1);
        repository.saveAndFlush(entity(empleado, "CC_A", new BigDecimal("60"), start, null));
        repository.saveAndFlush(entity(empleado, "CC_B", new BigDecimal("40"), start, null));

        List<CostCenterEntity> active = repository.findActiveAtDate(empleado, LocalDate.of(2026, 4, 1));

        assertEquals(2, active.size());
    }

    @Test
    void findActiveAtDateExcludesClosedLines() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        repository.saveAndFlush(entity(empleado, "CC_A", new BigDecimal("100"), start, end));

        List<CostCenterEntity> activeAfterClose = repository.findActiveAtDate(empleado, LocalDate.of(2026, 4, 1));
        List<CostCenterEntity> activeBeforeClose = repository.findActiveAtDate(empleado, LocalDate.of(2026, 3, 15));

        assertTrue(activeAfterClose.isEmpty());
        assertEquals(1, activeBeforeClose.size());
    }

    @Test
    void findByEmployeeIdAndStartDateReturnsAllLinesInWindow() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        LocalDate start = LocalDate.of(2026, 4, 1);
        repository.saveAndFlush(entity(empleado, "CC_A", new BigDecimal("60"), start, null));
        repository.saveAndFlush(entity(empleado, "CC_B", new BigDecimal("40"), start, null));

        List<CostCenterEntity> window = repository.findByEmployeeIdAndStartDate(empleado, start);

        assertEquals(2, window.size());
    }

    @Test
    void closeAllOpenForWindowSetsEndDateOnlyForOpenLines() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        LocalDate windowStart = LocalDate.of(2026, 1, 1);
        LocalDate closeDate = LocalDate.of(2026, 3, 31);

        repository.saveAndFlush(entity(empleado, "CC_A", new BigDecimal("60"), windowStart, null));
        repository.saveAndFlush(entity(empleado, "CC_B", new BigDecimal("40"), windowStart, null));

        repository.closeAllOpenForWindow(empleado, windowStart, closeDate);
        repository.flush();

        List<CostCenterEntity> after = repository.findByEmployeeIdAndStartDate(empleado, windowStart);
        assertEquals(2, after.size());
        after.forEach(e -> assertEquals(closeDate, e.getEndDate()));
    }

    @Test
    void closeAllOpenForWindowDoesNotAffectAlreadyClosedLines() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        LocalDate windowStart = LocalDate.of(2026, 1, 1);
        LocalDate alreadyClosed = LocalDate.of(2026, 2, 28);

        CostCenterEntity already = entity(empleado, "CC_A", new BigDecimal("100"), windowStart, alreadyClosed);
        repository.saveAndFlush(already);

        repository.closeAllOpenForWindow(empleado, windowStart, LocalDate.of(2026, 3, 31));
        repository.flush();

        List<CostCenterEntity> after = repository.findByEmployeeIdAndStartDate(empleado, windowStart);
        assertEquals(alreadyClosed, after.get(0).getEndDate()); // unchanged
    }

    @Test
    void findActiveAtDateReturnsNothingBeforeWindowStart() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        LocalDate start = LocalDate.of(2026, 4, 1);
        repository.saveAndFlush(entity(empleado, "CC_A", new BigDecimal("100"), start, null));

        List<CostCenterEntity> result = repository.findActiveAtDate(empleado, start.minusDays(1));

        assertTrue(result.isEmpty());
    }

    private CostCenterEntity entity(
            Long employeeId,
            String costCenterCode,
            BigDecimal allocationPercentage,
            LocalDate startDate,
            LocalDate endDate
    ) {
        CostCenterEntity e = new CostCenterEntity();
        e.setEmployeeId(employeeId);
        e.setCostCenterCode(costCenterCode);
        e.setAllocationPercentage(allocationPercentage);
        e.setStartDate(startDate);
        e.setEndDate(endDate);
        return e;
    }
}
