package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.PayrollCalculationStep;
import com.b4rrhh.payroll.application.port.PayrollCalculationStepReadPort;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Los dos vacios no son el mismo vacio ({@code backend#97}).
 *
 * <p>Un recibo que no existe y un recibo que existe y no guardo ni un paso responden cosas
 * distintas, y quien los separa es este servicio: {@code Optional} vacio contra lista vacia. Si se
 * juntaran, la pantalla de Valorizacion no podria decir por que no hay nada, que es justo la forma
 * que este issue existe para quitar.
 */
@ExtendWith(MockitoExtension.class)
class ListPayrollCalculationStepsServiceTest {

    @Mock
    private GetPayrollByBusinessKeyUseCase getPayrollByBusinessKeyUseCase;

    @Mock
    private PayrollCalculationStepReadPort payrollCalculationStepReadPort;

    private ListPayrollCalculationStepsService service;

    @BeforeEach
    void setUp() {
        service = new ListPayrollCalculationStepsService(
                getPayrollByBusinessKeyUseCase, payrollCalculationStepReadPort);
    }

    @Test
    void aPayrollThatDoesNotExistAnswersEmptyOptional_andNeverAsksForSteps() {
        when(getPayrollByBusinessKeyUseCase.getByBusinessKey(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Optional.empty());

        assertTrue(steps().isEmpty(), "no hay recibo: la web tiene que poder devolver 404");
        verify(payrollCalculationStepReadPort, never()).findStepsOf(anyLong());
    }

    @Test
    void aPayrollWithoutAnyStepAnswersAnEmptyList_notAnEmptyOptional() {
        givenPayroll(7L);
        when(payrollCalculationStepReadPort.findStepsOf(7L)).thenReturn(List.of());

        Optional<List<PayrollCalculationStep>> result = steps();

        assertTrue(result.isPresent(), "el recibo existe, y eso no es un 404");
        assertEquals(List.of(), result.get(),
                "se calculo antes de la V129: no tiene pasos, y no se le inventan derivandolos del folio");
    }

    @Test
    void theStepsComeBackExactlyAsThePortServesThem_withTheSameConceptMoreThanOnce() {
        givenPayroll(9L);
        when(payrollCalculationStepReadPort.findStepsOf(9L)).thenReturn(List.of(
                periodStep(1, "D01"),
                segmentStep(2, "101", new BigDecimal("30.00")),
                segmentStep(3, "101", new BigDecimal("15.00"))));

        List<PayrollCalculationStep> result = steps().orElseThrow();

        assertEquals(List.of(1, 2, 3), result.stream().map(PayrollCalculationStep::executionOrder).toList(),
                "el orden que sirve el puerto es el de ejecucion y aqui no se reordena");
        assertEquals(2, result.stream().filter(s -> s.conceptCode().equals("101")).count(),
                "el mismo concepto puede salir dos veces: la clave de fila es el orden, no el concepto");
    }

    @Test
    void theBusinessKeyIsNormalizedWhereItAlreadyWas_andNotAgainHere() {
        givenPayroll(3L);
        when(payrollCalculationStepReadPort.findStepsOf(3L)).thenReturn(List.of());

        service.listByPayrollBusinessKey("esp", "internal", "EMP000001", "202609", "normal", 1);

        // Tal cual, sin recortar ni pasar a mayusculas por el camino: quien decide que direccion es
        // valida es GetPayrollByBusinessKeyUseCase, y tiene que haber un solo sitio que lo decida.
        verify(getPayrollByBusinessKeyUseCase)
                .getByBusinessKey("esp", "internal", "EMP000001", "202609", "normal", 1);
    }

    private void givenPayroll(long payrollId) {
        Payroll payroll = Payroll.rehydrate(
                payrollId, "ESP", "INTERNAL", "EMP000001", "202609", "NORMAL", 1,
                PayrollStatus.CALCULATED, null, LocalDateTime.of(2026, 9, 30, 12, 0),
                "ENGINE", "1.0", List.of(), List.of(), List.of(), null, null);
        when(getPayrollByBusinessKeyUseCase.getByBusinessKey(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Optional.of(payroll));
    }

    private Optional<List<PayrollCalculationStep>> steps() {
        return service.listByPayrollBusinessKey("ESP", "INTERNAL", "EMP000001", "202609", "NORMAL", 1);
    }

    private static PayrollCalculationStep periodStep(int executionOrder, String conceptCode) {
        return new PayrollCalculationStep(
                executionOrder, conceptCode, conceptCode + "_MNEMO", "DIRECT_AMOUNT", "TECHNICAL",
                PayrollCalculationStep.PERIOD_SCOPE, null, null,
                BigDecimal.ZERO, null, null, null);
    }

    private static PayrollCalculationStep segmentStep(int executionOrder, String conceptCode, BigDecimal rate) {
        return new PayrollCalculationStep(
                executionOrder, conceptCode, "SALARIO_BASE", "RATE_BY_QUANTITY", "EARNING",
                "SEGMENT",
                java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 15),
                new BigDecimal("450.00"), new BigDecimal("15"), rate, "101");
    }
}
