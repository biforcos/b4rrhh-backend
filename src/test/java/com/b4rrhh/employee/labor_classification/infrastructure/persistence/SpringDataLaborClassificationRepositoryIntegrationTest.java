package com.b4rrhh.employee.labor_classification.infrastructure.persistence;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

@TestSobreEsquemaReal
// El esquema es el de produccion: el empleado tiene que existir de verdad
// (clave ajena) y su id lo asigna la base ('generated always').
//
// Lo que se prueba aqui son restricciones de base —unicidad, clave ajena,
// solape—, y para eso el codigo de convenio es una cadena opaca. Aun asi son
// los del convenio real: si manana alguien mete una comprobacion de catalogo
// en esta ruta, este test tiene que enterarse en vez de seguir verde por el
// motivo equivocado (backend#6).
class SpringDataLaborClassificationRepositoryIntegrationTest {

    @Autowired
    private SpringDataLaborClassificationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void enforcesUniqueConstraintByFunctionalIdentity() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        repository.saveAndFlush(entity(empleado, "99002405011982", "99002405-G3", LocalDate.of(2026, 1, 1), null));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(entity(
                        empleado,
                        "99002405011982",
                        "99002405-G1",
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
                        "99002405011982",
                        "99002405-G3",
                        LocalDate.of(2026, 1, 1),
                        null
                ))
        );
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
