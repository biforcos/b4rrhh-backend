package com.b4rrhh.payroll.infrastructure.async;

import com.b4rrhh.payroll.application.usecase.RecoverAbandonedPayrollCalculationRunsUseCase;
import com.b4rrhh.payroll.application.usecase.RecoveredPayrollLaunchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Recoge lo que dejo a medias el backend anterior, en cuanto este esta en pie (#75).
 *
 * <p><b>Por que al arrancar y no por un reloj.</b> Lo que distingue una ejecucion muerta de
 * una viva es el proceso que la estaba calculando. Al arrancar no hay ninguno: todo lo que
 * este en REQUESTED o RUNNING es de un proceso que ya no existe, y no hay que adivinar
 * cuanto tiempo sin latir es "demasiado". Un barrido por tiempo haria falta el dia que haya
 * dos instancias, y entonces hara falta tambien un lease por instancia; hoy la pila de la
 * demo levanta un unico contenedor de API.
 *
 * <p><b>Por que ApplicationReadyEvent.</b> Mas tarde que un {@code @PostConstruct}: las
 * migraciones de Flyway y el pool de conexiones ya han terminado, asi que el barrido no
 * compite con el arranque ni se come el fallo de una migracion.
 */
@Component
public class PayrollLaunchRecoveryOnStartup {

    private static final Logger log = LoggerFactory.getLogger(PayrollLaunchRecoveryOnStartup.class);

    private final RecoverAbandonedPayrollCalculationRunsUseCase recoverAbandonedPayrollCalculationRunsUseCase;

    public PayrollLaunchRecoveryOnStartup(
            RecoverAbandonedPayrollCalculationRunsUseCase recoverAbandonedPayrollCalculationRunsUseCase
    ) {
        this.recoverAbandonedPayrollCalculationRunsUseCase = recoverAbandonedPayrollCalculationRunsUseCase;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverAbandonedRuns() {
        RecoveredPayrollLaunchState recovered = recoverAbandonedPayrollCalculationRunsUseCase.recover();
        if (recovered.closedRuns() == 0 && recovered.deletedClaims() == 0) {
            return;
        }
        log.warn(
                "Barrido de arranque de nomina: {} ejecucion(es) a medias cerradas como FAILED y {} reserva(s) huerfana(s) borrada(s)",
                recovered.closedRuns(),
                recovered.deletedClaims()
        );
    }
}
