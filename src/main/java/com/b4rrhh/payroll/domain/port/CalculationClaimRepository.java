package com.b4rrhh.payroll.domain.port;

import com.b4rrhh.payroll.domain.model.CalculationClaim;

public interface CalculationClaimRepository {

    CalculationClaim save(CalculationClaim calculationClaim);

    void deleteById(Long id);

    void deleteByRunId(Long runId);

    /**
     * Borra todas las reservas y devuelve cuantas habia.
     *
     * <p>Solo tiene sentido al arrancar: un claim solo vive mientras una ejecucion lo
     * sostiene, y al arrancar no hay ninguna ejecucion corriendo. Lo que quede son
     * huerfanos de una corrida que murio, y dejarlos es peor que borrarlos: la siguiente
     * ejecucion se saltaria a esos empleados contandolos como ya reservados, o sea que no
     * volverian a calcularse y el informe saldria en verde (#75).
     */
    long deleteAll();
}