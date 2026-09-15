package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.exception.PayrollCalculationFailedException;
import com.b4rrhh.payroll.domain.exception.PayrollNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollRecalculationNotAllowedException;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.domain.port.CalculationRunRepository;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll_engine.metamodel.domain.port.RuleSystemMetamodelRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecalculatePayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;
    @Mock
    private CalculatePayrollUnitUseCase calculatePayrollUnitUseCase;
    @Mock
    private RuleSystemMetamodelRepository ruleSystemMetamodelRepository;
    @Mock
    private CalculationRunRepository calculationRunRepository;

    private RecalculatePayrollService service;

    @BeforeEach
    void setUp() {
        service = new RecalculatePayrollService(
                payrollRepository, calculatePayrollUnitUseCase, ruleSystemMetamodelRepository,
                calculationRunRepository, new ObjectMapper());
        // lenient: los tests que se caen antes de calcular no llegan a abrir ejecucion.
        lenient().when(calculationRunRepository.save(any(CalculationRun.class))).thenAnswer(invocation -> {
            CalculationRun run = invocation.getArgument(0);
            return conId(run, run.id() == null ? 77L : run.id());
        });
    }

    @Test
    void delegatesToCalculateUnitWhenPayrollIsNotValid() {
        RecalculatePayrollCommand command = command("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1);
        Payroll notValidPayroll = payroll("MAS000001", "202604", PayrollStatus.NOT_VALID, "ENGINE_001", "1.0");
        Payroll recalculated = payroll("MAS000001", "202604", PayrollStatus.CALCULATED, "ENGINE_001", "1.0");

        when(payrollRepository.findByBusinessKey("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1))
                .thenReturn(Optional.of(notValidPayroll));
        when(calculatePayrollUnitUseCase.calculate(any())).thenReturn(recalculated);

        Payroll result = service.recalculate(command);

        assertEquals(PayrollStatus.CALCULATED, result.getStatus());

        ArgumentCaptor<CalculatePayrollUnitCommand> captor = ArgumentCaptor.forClass(CalculatePayrollUnitCommand.class);
        verify(calculatePayrollUnitUseCase).calculate(captor.capture());
        CalculatePayrollUnitCommand sent = captor.getValue();
        assertEquals("MAS000001", sent.employeeNumber());
        assertEquals("202604", sent.payrollPeriodCode());
        assertEquals("ENGINE_001", sent.calculationEngineCode());
        assertEquals("1.0", sent.calculationEngineVersion());
    }

    @Test
    void throwsWhenPayrollNotFound() {
        when(payrollRepository.findByBusinessKey(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(PayrollNotFoundException.class, () ->
                service.recalculate(command("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1)));
    }

    @Test
    void throwsWhenPayrollIsNotInNotValidState() {
        when(payrollRepository.findByBusinessKey("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1))
                .thenReturn(Optional.of(payroll("MAS000001", "202604", PayrollStatus.CALCULATED, "ENGINE_001", "1.0")));

        assertThrows(PayrollRecalculationNotAllowedException.class, () ->
                service.recalculate(command("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1)));
    }

    @Test
    void derivesPeriodDatesFrom6DigitPeriodCode() {
        when(payrollRepository.findByBusinessKey(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(payroll("MAS000001", "202604", PayrollStatus.NOT_VALID, "ENG", "1")));
        when(calculatePayrollUnitUseCase.calculate(any())).thenReturn(
                payroll("MAS000001", "202604", PayrollStatus.CALCULATED, "ENG", "1"));

        service.recalculate(command("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1));

        ArgumentCaptor<CalculatePayrollUnitCommand> captor = ArgumentCaptor.forClass(CalculatePayrollUnitCommand.class);
        verify(calculatePayrollUnitUseCase).calculate(captor.capture());
        assertEquals(1, captor.getValue().periodStart().getDayOfMonth());
        assertEquals(4, captor.getValue().periodStart().getMonthValue());
        assertEquals(2026, captor.getValue().periodStart().getYear());
        assertEquals(30, captor.getValue().periodEnd().getDayOfMonth());
    }

    private RecalculatePayrollCommand command(String rsc, String etc, String en, String ppc, String ptc, int pn) {
        return new RecalculatePayrollCommand(rsc, etc, en, ppc, ptc, pn, "claude");
    }


    /**
     * Un recalculo es una ejecucion, de una unidad (#99). El paso 2 del camino ato el recibo a la
     * suya; el 3 hizo del recalculo el gesto principal, y el recibo nuevo nacia sin ella. Lo que
     * este test sujeta es que el {@code runId} llegue <b>dentro</b> del encargo de la unidad: si se
     * anotara despues, el recibo existiria un instante sin saber de donde viene, y es en ese
     * instante en el que se le ponen los pasos y los conceptos.
     */
    @Test
    void opensItsOwnRunAndCalculatesInsideIt() {
        when(payrollRepository.findByBusinessKey(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(payroll("MAS000001", "202604", PayrollStatus.NOT_VALID, "ENG", "1")));
        when(calculatePayrollUnitUseCase.calculate(any())).thenReturn(
                payroll("MAS000001", "202604", PayrollStatus.CALCULATED, "ENG", "1"));

        service.recalculate(command("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1));

        ArgumentCaptor<CalculatePayrollUnitCommand> unidad = ArgumentCaptor.forClass(CalculatePayrollUnitCommand.class);
        verify(calculatePayrollUnitUseCase).calculate(unidad.capture());
        assertEquals(77L, unidad.getValue().runId(),
                "el recibo nuevo tiene que nacer con su ejecucion dentro, no recibirla despues");
    }

    /** Y la ejecucion dice que es de un recalculo, y de que unidad: SINGLE_EMPLOYEE no lo diria. */
    @Test
    void theRunSaysWhichSingleUnitItRecalculated() {
        when(payrollRepository.findByBusinessKey(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(payroll("MAS000001", "202604", PayrollStatus.NOT_VALID, "ENG", "1")));
        when(calculatePayrollUnitUseCase.calculate(any())).thenReturn(
                payroll("MAS000001", "202604", PayrollStatus.CALCULATED, "ENG", "1"));

        service.recalculate(command("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1));

        CalculationRun abierta = ejecucionesGuardadas().getFirst();
        assertEquals("RUNNING", abierta.status());
        assertEquals("claude", abierta.requestedBy());
        assertEquals(1, abierta.totalCandidates());
        assertEquals(1, abierta.totalEligible());
        assertTrue(abierta.targetSelectionJson().contains("SINGLE_CALCULATION_UNIT"));
        assertTrue(abierta.targetSelectionJson().contains("MAS000001"));
        assertTrue(abierta.targetSelectionJson().contains("presenceNumber"));
    }

    /** Se cierra COMPLETED y con la unidad contada donde va segun como acabo. */
    @Test
    void closesTheRunCountingHowTheUnitEnded() {
        when(payrollRepository.findByBusinessKey(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(payroll("MAS000001", "202604", PayrollStatus.NOT_VALID, "ENG", "1")));
        when(calculatePayrollUnitUseCase.calculate(any())).thenReturn(
                payroll("MAS000001", "202604", PayrollStatus.CALCULATED, "ENG", "1"));

        service.recalculate(command("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1));

        CalculationRun cerrada = ejecucionesGuardadas().getLast();
        assertEquals("COMPLETED", cerrada.status());
        assertEquals(1, cerrada.totalCalculated());
        assertEquals(0, cerrada.totalNotValid());
        assertNotNull(cerrada.finishedAt());
    }

    /**
     * Y si el motor devuelve el recibo invalido, la ejecucion lo cuenta como tal. Es la
     * distincion del backend#85: totalNotValid es el desenlace, no el intento, y meterlo en
     * totalCalculated diria que salio bien.
     */
    @Test
    void countsANotValidOutcomeAsNotValidAndNotAsCalculated() {
        when(payrollRepository.findByBusinessKey(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(payroll("MAS000001", "202604", PayrollStatus.NOT_VALID, "ENG", "1")));
        when(calculatePayrollUnitUseCase.calculate(any())).thenReturn(
                payroll("MAS000001", "202604", PayrollStatus.NOT_VALID, "ENG", "1"));

        service.recalculate(command("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1));

        CalculationRun cerrada = ejecucionesGuardadas().getLast();
        assertEquals(0, cerrada.totalCalculated());
        assertEquals(1, cerrada.totalNotValid());
    }

    /**
     * Un recalculo que no se puede hacer no deja ejecucion abierta a medias: la unica que se
     * guardo es la de apertura, y la transaccion del recalculo se la lleva. Lo que este test puede
     * afirmar sin base de datos es que <b>no se cierra</b> ninguna.
     */
    @Test
    void doesNotCloseARunWhenTheCalculationFails() {
        when(payrollRepository.findByBusinessKey(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(payroll("MAS000001", "202604", PayrollStatus.NOT_VALID, "ENG", "1")));
        when(calculatePayrollUnitUseCase.calculate(any()))
                .thenThrow(new IllegalStateException("Configuration error: No table binding found"));

        assertThrows(PayrollCalculationFailedException.class, () ->
                service.recalculate(command("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1)));

        assertTrue(ejecucionesGuardadas().stream().noneMatch(r -> "COMPLETED".equals(r.status())),
                "no se cierra una ejecucion que no produjo recibo");
    }

    private List<CalculationRun> ejecucionesGuardadas() {
        ArgumentCaptor<CalculationRun> captor = ArgumentCaptor.forClass(CalculationRun.class);
        verify(calculationRunRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private static CalculationRun conId(CalculationRun run, Long id) {
        return new CalculationRun(
                id, run.ruleSystemCode(), run.payrollPeriodCode(), run.payrollTypeCode(),
                run.calculationEngineCode(), run.calculationEngineVersion(), run.requestedAt(),
                run.requestedBy(), run.status(), run.targetSelectionJson(),
                run.totalCandidates(), run.totalEligible(), run.totalClaimed(),
                run.totalSkippedNotEligible(), run.totalSkippedAlreadyClaimed(),
                run.totalSkippedMissingInput(), run.totalCalculated(), run.totalNotValid(),
                run.totalErrors(), run.startedAt(), run.finishedAt(), run.summaryJson(),
                run.createdAt(), run.updatedAt());
    }

    private Payroll payroll(String employeeNumber, String periodCode, PayrollStatus status, String engCode, String engVer) {
        // id, ruleSystemCode, employeeTypeCode, employeeNumber, periodCode, payrollTypeCode, presenceNumber,
        // status, statusReasonCode (null=none), calculatedAt, engCode, engVer, warnings, concepts, contextSnapshots, createdAt, updatedAt
        return Payroll.rehydrate(
                1L, "MAS", "EMP", employeeNumber, periodCode, "NORMAL", 1,
                status, null, LocalDateTime.now(), engCode, engVer,
                List.of(), List.of(), List.of(),
                LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
