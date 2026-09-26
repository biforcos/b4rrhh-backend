package com.b4rrhh.payroll.retro.domain.port;

import com.b4rrhh.payroll.retro.domain.model.CurrentCalculation;

import java.util.Optional;

/** El calculo vigente, como lo ve el dominio ({@code backend#131}). */
public interface CurrentCalculationRepository {

    /**
     * Guarda el vigente de ese mes, <b>pisando el que hubiera</b>.
     *
     * <p>Pisar y no acumular es la decision: el vigente no es historia, es «cuanto vale este mes
     * hoy». La historia son los recibos.
     */
    CurrentCalculation save(CurrentCalculation calculation);

    Optional<CurrentCalculation> findByBusinessKey(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode,
            Integer presenceNumber
    );
}
