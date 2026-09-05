package com.b4rrhh.payroll_engine.concept.domain.model;

import com.b4rrhh.payroll_engine.concept.domain.exception.OperandCrossesSegmentToPeriodException;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// La regla es de una sola direccion (ADR-058): la unica arista prohibida es un
// operando de un concepto PERIOD cuyo origen es SEGMENT. Las otras tres
// combinaciones de ambito son validas.
class OperandScopeInvariantTest {

    @Test
    void periodTargetReadingSegmentSourceIsRejectedNamingRoleAndBothConcepts() {
        PayrollConcept target = concept(1L, "800", "IRPF", ExecutionScope.PERIOD);
        PayrollConcept source = concept(2L, "101", "SALARIO_BASE", ExecutionScope.SEGMENT);

        assertThat(OperandScopeInvariant.crossesSegmentToPeriod(target, source)).isTrue();
        assertThatThrownBy(() -> OperandScopeInvariant.check(target, OperandRole.BASE, source))
                .isInstanceOf(OperandCrossesSegmentToPeriodException.class)
                .hasMessageContaining("BASE")
                .hasMessageContaining("ESP/800 (IRPF)")
                .hasMessageContaining("ESP/101 (SALARIO_BASE)")
                .hasMessageContaining("ADR-058");
    }

    @Test
    void segmentTargetMayReadSegmentSource() {
        PayrollConcept target = concept(1L, "101", "SALARIO_BASE", ExecutionScope.SEGMENT);
        PayrollConcept source = concept(2L, "D01", "DIAS_DEVENGO", ExecutionScope.SEGMENT);

        assertThat(OperandScopeInvariant.crossesSegmentToPeriod(target, source)).isFalse();
        assertThatCode(() -> OperandScopeInvariant.check(target, OperandRole.QUANTITY, source))
                .doesNotThrowAnyException();
    }

    @Test
    void segmentTargetMayReadPeriodSource() {
        PayrollConcept target = concept(1L, "101", "SALARIO_BASE", ExecutionScope.SEGMENT);
        PayrollConcept source = concept(2L, "P01", "PRECIO_DIA", ExecutionScope.PERIOD);

        assertThatCode(() -> OperandScopeInvariant.check(target, OperandRole.RATE, source))
                .doesNotThrowAnyException();
    }

    @Test
    void periodTargetMayReadPeriodSource() {
        PayrollConcept target = concept(1L, "B_CC", "BASE_COTIZACION_COTIZ", ExecutionScope.PERIOD);
        PayrollConcept source = concept(2L, "B_CC_MAX", "BASE_COTIZACION_MAX", ExecutionScope.PERIOD);

        assertThatCode(() -> OperandScopeInvariant.check(target, OperandRole.LEFT, source))
                .doesNotThrowAnyException();
    }

    private static PayrollConcept concept(Long id, String code, String mnemonic, ExecutionScope scope) {
        LocalDateTime now = LocalDateTime.now();
        PayrollObject object = new PayrollObject(id, "ESP", PayrollObjectTypeCode.CONCEPT, code, now, now);
        return new PayrollConcept(object, mnemonic, CalculationType.DIRECT_AMOUNT,
                FunctionalNature.TECHNICAL, null, scope, now, now);
    }
}
