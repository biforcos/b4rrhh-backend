package com.b4rrhh.payroll.application.port;

import com.b4rrhh.payroll.application.usecase.PayrollCalculationUnit;
import com.b4rrhh.payroll.domain.model.PayrollStatus;

import java.util.List;
import java.util.Map;

/**
 * Cambia de estado muchos recibos a la vez, en una sola sentencia (b4rrhh/backend#150).
 *
 * <p>Invalidar un mes de ESP en la semilla, 883 recibos, costaba entre 60 y 100 segundos: por
 * cada candidato se cargaba el agregado entero —conceptos, instantáneas, tramos— para cambiar un
 * estado. Aquí no se carga nada. Qué transición se pide lo decide el servicio; esto sólo la
 * aplica.
 */
public interface PayrollBulkStatusTransitionPort {

    /**
     * Pasa a {@code to}, con este motivo, los recibos de estas unidades que están en
     * {@code from}, y devuelve cuántos recibos de las unidades había en cada estado
     * <b>antes</b> de tocarlos. Las unidades sin recibo no cuentan en ningún estado.
     */
    Map<PayrollStatus, Integer> moveStatus(
            String ruleSystemCode,
            String payrollPeriodCode,
            String payrollTypeCode,
            List<PayrollCalculationUnit> units,
            PayrollStatus from,
            PayrollStatus to,
            String statusReasonCode
    );
}
