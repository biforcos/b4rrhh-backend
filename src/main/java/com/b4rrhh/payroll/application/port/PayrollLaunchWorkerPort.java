package com.b4rrhh.payroll.application.port;

import java.util.concurrent.RejectedExecutionException;

/**
 * Donde corre el trabajo de un lanzamiento cuando la peticion HTTP ya ha contestado (#75).
 *
 * <p>Es un puerto y no un {@code Executor} inyectado a pelo porque Spring Boot ya publica
 * su propio executor de aplicacion: pedir "un Executor" seria ambiguo, y el tipo no dice
 * nada de la politica —un hilo, en cola— que hace que esto sea seguro.
 */
public interface PayrollLaunchWorkerPort {

    /**
     * Encola el trabajo y vuelve. No espera a que termine.
     *
     * @throws RejectedExecutionException si la cola esta llena. Quien llama tiene que
     *         cerrar la ejecucion que ya creo: una peticion aceptada y perdida es peor
     *         que una rechazada.
     */
    void submit(Runnable work);
}
