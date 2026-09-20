package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceLookupPort;
import com.b4rrhh.payroll.application.port.PayrollLaunchEmployeeContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchWorkerPort;
import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.payroll.domain.model.CalculationClaim;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.domain.model.CalculationRunMessage;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.CalculationClaimRepository;
import com.b4rrhh.payroll.domain.port.CalculationRunMessageRepository;
import com.b4rrhh.payroll.domain.port.CalculationRunRepository;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import com.b4rrhh.payroll_engine.metamodel.domain.port.RuleSystemMetamodelRepository;
import com.b4rrhh.payroll_engine.planning.application.service.DefaultEligibleConceptExpansionService;
import com.b4rrhh.payroll_engine.planning.application.service.UnreachableConceptFinder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

import static com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodelFixtures.metamodel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaunchPayrollCalculationServiceTest {

    @Mock
    private CalculationRunRepository calculationRunRepository;
    @Mock
    private CalculationClaimRepository calculationClaimRepository;
    @Mock
    private CalculationRunMessageRepository calculationRunMessageRepository;
    @Mock
    private PayrollRepository payrollRepository;
    @Mock
    private PayrollLaunchPresenceLookupPort payrollLaunchPresenceLookupPort;
    @Mock
    private CalculatePayrollUnitUseCase calculatePayrollUnitUseCase;
    @Mock
    private RuleSystemMetamodelRepository ruleSystemMetamodelRepository;

    private LaunchPayrollCalculationService service;
    private RecordingWorker worker;

    @BeforeEach
    void setUp() {
        worker = new RecordingWorker();
        service = new LaunchPayrollCalculationService(
                calculationRunRepository,
                calculationClaimRepository,
                calculationRunMessageRepository,
                payrollRepository,
                payrollLaunchPresenceLookupPort,
                calculatePayrollUnitUseCase,
                worker,
                ruleSystemMetamodelRepository,
                // El de verdad y no un doble: no tiene estado, su respuesta sale del metamodelo
                // que este test ya construye, y para una reglamentacion sin conceptos es la lista
                // vacia. Un mock aqui solo anadiria una linea que mantener (backend#110).
                new UnreachableConceptFinder(new DefaultEligibleConceptExpansionService()),
                new ObjectMapper()
        );

        // La ejecucion lee su reglamentacion al empezar. Lo que se prueba aqui es la cola y
        // los contadores, no el motor, asi que basta con que la carga conteste.
        lenient().when(ruleSystemMetamodelRepository.load(any(), any()))
                .thenReturn(metamodel("ESP", LocalDate.of(2025, 1, 31)).build());

        // lenient: un lanzamiento que se cae en la validacion no llega a guardar nada, y
        // este eco del save lo comparten todos los demas tests de la clase.
        lenient().when(calculationRunRepository.save(any(CalculationRun.class))).thenAnswer(invocation -> {
            CalculationRun run = invocation.getArgument(0);
            LocalDateTime now = LocalDateTime.of(2026, 4, 11, 10, 0);
            return new CalculationRun(
                    run.id() == null ? 1L : run.id(),
                    run.ruleSystemCode(),
                    run.payrollPeriodCode(),
                    run.payrollTypeCode(),
                    run.calculationEngineCode(),
                    run.calculationEngineVersion(),
                    run.requestedAt(),
                    run.requestedBy(),
                    run.status(),
                    run.targetSelectionJson(),
                    run.totalCandidates(),
                    run.totalEligible(),
                    run.totalClaimed(),
                    run.totalSkippedNotEligible(),
                    run.totalSkippedAlreadyClaimed(),
                    run.totalSkippedMissingInput(),
                    run.totalCalculated(),
                    run.totalNotValid(),
                    run.totalErrors(),
                    run.startedAt(),
                    run.finishedAt(),
                    run.summaryJson(),
                    run.createdAt() == null ? now : run.createdAt(),
                    now
            );
        });
    }

    @Test
    void launchHappyPathCreatesRunCalculatesEligibleUnitsAndCompletes() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.empty());
        when(calculationClaimRepository.save(any(CalculationClaim.class)))
                .thenReturn(new CalculationClaim(9L, 1L, "ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1, LocalDateTime.now(), null));
        when(calculatePayrollUnitUseCase.calculate(any(CalculatePayrollUnitCommand.class)))
                .thenReturn(payroll(PayrollStatus.CALCULATED));

        CalculationRun run = service.launch(singleEmployeeCommand());

        assertEquals(CalculationRunStatuses.COMPLETED, run.status());
        assertEquals(1, run.totalCandidates());
        assertEquals(1, run.totalEligible());
        assertEquals(1, run.totalClaimed());
        assertEquals(1, run.totalCalculated());
        assertEquals(0, run.totalErrors());
                verify(calculationClaimRepository).deleteById(9L);
                verify(calculationClaimRepository).deleteByRunId(1L);
    }

    @Test
    void launchSkipsUnitsThatAreNotEligible() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll(PayrollStatus.CALCULATED)));

        CalculationRun run = service.launch(singleEmployeeCommand());

        assertEquals(1, run.totalSkippedNotEligible());
        assertEquals(0, run.totalSkippedMissingInput());
        assertEquals(0, run.totalEligible());
        verify(calculatePayrollUnitUseCase, never()).calculate(any(CalculatePayrollUnitCommand.class));
        verify(calculationRunMessageRepository).save(any(CalculationRunMessage.class));
    }

    @Test
    void launchSkipsUnitsAlreadyClaimed() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.empty());
        when(calculationClaimRepository.save(any(CalculationClaim.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate claim"));

        CalculationRun run = service.launch(singleEmployeeCommand());

        assertEquals(1, run.totalEligible());
        assertEquals(1, run.totalSkippedAlreadyClaimed());
        assertEquals(0, run.totalClaimed());
        verify(calculationRunMessageRepository).save(any(CalculationRunMessage.class));
    }

    @Test
    void launchTreatsMissingPayrollAsEligibleAndCalculates() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.empty());
        when(calculationClaimRepository.save(any(CalculationClaim.class)))
                .thenReturn(new CalculationClaim(11L, 1L, "ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1, LocalDateTime.now(), null));
        when(calculatePayrollUnitUseCase.calculate(any(CalculatePayrollUnitCommand.class)))
                .thenReturn(payroll(PayrollStatus.CALCULATED));

        CalculationRun run = service.launch(singleEmployeeCommand());

        assertEquals(1, run.totalCalculated());
        verify(calculatePayrollUnitUseCase).calculate(any(CalculatePayrollUnitCommand.class));
    }

    @Test
    void launchAllowsRecalculationWhenExistingPayrollIsNotValid() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll(PayrollStatus.NOT_VALID)));
        when(calculationClaimRepository.save(any(CalculationClaim.class)))
                .thenReturn(new CalculationClaim(12L, 1L, "ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1, LocalDateTime.now(), null));
        when(calculatePayrollUnitUseCase.calculate(any(CalculatePayrollUnitCommand.class)))
                .thenReturn(payroll(PayrollStatus.CALCULATED));

        CalculationRun run = service.launch(singleEmployeeCommand());

        assertEquals(1, run.totalEligible());
        assertEquals(1, run.totalCalculated());
        verify(calculatePayrollUnitUseCase).calculate(any(CalculatePayrollUnitCommand.class));
    }

    @Test
    void recalculationStampsTheRunningExecutionAndNotTheOneThatProducedThePreviousPayroll() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll(PayrollStatus.NOT_VALID, 41L)));
        when(calculationClaimRepository.save(any(CalculationClaim.class)))
                .thenReturn(new CalculationClaim(14L, 1L, "ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1, LocalDateTime.now(), null));
        when(calculatePayrollUnitUseCase.calculate(any(CalculatePayrollUnitCommand.class)))
                .thenReturn(payroll(PayrollStatus.CALCULATED, 1L));

        CalculationRun run = service.launch(singleEmployeeCommand());

        ArgumentCaptor<CalculatePayrollUnitCommand> captor = ArgumentCaptor.forClass(CalculatePayrollUnitCommand.class);
        verify(calculatePayrollUnitUseCase).calculate(captor.capture());
        assertEquals(run.id(), captor.getValue().runId());
        assertEquals(1L, captor.getValue().runId());
    }

    @Test
    void launchPersistsRunMessageAndFinalizesWithErrorsWhenUnitCalculationFails() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.empty());
        when(calculationClaimRepository.save(any(CalculationClaim.class)))
                .thenReturn(new CalculationClaim(13L, 1L, "ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1, LocalDateTime.now(), null));
        when(calculatePayrollUnitUseCase.calculate(any(CalculatePayrollUnitCommand.class)))
                .thenThrow(new IllegalStateException("boom"));

        CalculationRun run = service.launch(singleEmployeeCommand());

        assertEquals(CalculationRunStatuses.COMPLETED_WITH_ERRORS, run.status());
        assertEquals(1, run.totalErrors());
        verify(calculationRunMessageRepository).save(any(CalculationRunMessage.class));
        verify(calculationClaimRepository).deleteById(13L);
    }

    @Test
    void launchSkipsUnitExplicitlyWhenEligibleRealInputIsMissing() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.empty());
        when(calculationClaimRepository.save(any(CalculationClaim.class)))
                .thenReturn(new CalculationClaim(31L, 1L, "ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1, LocalDateTime.now(), null));
        when(calculatePayrollUnitUseCase.calculate(any(CalculatePayrollUnitCommand.class)))
                .thenThrow(new PayrollLaunchInputMissingException(
                        "MONTHLY_SALARY_NOT_CONFIGURED",
                        "Eligible real execution skipped: monthly salary is not configured",
                        java.util.Map.of("executionMode", "ELIGIBLE_REAL")
                ));

        CalculationRun run = service.launch(singleEmployeeCommand());

        assertEquals(CalculationRunStatuses.COMPLETED, run.status());
        // backend#85: sumaba a totalSkippedNotEligible, que es la lectura contraria.
        assertEquals(1, run.totalSkippedMissingInput());
        assertEquals(0, run.totalSkippedNotEligible());
        // Era elegible, y lo sigue siendo: totalEligible las incluye, y esta documentado.
        assertEquals(1, run.totalEligible());
        assertEquals(1, run.totalClaimed());
        assertEquals(0, run.totalErrors());

        ArgumentCaptor<CalculationRunMessage> captor = ArgumentCaptor.forClass(CalculationRunMessage.class);
        verify(calculationRunMessageRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(m -> "UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT".equals(m.messageCode())));
    }

    // El criterio 1 del backend#85: una ejecucion que salta por las DOS razones las ensena
    // separadas. Antes las dos sumaban al mismo contador y la pantalla decia «2 saltadas por
    // falta de datos» cuando una de ellas ya tenia recibo — la lectura contraria.
    @Test
    void launchCountsTheTwoKindsOfSkipApart() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 1)));
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP002"), any(), any()))
                .thenReturn(List.of(new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP002", 1)));
        // EMP001 ya tiene recibo inmutable: no elegible, y no pide nada de nadie.
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll(PayrollStatus.CALCULATED)));
        // EMP002 era elegible y le faltan datos: eso si pide que alguien mire.
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP002", "202501", "NORMAL", 1))
                .thenReturn(Optional.empty());
        when(calculationClaimRepository.save(any(CalculationClaim.class)))
                .thenReturn(new CalculationClaim(41L, 1L, "ESP", "INTERNAL", "EMP002", "202501", "NORMAL", 1, LocalDateTime.now(), null));
        when(calculatePayrollUnitUseCase.calculate(any(CalculatePayrollUnitCommand.class)))
                .thenThrow(new PayrollLaunchInputMissingException(
                        "MONTHLY_SALARY_NOT_CONFIGURED",
                        "Eligible real execution skipped: monthly salary is not configured",
                        java.util.Map.of("executionMode", "ELIGIBLE_REAL")
                ));

        CalculationRun run = service.launch(employeeListCommand("EMP001", "EMP002"));

        assertEquals(2, run.totalCandidates());
        assertEquals(1, run.totalSkippedNotEligible());
        assertEquals(1, run.totalSkippedMissingInput());
        // La particion cuadra: 2 candidatas = 1 + 0 + 1 + 0 + 0 + 0. totalEligible no entra en
        // la suma, es una etapa, y vale 1 porque EMP002 si era elegible.
        assertEquals(1, run.totalEligible());
        assertEquals(
                run.totalCandidates(),
                run.totalSkippedNotEligible() + run.totalSkippedAlreadyClaimed()
                        + run.totalSkippedMissingInput() + run.totalCalculated()
                        + run.totalNotValid() + run.totalErrors());
    }

    @Test
    void launchCountsCandidatesAsExpandedPresenceBasedUnits() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(
                        new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 1),
                        new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 2)
                ));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.empty());
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 2))
                .thenReturn(Optional.empty());
        when(calculationClaimRepository.save(any(CalculationClaim.class)))
                .thenReturn(
                        new CalculationClaim(14L, 1L, "ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1, LocalDateTime.now(), null),
                        new CalculationClaim(15L, 1L, "ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 2, LocalDateTime.now(), null)
                );
        when(calculatePayrollUnitUseCase.calculate(any(CalculatePayrollUnitCommand.class)))
                .thenReturn(payroll(PayrollStatus.CALCULATED));

        CalculationRun run = service.launch(singleEmployeeCommand());

        assertEquals(2, run.totalCandidates());
        assertEquals(2, run.totalCalculated());
    }

    @Test
    void launchPersistsTargetSelectionAsStructuredJson() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of());

        service.launch(singleEmployeeCommand());

        ArgumentCaptor<CalculationRun> captor = ArgumentCaptor.forClass(CalculationRun.class);
        verify(calculationRunRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().getFirst().targetSelectionJson().contains("SINGLE_EMPLOYEE"));
        assertEquals(0, captor.getAllValues().get(2).totalCandidates());
    }

    @Test
    void launchInvokesBestEffortDeleteByRunIdOnControlledGlobalFailure() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenThrow(new IllegalStateException("lookup failed"));

        CalculationRun run = service.launch(singleEmployeeCommand());

        assertEquals(CalculationRunStatuses.FAILED, run.status());
        assertEquals(1, run.totalErrors());
        verify(calculationClaimRepository).deleteByRunId(1L);
    }

    @Test
    void launchKeepsNoRelevantPresenceMessageAndZeroPresenceCandidates() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of());

        CalculationRun run = service.launch(singleEmployeeCommand());

        assertEquals(0, run.totalCandidates());
        verify(calculationRunMessageRepository).save(any(CalculationRunMessage.class));
        verify(calculationClaimRepository).deleteByRunId(1L);
    }

    @Test
    void launchAllEmployeesWithPresenceInPeriodResolvesPopulationAndKeepsPresenceBasedCandidates() {
        when(payrollLaunchPresenceLookupPort.findEmployeesWithPresenceInPeriod(eq("ESP"), any(), any()))
                .thenReturn(List.of(
                        new PayrollLaunchEmployeeContext("INTERNAL", "EMP001"),
                        new PayrollLaunchEmployeeContext("INTERNAL", "EMP002")
                ));
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(
                        new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 1),
                        new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 2)
                ));
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP002"), any(), any()))
                .thenReturn(List.of(new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP002", 1)));
        when(payrollRepository.findByBusinessKey(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Optional.empty());
        when(calculationClaimRepository.save(any(CalculationClaim.class)))
                .thenReturn(
                        new CalculationClaim(21L, 1L, "ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1, LocalDateTime.now(), null),
                        new CalculationClaim(22L, 1L, "ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 2, LocalDateTime.now(), null),
                        new CalculationClaim(23L, 1L, "ESP", "INTERNAL", "EMP002", "202501", "NORMAL", 1, LocalDateTime.now(), null)
                );
        when(calculatePayrollUnitUseCase.calculate(any(CalculatePayrollUnitCommand.class)))
                .thenReturn(payroll(PayrollStatus.CALCULATED));

        CalculationRun run = service.launch(allEmployeesWithPresenceCommand());

        assertEquals(CalculationRunStatuses.COMPLETED, run.status());
        assertEquals(3, run.totalCandidates());
        assertEquals(3, run.totalCalculated());
        verify(payrollLaunchPresenceLookupPort).findEmployeesWithPresenceInPeriod(eq("ESP"), any(), any());
    }

    @Test
    void launchAllEmployeesWithPresenceInPeriodExcludesEmployeesOutsideResolvedPopulation() {
        when(payrollLaunchPresenceLookupPort.findEmployeesWithPresenceInPeriod(eq("ESP"), any(), any()))
                .thenReturn(List.of(new PayrollLaunchEmployeeContext("INTERNAL", "EMP001")));
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.empty());
        when(calculationClaimRepository.save(any(CalculationClaim.class)))
                .thenReturn(new CalculationClaim(24L, 1L, "ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1, LocalDateTime.now(), null));
        when(calculatePayrollUnitUseCase.calculate(any(CalculatePayrollUnitCommand.class)))
                .thenReturn(payroll(PayrollStatus.CALCULATED));

        CalculationRun run = service.launch(allEmployeesWithPresenceCommand());

        assertEquals(1, run.totalCandidates());
        verify(payrollLaunchPresenceLookupPort).findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any());
        verify(payrollLaunchPresenceLookupPort, never()).findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP999"), any(), any());
    }

    @Test
    void launchStampsWhoRequestedTheRunOnTheVeryFirstSave() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of());

        CalculationRun run = service.launch(singleEmployeeCommand("hr.manager@b4rrhh"));

        assertEquals("hr.manager@b4rrhh", run.requestedBy());

        ArgumentCaptor<CalculationRun> captor = ArgumentCaptor.forClass(CalculationRun.class);
        verify(calculationRunRepository, atLeastOnce()).save(captor.capture());
        // La primera fila que se escribe ya lleva quien pidio la ejecucion: es la unica
        // que el cliente ve cuando el lanzamiento deja de esperar (#75).
        assertEquals("hr.manager@b4rrhh", captor.getAllValues().getFirst().requestedBy());
    }

    @Test
    void launchLeavesRequestedByEmptyWhenNobodyIsBehindTheLaunch() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of());

        CalculationRun run = service.launch(singleEmployeeCommand("   "));

        assertNull(run.requestedBy());
    }

    @Test
    void launchRejectsRequestedByThatDoesNotFitTheColumn() {
        assertThrows(
                InvalidPayrollArgumentException.class,
                () -> service.launch(singleEmployeeCommand("x".repeat(101)))
        );
    }

    @Test
    void requestLaunchHandsBackTheRunInRequestedWithoutDoingTheWork() {
        CalculationRun run = service.requestLaunch(singleEmployeeCommand("hr.manager@b4rrhh"));

        assertEquals(CalculationRunStatuses.REQUESTED, run.status());
        assertEquals(1L, run.id());
        assertEquals("hr.manager@b4rrhh", run.requestedBy());
        assertNull(run.startedAt());
        assertEquals(1, worker.submitted.size());
        // Nadie ha mirado presencias todavia: el trabajo esta encolado, no hecho.
        verifyNoInteractions(payrollLaunchPresenceLookupPort, calculatePayrollUnitUseCase);
    }

    @Test
    void theWorkerRunsTheSameWorkAndCompletesTheRunItWasGiven() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.empty());
        when(calculationClaimRepository.save(any(CalculationClaim.class)))
                .thenReturn(new CalculationClaim(31L, 1L, "ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1, LocalDateTime.now(), null));
        when(calculatePayrollUnitUseCase.calculate(any(CalculatePayrollUnitCommand.class)))
                .thenReturn(payroll(PayrollStatus.CALCULATED));

        service.requestLaunch(singleEmployeeCommand());
        worker.runAll();

        ArgumentCaptor<CalculationRun> captor = ArgumentCaptor.forClass(CalculationRun.class);
        verify(calculationRunRepository, atLeastOnce()).save(captor.capture());
        CalculationRun lastSaved = captor.getAllValues().getLast();
        assertEquals(CalculationRunStatuses.COMPLETED, lastSaved.status());
        assertEquals(1, lastSaved.totalCalculated());
        verify(calculationClaimRepository).deleteById(31L);
        verify(calculationClaimRepository).deleteByRunId(1L);
    }

    @Test
    void requestLaunchClosesTheRunWhenTheQueueHasNoRoom() {
        worker.rejectEverything = true;

        CalculationRun run = service.requestLaunch(singleEmployeeCommand());

        assertEquals(CalculationRunStatuses.FAILED, run.status());
        // finished_at no se admite sin started_at (V55), y una ejecucion rechazada no
        // arranco nunca: se cierra con el instante en que se pidio.
        assertEquals(run.requestedAt(), run.startedAt());
        assertNotNull(run.finishedAt());

        ArgumentCaptor<CalculationRunMessage> messages = ArgumentCaptor.forClass(CalculationRunMessage.class);
        verify(calculationRunMessageRepository).save(messages.capture());
        assertEquals("LAUNCH_REJECTED", messages.getValue().messageCode());
        assertEquals("ERROR", messages.getValue().severityCode());
    }

    /**
     * Un worker de mentira: guarda lo que se le encola y solo lo ejecuta cuando el test se
     * lo pide. Asi el test ve las dos mitades —aceptar y trabajar— por separado.
     */
    private static final class RecordingWorker implements PayrollLaunchWorkerPort {

        private final List<Runnable> submitted = new ArrayList<>();
        private boolean rejectEverything;

        @Override
        public void submit(Runnable work) {
            if (rejectEverything) {
                throw new RejectedExecutionException("queue is full");
            }
            submitted.add(work);
        }

        void runAll() {
            List.copyOf(submitted).forEach(Runnable::run);
        }
    }

    // ── La reglamentacion se lee una vez por ejecucion (backend#87) ──────────

    /**
     * Tres unidades, una sola lectura del metamodelo, y la misma instancia para las tres.
     *
     * <p>Es el criterio del issue: el numero de lecturas a la reglamentacion no depende del
     * numero de unidades. Y no es solo cuentas — que las tres reciban <b>el mismo objeto</b>
     * es lo que garantiza que las tres calculan con las mismas reglas.
     */
    @Test
    void laReglamentacionSeLeeUnaVezPorEjecucionYNoUnaVezPorUnidad() {
        for (String numero : List.of("EMP001", "EMP002", "EMP003")) {
            when(payrollLaunchPresenceLookupPort.findRelevantPresences(
                    eq("ESP"), eq("INTERNAL"), eq(numero), any(), any()))
                    .thenReturn(List.of(new PayrollLaunchPresenceContext("ESP", "INTERNAL", numero, 1)));
            when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", numero, "202501", "NORMAL", 1))
                    .thenReturn(Optional.empty());
        }
        when(calculationClaimRepository.save(any(CalculationClaim.class)))
                .thenReturn(new CalculationClaim(21L, 1L, "ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1, LocalDateTime.now(), null));
        when(calculatePayrollUnitUseCase.calculate(any(CalculatePayrollUnitCommand.class)))
                .thenReturn(payroll(PayrollStatus.CALCULATED));

        CalculationRun run = service.launch(employeeListCommand("EMP001", "EMP002", "EMP003"));

        assertEquals(3, run.totalCalculated());
        verify(ruleSystemMetamodelRepository, times(1)).load(eq("ESP"), any());

        ArgumentCaptor<CalculatePayrollUnitCommand> captor =
                ArgumentCaptor.forClass(CalculatePayrollUnitCommand.class);
        verify(calculatePayrollUnitUseCase, times(3)).calculate(captor.capture());
        RuleSystemMetamodel primero = captor.getAllValues().getFirst().metamodel();
        assertNotNull(primero);
        for (CalculatePayrollUnitCommand enviado : captor.getAllValues()) {
            assertSame(primero, enviado.metamodel(),
                    "todas las unidades de una ejecucion calculan contra la misma reglamentacion");
        }
    }

    /**
     * Lo cargado vive lo que dura la ejecucion y no mas: la siguiente vuelve a leer, y por
     * eso ve los cambios del grafo que haya habido en medio. No es una cache del proceso.
     */
    @Test
    void cadaEjecucionLeeSuPropiaReglamentacion() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(new PayrollLaunchPresenceContext("ESP", "INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.empty());
        when(calculationClaimRepository.save(any(CalculationClaim.class)))
                .thenReturn(new CalculationClaim(22L, 1L, "ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1, LocalDateTime.now(), null));
        when(calculatePayrollUnitUseCase.calculate(any(CalculatePayrollUnitCommand.class)))
                .thenReturn(payroll(PayrollStatus.CALCULATED));

        service.launch(singleEmployeeCommand());
        service.launch(singleEmployeeCommand());

        verify(ruleSystemMetamodelRepository, times(2)).load(eq("ESP"), any());
    }

    /** La fecha con la que se carga es el fin del periodo de la ejecucion, no hoy. */
    @Test
    void laReglamentacionSeLeeAlCierreDelPeriodoDeLaEjecucion() {
        service.launch(allEmployeesWithPresenceCommand());

        verify(ruleSystemMetamodelRepository).load("ESP", LocalDate.of(2025, 1, 31));
    }

    private LaunchPayrollCalculationCommand employeeListCommand(String... employeeNumbers) {
        return new LaunchPayrollCalculationCommand(
                "ESP",
                "202501",
                "NORMAL",
                "ENGINE",
                "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.EMPLOYEE_LIST,
                        null,
                        java.util.Arrays.stream(employeeNumbers)
                                .map(numero -> new PayrollLaunchEmployeeTarget("INTERNAL", numero))
                                .toList()
                ),
                null
        );
    }

    private LaunchPayrollCalculationCommand singleEmployeeCommand() {
        return singleEmployeeCommand(null);
    }

    private LaunchPayrollCalculationCommand singleEmployeeCommand(String requestedBy) {
        return new LaunchPayrollCalculationCommand(
                "ESP",
                "202501",
                "NORMAL",
                "ENGINE",
                "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget("INTERNAL", "EMP001"),
                        null
                ),
                requestedBy
        );
    }

    private LaunchPayrollCalculationCommand allEmployeesWithPresenceCommand() {
        return new LaunchPayrollCalculationCommand(
                "ESP",
                "202501",
                "NORMAL",
                "ENGINE",
                "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD,
                        null,
                        null
                ),
                null
        );
    }

    private Payroll payroll(PayrollStatus status) {
        return payroll(status, null);
    }

    private Payroll payroll(PayrollStatus status, Long runId) {
        return Payroll.rehydrate(
                7L,
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                1,
                status,
                null,
                LocalDateTime.of(2026, 1, 31, 10, 15),
                "ENGINE",
                "1.0",
                runId,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}