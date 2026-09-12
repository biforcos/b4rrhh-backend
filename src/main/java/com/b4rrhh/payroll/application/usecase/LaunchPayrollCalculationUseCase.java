package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.model.CalculationRun;

public interface LaunchPayrollCalculationUseCase {

    /**
     * Acepta el lanzamiento y devuelve la ejecucion recien creada, en REQUESTED, sin
     * esperar a que termine. Es lo que usa la API: calcular la plantilla son cinco minutos
     * y la peticion no puede quedarse esperandolos (#75).
     */
    CalculationRun requestLaunch(LaunchPayrollCalculationCommand command);

    /**
     * Lanza y espera a que acabe. Es el camino en proceso —escenarios y tests, que corren
     * en su propia transaccion— y el mismo trabajo que ejecuta el worker.
     */
    CalculationRun launch(LaunchPayrollCalculationCommand command);
}