package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.domain.model.CalculationRunMessage;
import com.b4rrhh.payroll.domain.port.CalculationClaimRepository;
import com.b4rrhh.payroll.domain.port.CalculationRunMessageRepository;
import com.b4rrhh.payroll.domain.port.CalculationRunRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecoverAbandonedPayrollCalculationRunsService implements RecoverAbandonedPayrollCalculationRunsUseCase {

    /**
     * Los dos estados que significan "esto estaba en marcha". Al arrancar no hay nada en
     * marcha, asi que una ejecucion en cualquiera de los dos es una ejecucion muerta:
     * RUNNING es la que calculaba, REQUESTED la que esperaba turno en la cola.
     */
    private static final List<String> UNFINISHED_STATUSES =
            List.of(CalculationRunStatuses.REQUESTED, CalculationRunStatuses.RUNNING);

    private final CalculationRunRepository calculationRunRepository;
    private final CalculationRunMessageRepository calculationRunMessageRepository;
    private final CalculationClaimRepository calculationClaimRepository;

    public RecoverAbandonedPayrollCalculationRunsService(
            CalculationRunRepository calculationRunRepository,
            CalculationRunMessageRepository calculationRunMessageRepository,
            CalculationClaimRepository calculationClaimRepository
    ) {
        this.calculationRunRepository = calculationRunRepository;
        this.calculationRunMessageRepository = calculationRunMessageRepository;
        this.calculationClaimRepository = calculationClaimRepository;
    }

    @Override
    public RecoveredPayrollLaunchState recover() {
        List<CalculationRun> abandonedRuns = calculationRunRepository.findByStatusIn(UNFINISHED_STATUSES);
        for (CalculationRun abandonedRun : abandonedRuns) {
            closeAsFailed(abandonedRun);
        }

        // Se borran todas, no solo las de las ejecuciones cerradas arriba: al arrancar
        // ninguna ejecucion sostiene una reserva, asi que cualquier claim que quede es
        // huerfano. Esto recoge tambien los que dejo sin limpiar una corrida que si
        // termino, porque esa limpieza es best-effort y puede haberse perdido.
        long deletedClaims = calculationClaimRepository.deleteAll();

        return new RecoveredPayrollLaunchState(abandonedRuns.size(), deletedClaims);
    }

    private void closeAsFailed(CalculationRun abandonedRun) {
        calculationRunMessageRepository.save(new CalculationRunMessage(
                null,
                abandonedRun.id(),
                "RUN_ABANDONED_ON_RESTART",
                "ERROR",
                "Payroll calculation run did not finish: the backend was restarted while it was "
                        + abandonedRun.status(),
                null,
                abandonedRun.ruleSystemCode(),
                null,
                null,
                abandonedRun.payrollPeriodCode(),
                abandonedRun.payrollTypeCode(),
                null,
                LocalDateTime.now()
        ));

        // Los contadores se quedan como estaban: dicen cuanto se habia hecho de verdad
        // antes del corte, y eso es informacion. Lo que cambia es el estado, que es lo que
        // nadie movia: FAILED se lee como "no te fies de esto", y COMPLETED no.
        calculationRunRepository.save(abandonedRun.withFinishedExecutionEvenIfNeverStarted(
                CalculationRunStatuses.FAILED,
                LocalDateTime.now(),
                abandonedRun.summaryJson()
        ));
    }
}
