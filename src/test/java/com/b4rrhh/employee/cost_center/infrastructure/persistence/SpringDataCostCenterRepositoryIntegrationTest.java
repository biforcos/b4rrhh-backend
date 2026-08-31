package com.b4rrhh.employee.cost_center.infrastructure.persistence;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestSobreEsquemaReal
// El esquema es el de produccion: el empleado tiene que existir de verdad
// (clave ajena) y su id lo asigna la base ('generated always').
class SpringDataCostCenterRepositoryIntegrationTest {

    @Autowired
    private SpringDataCostCenterRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void enforcesUniqueConstraintByFunctionalIdentity() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        repository.saveAndFlush(costCenterEntity(empleado, "CC01", new BigDecimal("50"), LocalDate.of(2026, 1, 1), null));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(
                        costCenterEntity(empleado, "CC01", new BigDecimal("70"), LocalDate.of(2026, 1, 1), null)
                )
        );
    }

    @Test
    void enforcesForeignKeyConstraint() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(
                        costCenterEntity(999999999L, "CC01", new BigDecimal("50"), LocalDate.of(2026, 1, 1), null)
                )
        );
    }

    @Test
    void detectsOverlappingPeriodsOnlyForSameCostCenter() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        repository.saveAndFlush(
                costCenterEntity(
                        empleado,
                        "CC01",
                        new BigDecimal("50"),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31)
                )
        );

        boolean overlapSameCode = repository.existsOverlappingPeriodByCostCenterCode(
                empleado,
                "CC01",
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 2, 15),
                LocalDate.of(9999, 12, 31)
        );
        boolean overlapDifferentCode = repository.existsOverlappingPeriodByCostCenterCode(
                empleado,
                "CC02",
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 2, 15),
                LocalDate.of(9999, 12, 31)
        );

        assertTrue(overlapSameCode);
        assertFalse(overlapDifferentCode);
    }

    private CostCenterEntity costCenterEntity(
            Long employeeId,
            String costCenterCode,
            BigDecimal allocationPercentage,
            LocalDate startDate,
            LocalDate endDate
    ) {
        CostCenterEntity entity = new CostCenterEntity();
        entity.setEmployeeId(employeeId);
        entity.setCostCenterCode(costCenterCode);
        entity.setAllocationPercentage(allocationPercentage);
        entity.setStartDate(startDate);
        entity.setEndDate(endDate);
        return entity;
    }
}
