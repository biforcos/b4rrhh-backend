package com.b4rrhh.payroll.infrastructure.async;

import com.b4rrhh.payroll.application.port.PayrollLaunchWorkerPort;
import com.b4rrhh.payroll.infrastructure.config.PayrollLaunchExecutionProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * El hilo que calcula la nomina cuando la peticion ya ha contestado (#75).
 *
 * <p><b>Un solo hilo, a proposito.</b> Dos lanzamientos simultaneos no compiten: el
 * segundo se queda en {@code REQUESTED} —encolado y consultable— hasta que el primero
 * termina. El {@code calculation_claim} protege los datos unidad por unidad, pero no dice
 * nada de la cola: serializar las ejecuciones es lo que hace que no haga falta que lo
 * diga. Y un cliente real con diez mil empleados no gana nada repartiendo un calculo que
 * ya satura la base; lo que necesita es verlo avanzar, y eso ya lo da el run persistido.
 *
 * <p><b>La cola es acotada.</b> Quien pulsa el boton diez veces no crea diez ejecuciones
 * vivas: a partir de la capacidad configurada el {@code submit} rechaza, y el lanzamiento
 * cierra esa ejecucion como {@code FAILED} con su mensaje. Rechazar se ve; aceptar y
 * perder, no.
 *
 * <p><b>No espera a terminar al apagarse.</b> Un despliegue no puede quedarse cinco
 * minutos colgado esperando a una nomina, asi que al apagar se interrumpe el trabajo en
 * curso. La ejecucion se queda a medias en {@code RUNNING} y es el barrido de arranque
 * quien la cierra: esa es la unica manera de que un {@code docker compose up -d} no
 * dependa de lo que estuviera corriendo.
 */
@Component
public class SingleWorkerPayrollLaunchAdapter implements PayrollLaunchWorkerPort {

    private final ThreadPoolExecutor executor;

    public SingleWorkerPayrollLaunchAdapter(PayrollLaunchExecutionProperties properties) {
        int queueCapacity = properties.getQueueCapacity();
        if (queueCapacity < 1) {
            throw new IllegalStateException(
                    "payroll.launch.execution.queue-capacity must be at least 1, was " + queueCapacity
            );
        }
        this.executor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new PayrollLaunchThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    @Override
    public void submit(Runnable work) {
        executor.execute(work);
    }

    @PreDestroy
    void stopWithoutWaiting() {
        executor.shutdownNow();
    }

    /**
     * Hilos con nombre y demonio: el nombre para que un volcado de hilos diga que estaba
     * calculando nomina, y demonio para que la JVM no se niegue a morir por ellos.
     */
    private static final class PayrollLaunchThreadFactory implements java.util.concurrent.ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "payroll-launch");
            thread.setDaemon(true);
            return thread;
        }
    }
}
