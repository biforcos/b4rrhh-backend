package com.b4rrhh.payroll.application.usecase;

public interface RecoverAbandonedPayrollCalculationRunsUseCase {

    /**
     * Cierra las ejecuciones que se quedaron a medias y borra las reservas huerfanas (#75).
     *
     * <p>Se llama al arrancar. Mientras el lanzamiento era sincrono esto no hacia falta:
     * el cliente esperaba y veia el error. En cuanto el trabajo vive fuera de la peticion,
     * un backend que se reinicia a mitad deja una ejecucion en RUNNING para siempre y sus
     * reservas puestas, y la siguiente corrida se salta a esos empleados contandolos en
     * total_skipped_already_claimed: nadie vuelve a calcularlos y el informe sale en verde.
     */
    RecoveredPayrollLaunchState recover();
}
