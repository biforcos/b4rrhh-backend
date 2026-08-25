package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import com.b4rrhh.support.TestSobreEsquemaReal;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.PayrollObjectEntity;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.SpringDataPayrollObjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestSobreEsquemaReal
// Los codigos de objeto que usa el test no estan entre los que siembran las
// migraciones.
class PayrollConceptPersistenceAdapterTest {

    @Autowired
    private SpringDataPayrollObjectRepository objectRepository;

    @Autowired
    private SpringDataPayrollConceptRepository conceptRepository;

    @Test
    void persistsConceptWithSemanticPropertiesAndLoadsIt() {
        PayrollObjectEntity objectEntity = saveObject("ESP", "CONCEPT", "SALBASE");

        PayrollConceptEntity conceptEntity = buildConceptEntity(
                objectEntity, "SAL_BASE", "DIRECT_AMOUNT", "EARNING", "REPLACE", null, "SEGMENT"
        );
        conceptRepository.saveAndFlush(conceptEntity);

        Optional<PayrollConceptEntity> found = conceptRepository
                .findByRuleSystemCodeAndConceptCode("ESP", "SALBASE");

        assertTrue(found.isPresent());
        assertEquals("SAL_BASE", found.get().getConceptMnemonic());
        assertEquals("DIRECT_AMOUNT", found.get().getCalculationType());
        assertEquals("EARNING", found.get().getFunctionalNature());
        assertEquals("REPLACE", found.get().getResultCompositionMode());
        assertEquals("SEGMENT", found.get().getExecutionScope());
    }

    @Test
    void persistsConceptWithPeriodExecutionScope() {
        PayrollObjectEntity objectEntity = saveObject("ESP", "CONCEPT", "IRPF");

        PayrollConceptEntity conceptEntity = buildConceptEntity(
                objectEntity, "IRPF_RET", "PERCENTAGE", "DEDUCTION", "REPLACE", "200", "PERIOD"
        );
        conceptRepository.saveAndFlush(conceptEntity);

        Optional<PayrollConceptEntity> found = conceptRepository
                .findByRuleSystemCodeAndConceptCode("ESP", "IRPF");

        assertTrue(found.isPresent());
        assertEquals("PERIOD", found.get().getExecutionScope());
        assertEquals("PERCENTAGE", found.get().getCalculationType());
        assertEquals("200", found.get().getPayslipOrderCode());
    }

    @Test
    void conceptWithoutBaseObjectFails() {
        PayrollConceptEntity orphanConcept = new PayrollConceptEntity();
        // objectId not set, no associated PayrollObjectEntity
        orphanConcept.setConceptMnemonic("ORPHAN");
        orphanConcept.setCalculationType("DIRECT_AMOUNT");
        orphanConcept.setFunctionalNature("EARNING");
        orphanConcept.setResultCompositionMode("REPLACE");
        orphanConcept.setExecutionScope("SEGMENT");

        assertThrows(Exception.class, () -> conceptRepository.saveAndFlush(orphanConcept));
    }

    private PayrollObjectEntity saveObject(String ruleSystemCode, String objectTypeCode, String objectCode) {
        PayrollObjectEntity entity = new PayrollObjectEntity();
        entity.setRuleSystemCode(ruleSystemCode);
        entity.setObjectTypeCode(objectTypeCode);
        entity.setObjectCode(objectCode);
        return objectRepository.saveAndFlush(entity);
    }

    private PayrollConceptEntity buildConceptEntity(
            PayrollObjectEntity objectEntity,
            String mnemonic,
            String calculationType,
            String functionalNature,
            String resultCompositionMode,
            String payslipOrderCode,
            String executionScope
    ) {
        PayrollConceptEntity entity = new PayrollConceptEntity();
        entity.setPayrollObject(objectEntity);
        entity.setConceptMnemonic(mnemonic);
        entity.setCalculationType(calculationType);
        entity.setFunctionalNature(functionalNature);
        entity.setResultCompositionMode(resultCompositionMode);
        entity.setPayslipOrderCode(payslipOrderCode);
        entity.setExecutionScope(executionScope);
        return entity;
    }
}
