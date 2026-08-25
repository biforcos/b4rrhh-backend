package com.b4rrhh.payroll_engine.object.infrastructure.persistence;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestSobreEsquemaReal
// Los codigos de objeto que usa el test no estan entre los que siembran las
// migraciones.
class PayrollObjectPersistenceAdapterTest {

    @Autowired
    private SpringDataPayrollObjectRepository repository;

    @Test
    void persistsPayrollObjectAndLoadsItByBusinessKey() {
        PayrollObjectEntity entity = buildEntity("ESP", "CONCEPT", "SALBASE");
        repository.saveAndFlush(entity);

        Optional<PayrollObjectEntity> found = repository
                .findByRuleSystemCodeAndObjectTypeCodeAndObjectCode("ESP", "CONCEPT", "SALBASE");

        assertTrue(found.isPresent());
        assertEquals("ESP", found.get().getRuleSystemCode());
        assertEquals("CONCEPT", found.get().getObjectTypeCode());
        assertEquals("SALBASE", found.get().getObjectCode());
    }

    @Test
    void enforcesUniqueBusinessKeyConstraint() {
        repository.saveAndFlush(buildEntity("ESP", "CONCEPT", "SALBASE"));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(buildEntity("ESP", "CONCEPT", "SALBASE")));
    }

    @Test
    void allowsDifferentObjectCodesUnderSameRuleSystem() {
        repository.saveAndFlush(buildEntity("ESP", "CONCEPT", "SALBASE"));
        repository.saveAndFlush(buildEntity("ESP", "CONCEPT", "IRPF"));

        assertTrue(repository.findByRuleSystemCodeAndObjectTypeCodeAndObjectCode("ESP", "CONCEPT", "SALBASE").isPresent());
        assertTrue(repository.findByRuleSystemCodeAndObjectTypeCodeAndObjectCode("ESP", "CONCEPT", "IRPF").isPresent());
    }

    @Test
    void allowsSameObjectCodeUnderDifferentObjectTypes() {
        repository.saveAndFlush(buildEntity("ESP", "CONCEPT", "BASE"));
        repository.saveAndFlush(buildEntity("ESP", "TABLE", "BASE"));

        assertTrue(repository.findByRuleSystemCodeAndObjectTypeCodeAndObjectCode("ESP", "CONCEPT", "BASE").isPresent());
        assertTrue(repository.findByRuleSystemCodeAndObjectTypeCodeAndObjectCode("ESP", "TABLE", "BASE").isPresent());
    }

    private PayrollObjectEntity buildEntity(String ruleSystemCode, String objectTypeCode, String objectCode) {
        PayrollObjectEntity entity = new PayrollObjectEntity();
        entity.setRuleSystemCode(ruleSystemCode);
        entity.setObjectTypeCode(objectTypeCode);
        entity.setObjectCode(objectCode);
        return entity;
    }
}
