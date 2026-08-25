package com.b4rrhh.employee.identifier.infrastructure.persistence;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestSobreEsquemaReal
class SpringDataIdentifierRepositoryIntegrationTest {

    @Autowired
    private SpringDataIdentifierRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void enforcesUniqueConstraintPerEmployeeAndIdentifierType() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        repository.saveAndFlush(identifierEntity(empleado, "NATIONAL_ID", "12345678A", true));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(identifierEntity(empleado, "NATIONAL_ID", "87654321Z", false))
        );
    }

    @Test
    void allowsSameIdentifierTypeForDifferentEmployees() {
        Long uno = DatosDePrueba.empleado(jdbcTemplate);
        Long otro = DatosDePrueba.empleado(jdbcTemplate);
        repository.saveAndFlush(identifierEntity(uno, "NATIONAL_ID", "12345678A", true));
        repository.saveAndFlush(identifierEntity(otro, "NATIONAL_ID", "87654321Z", false));

        assertTrue(repository.findByEmployeeIdAndIdentifierTypeCode(otro, "NATIONAL_ID").isPresent());
    }

    @Test
    void supportsPrimaryLookupMethods() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        repository.saveAndFlush(identifierEntity(empleado, "NATIONAL_ID", "12345678A", true));
        repository.saveAndFlush(identifierEntity(empleado, "PASSPORT", "PA123456", false));

        assertTrue(repository.existsByEmployeeIdAndIsPrimaryTrue(empleado));
        assertTrue(repository.existsByEmployeeIdAndIsPrimaryTrueAndIdentifierTypeCodeNot(empleado, "PASSPORT"));
        assertFalse(repository.existsByEmployeeIdAndIsPrimaryTrueAndIdentifierTypeCodeNot(empleado, "NATIONAL_ID"));
    }

    private IdentifierEntity identifierEntity(Long employeeId, String identifierTypeCode, String identifierValue, boolean isPrimary) {
        IdentifierEntity entity = new IdentifierEntity();
        entity.setEmployeeId(employeeId);
        entity.setIdentifierTypeCode(identifierTypeCode);
        entity.setIdentifierValue(identifierValue);
        entity.setIssuingCountryCode("ESP");
        entity.setPrimary(isPrimary);
        return entity;
    }
}
