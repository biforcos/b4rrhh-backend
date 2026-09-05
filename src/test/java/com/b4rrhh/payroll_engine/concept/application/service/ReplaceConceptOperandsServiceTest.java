package com.b4rrhh.payroll_engine.concept.application.service;

import com.b4rrhh.payroll_engine.concept.application.usecase.ReplaceConceptOperandsCommand;
import com.b4rrhh.payroll_engine.concept.domain.exception.OperandCrossesSegmentToPeriodException;
import com.b4rrhh.payroll_engine.concept.domain.exception.PayrollConceptNotFoundException;
import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;
import com.b4rrhh.payroll_engine.concept.domain.model.OperandRole;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptOperandRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import com.b4rrhh.payroll_engine.object.domain.exception.PayrollObjectNotFoundException;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// El invariante de aristas de ADR-058 se comprueba al guardar el grafo, y antes de
// borrar nada: un PUT rechazado deja los operandos que habia, no un concepto vacio.
@ExtendWith(MockitoExtension.class)
class ReplaceConceptOperandsServiceTest {

    private static final String RS = "ESP";

    @Mock
    private PayrollConceptOperandRepository operandRepository;

    @Mock
    private PayrollConceptRepository conceptRepository;

    @InjectMocks
    private ReplaceConceptOperandsService service;

    @Test
    void rejectsOperandCrossingFromSegmentToPeriodWithoutTouchingExistingOperands() {
        PayrollConcept target = concept(1L, "800", "IRPF", ExecutionScope.PERIOD);
        PayrollConcept base = concept(2L, "B01", "BASE_IRPF", ExecutionScope.PERIOD);
        PayrollConcept segmentSource = concept(3L, "101", "SALARIO_BASE", ExecutionScope.SEGMENT);
        when(conceptRepository.findByBusinessKey(RS, "800")).thenReturn(Optional.of(target));
        when(conceptRepository.findAllByCodes(eq(RS), anyCollection())).thenReturn(List.of(base, segmentSource));

        ReplaceConceptOperandsCommand command = new ReplaceConceptOperandsCommand(RS, "800", List.of(
                new ReplaceConceptOperandsCommand.Item(OperandRole.BASE, "B01"),
                new ReplaceConceptOperandsCommand.Item(OperandRole.PERCENTAGE, "101")
        ));

        assertThatThrownBy(() -> service.replace(command))
                .isInstanceOf(OperandCrossesSegmentToPeriodException.class)
                .hasMessageContaining("PERCENTAGE")
                .hasMessageContaining("ESP/800 (IRPF)")
                .hasMessageContaining("ESP/101 (SALARIO_BASE)");

        verify(operandRepository, never()).deleteAllByRuleSystemCodeAndConceptCode(anyString(), anyString());
        verify(operandRepository, never()).save(any());
    }

    @Test
    void segmentTargetMayReadSegmentAndPeriodSources() {
        PayrollConcept target = concept(1L, "101", "SALARIO_BASE", ExecutionScope.SEGMENT);
        PayrollConcept quantity = concept(2L, "D01", "DIAS_DEVENGO", ExecutionScope.SEGMENT);
        PayrollConcept rate = concept(3L, "P01", "PRECIO_DIA", ExecutionScope.PERIOD);
        when(conceptRepository.findByBusinessKey(RS, "101")).thenReturn(Optional.of(target));
        when(conceptRepository.findAllByCodes(eq(RS), anyCollection())).thenReturn(List.of(quantity, rate));
        when(operandRepository.save(any(PayrollConceptOperand.class))).thenAnswer(inv -> inv.getArgument(0));

        List<PayrollConceptOperand> result = service.replace(new ReplaceConceptOperandsCommand(RS, "101", List.of(
                new ReplaceConceptOperandsCommand.Item(OperandRole.QUANTITY, "D01"),
                new ReplaceConceptOperandsCommand.Item(OperandRole.RATE, "P01")
        )));

        verify(operandRepository).deleteAllByRuleSystemCodeAndConceptCode(RS, "101");
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PayrollConceptOperand::getOperandRole)
                .containsExactly(OperandRole.QUANTITY, OperandRole.RATE);
        assertThat(result).extracting(o -> o.getSourceObject().getObjectCode())
                .containsExactly("D01", "P01");
        assertThat(result).allMatch(o -> o.getTargetObject().getObjectCode().equals("101"));
    }

    @Test
    void emptyItemsClearEveryOperand() {
        PayrollConcept target = concept(1L, "101", "SALARIO_BASE", ExecutionScope.SEGMENT);
        when(conceptRepository.findByBusinessKey(RS, "101")).thenReturn(Optional.of(target));
        when(conceptRepository.findAllByCodes(eq(RS), anyCollection())).thenReturn(List.of());

        List<PayrollConceptOperand> result = service.replace(new ReplaceConceptOperandsCommand(RS, "101", List.of()));

        assertThat(result).isEmpty();
        verify(operandRepository).deleteAllByRuleSystemCodeAndConceptCode(RS, "101");
        verify(operandRepository, never()).save(any());
    }

    @Test
    void throwsNotFoundForUnknownTarget() {
        when(conceptRepository.findByBusinessKey(RS, "999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replace(new ReplaceConceptOperandsCommand(RS, "999", List.of())))
                .isInstanceOf(PayrollConceptNotFoundException.class);

        verify(operandRepository, never()).deleteAllByRuleSystemCodeAndConceptCode(anyString(), anyString());
    }

    @Test
    void throwsObjectNotFoundForUnknownSourceBeforeDeletingAnything() {
        PayrollConcept target = concept(1L, "101", "SALARIO_BASE", ExecutionScope.SEGMENT);
        when(conceptRepository.findByBusinessKey(RS, "101")).thenReturn(Optional.of(target));
        when(conceptRepository.findAllByCodes(eq(RS), any(Collection.class))).thenReturn(List.of());

        assertThatThrownBy(() -> service.replace(new ReplaceConceptOperandsCommand(RS, "101", List.of(
                new ReplaceConceptOperandsCommand.Item(OperandRole.QUANTITY, "NOT_A_CONCEPT")))))
                .isInstanceOf(PayrollObjectNotFoundException.class)
                .hasMessageContaining("NOT_A_CONCEPT");

        verify(operandRepository, never()).deleteAllByRuleSystemCodeAndConceptCode(anyString(), anyString());
    }

    private static PayrollConcept concept(Long id, String code, String mnemonic, ExecutionScope scope) {
        LocalDateTime now = LocalDateTime.now();
        PayrollObject object = new PayrollObject(id, RS, PayrollObjectTypeCode.CONCEPT, code, now, now);
        return new PayrollConcept(object, mnemonic, CalculationType.DIRECT_AMOUNT,
                FunctionalNature.TECHNICAL, null, scope, now, now);
    }
}
