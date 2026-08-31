package com.b4rrhh.employee.labor_classification.infrastructure.persistence;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestSobreEsquemaReal
// El esquema es el de produccion: el empleado tiene que existir de verdad
// (clave ajena) y su id lo asigna la base ('generated always').
class SpringDataLaborClassificationRepositoryIntegrationTest {

    @Autowired
    private SpringDataLaborClassificationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void enforcesUniqueConstraintByFunctionalIdentity() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        repository.saveAndFlush(entity(empleado, "AGR_OFFICE", "CAT_ADMIN", LocalDate.of(2026, 1, 1), null));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity(
                        empleado,
                        "AGR_TECH",
                        "CAT_TECH_1",
                        LocalDate.of(2026, 1, 1),
                        null
                ))
        );
    }

    @Test
    void enforcesForeignKeyConstraint() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity(
                        999999999L,
                        "AGR_OFFICE",
                        "CAT_ADMIN",
                        LocalDate.of(2026, 1, 1),
                        null
                ))
        );
    }

    @Test
    void detectsOverlapAndSupportsExcludeStartDate() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        repository.saveAndFlush(entity(
                empleado,
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        ));

        boolean overlap = repository.existsOverlappingPeriod(
                empleado,
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(9999, 12, 31),
                null
        );

        boolean overlapExcluded = repository.existsOverlappingPeriod(
                empleado,
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(9999, 12, 31),
                LocalDate.of(2026, 1, 1)
        );

        boolean nonOverlap = repository.existsOverlappingPeriod(
                empleado,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(9999, 12, 31),
                null
        );

        assertTrue(overlap);
        assertFalse(overlapExcluded);
        assertFalse(nonOverlap);
    }

    private LaborClassificationEntity entity(
            Long employeeId,
            String agreementCode,
            String agreementCategoryCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        LaborClassificationEntity entity = new LaborClassificationEntity();
        entity.setEmployeeId(employeeId);
        entity.setAgreementCode(agreementCode);
        entity.setAgreementCategoryCode(agreementCategoryCode);
        entity.setStartDate(startDate);
        entity.setEndDate(endDate);
        return entity;
    }
}
