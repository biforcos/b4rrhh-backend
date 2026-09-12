package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceLookupPort;
import com.b4rrhh.payroll.application.port.PayrollLaunchEmployeeContext;
import com.b4rrhh.payroll.domain.model.CalculationClaim;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.domain.model.CalculationRunMessage;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.CalculationClaimRepository;
import com.b4rrhh.payroll.domain.port.CalculationRunMessageRepository;
import com.b4rrhh.payroll.domain.port.CalculationRunRepository;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    private LaunchPayrollCalculationService service;

    @BeforeEach
    void setUp() {
        service = new LaunchPayrollCalculationService(
                calculationRunRepository,
                calculationClaimRepository,
                calculationRunMessageRepository,
                payrollRepository,
                payrollLaunchPresenceLookupPort,
                calculatePayrollUnitUseCase,
                new ObjectMapper()
        );

        when(calculationRunRepository.save(any(CalculationRun.class))).thenAnswer(invocation -> {
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
        assertEquals(1, run.totalSkippedNotEligible());
        assertEquals(0, run.totalErrors());

        ArgumentCaptor<CalculationRunMessage> captor = ArgumentCaptor.forClass(CalculationRunMessage.class);
        verify(calculationRunMessageRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(m -> "UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT".equals(m.messageCode())));
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

    private LaunchPayrollCalculationCommand singleEmployeeCommand() {
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
                )
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
                )
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