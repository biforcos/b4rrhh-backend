package com.b4rrhh.payroll.application.usecase;

/**
 * Lo que encontro el barrido de arranque. Los dos numeros son el diagnostico de lo que
 * pasaba cuando el backend se fue: ejecuciones a medias y reservas que nadie sostenia.
 */
public record RecoveredPayrollLaunchState(
        int closedRuns,
        long deletedClaims
) {
}
