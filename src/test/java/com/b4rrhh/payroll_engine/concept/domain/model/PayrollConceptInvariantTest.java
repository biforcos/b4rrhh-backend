package com.b4rrhh.payroll_engine.concept.domain.model;

import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayrollConceptInvariantTest {

    private PayrollObject conceptObject(String code) {
        return new PayrollObject(1L, "ESP", PayrollObjectTypeCode.CONCEPT, code, null, null);
    }

    private PayrollObject tableObject(String code) {
        return new PayrollObject(2L, "ESP", PayrollObjectTypeCode.TABLE, code, null, null);
    }

    @Test
    void conceptRequiresObjectTypeCodeConcept() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new PayrollConcept(
                        tableObject("TABLA1"),
                        "TABLA_MNE",
                        CalculationType.DIRECT_AMOUNT,
                        FunctionalNature.BASE,
                        null,
                        ExecutionScope.SEGMENT,
                        null, null
                ));

        assertTrue(ex.getMessage().contains("objectTypeCode=CONCEPT"));
    }

    @Test
    void conceptAcceptsObjectWithTypeCodeConcept() {
        // must not throw
        new PayrollConcept(
                conceptObject("SALBASE"),
                "SAL_BASE",
                CalculationType.DIRECT_AMOUNT,
                FunctionalNature.EARNING,
                null,
                ExecutionScope.SEGMENT,
                null, null
        );
    }
}
