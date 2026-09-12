package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.domain.model.CalculationRunMessage;
import com.b4rrhh.payroll.domain.port.CalculationClaimRepository;
import com.b4rrhh.payroll.domain.port.CalculationRunMessageRepository;
import com.b4rrhh.payroll.domain.port.CalculationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoverAbandonedPayrollCalculationRunsServiceTest {

    @Mock
    private CalculationRunRepository calculationRunRepository;
    @Mock
    private CalculationRunMessageRepository calculationRunMessageRepository;
    @Mock
    private CalculationClaimRepository calculationClaimRepository;

    private RecoverAbandonedPayrollCalculationRunsService service;

    @BeforeEach
    void setUp() {
        service = new RecoverAbandonedPayrollCalculationRunsService(
                calculationRunRepository,
                calculationRunMessageRepository,
                calculationClaimRepository
        );
    }

    @Test
    void closesTheRunThatDiedCalculatingAndSaysWhy() {
        when(calculationRunRepository.findByStatusIn(anyCollection()))
                .thenReturn(List.of(runningRun()));

        RecoveredPayrollLaunchState recovered = service.recover();

        assertEquals(1, recovered.closedRuns());

        ArgumentCaptor<CalculationRun> runs = ArgumentCaptor.forClass(CalculationRun.class);
        verify(calculationRunRepository).save(runs.capture());
        assertEquals(CalculationRunStatuses.FAILED, runs.getValue().status());
        assertNotNull(runs.getValue().finishedAt());
        // Los contadores no se tocan: dicen cuanto se habia hecho antes del corte.
        assertEquals(7, runs.getValue().totalCalculated());

        ArgumentCaptor<CalculationRunMessage> messages = ArgumentCaptor.forClass(CalculationRunMessage.class);
        verify(calculationRunMessageRepository).save(messages.capture());
        assertEquals("RUN_ABANDONED_ON_RESTART", messages.getValue().messageCode());
        assertEquals("ERROR", messages.getValue().severityCode());
    }

    @Test
    void closesTheRunThatDiedWaitingInTheQueueEvenThoughItNeverStarted() {
        when(calculationRunRepository.findByStatusIn(anyCollection()))
                .thenReturn(List.of(requestedRun()));

        service.recover();

        ArgumentCaptor<CalculationRun> runs = ArgumentCaptor.forClass(CalculationRun.class);
        verify(calculationRunRepository).save(runs.capture());
        assertEquals(CalculationRunStatuses.FAILED, runs.getValue().status());
        // El esquema no admite fin sin inicio (V55): una ejecucion que murio en la cola se
        // cierra con el instante en que se pidio.
        assertEquals(runs.getValue().requestedAt(), runs.getValue().startedAt());
        assertNotNull(runs.getValue().finishedAt());
    }

    @Test
    void clearsOrphanClaimsEvenWhenEveryRunHadFinished() {
        when(calculationRunRepository.findByStatusIn(anyCollection())).thenReturn(List.of());
        when(calculationClaimRepository.deleteAll()).thenReturn(3L);

        RecoveredPayrollLaunchState recovered = service.recover();

        assertEquals(0, recovered.closedRuns());
        assertEquals(3L, recovered.deletedClaims());
        verify(calculationRunRepository, never()).save(any(CalculationRun.class));
    }

    private CalculationRun runningRun() {
        return run(CalculationRunStatuses.RUNNING, LocalDateTime.of(2026, 4, 11, 10, 1));
    }

    private CalculationRun requestedRun() {
        return run(CalculationRunStatuses.REQUESTED, null);
    }

    private CalculationRun run(String status, LocalDateTime startedAt) {
        return new CalculationRun(
                5L,
                "ESP",
                "202501",
                "NORMAL",
                "ENGINE",
                "1.0",
                LocalDateTime.of(2026, 4, 11, 10, 0),
                "hr.manager@b4rrhh",
                status,
                "{\"selectionType\":\"ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD\"}",
                871,
                7,
                7,
                0,
                0,
                7,
                0,
                0,
                startedAt,
                null,
                null,
                LocalDateTime.of(2026, 4, 11, 10, 0),
                LocalDateTime.of(2026, 4, 11, 10, 1)
        );
    }
}
