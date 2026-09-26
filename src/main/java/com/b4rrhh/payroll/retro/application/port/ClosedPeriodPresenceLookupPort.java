package com.b4rrhh.payroll.retro.application.port;

import java.util.List;

/**
 * Que presencias de este empleado tienen ya un recibo <b>entregado</b> de un periodo
 * ({@code backend#130}).
 *
 * <p>Es la pregunta que decide si una escritura con fecha deja marca o no deja nada, y es la
 * definicion operativa de «el pasado»: pasado no es «antes de hoy», es <b>un periodo que ya tiene un
 * documento fuera del sistema</b>. Un mes calculado y sin cerrar no es pasado: se vuelve a calcular
 * y no se lo cuenta a nadie.
 *
 * <p>Devuelve presencias y no un si o un no porque el recibo es de una presencia
 * ({@code payroll.payroll} la lleva en su clave). Un empleado que ceso y volvio en agosto tiene dos
 * recibos de agosto, y unas horas metidas a un dia de la primera presencia no obligan a recalcular la
 * segunda.
 */
public interface ClosedPeriodPresenceLookupPort {

    /**
     * Los numeros de presencia con recibo {@code DEFINITIVE} de ese periodo, o la lista vacia.
     *
     * <p>El filtro por estado va <b>dentro de la consulta</b> y escrito, no parametrizado (ADR-069
     * §2): esta pregunta no admite otro estado.
     */
    List<Integer> findPresencesWithDefinitivePayroll(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode
    );
}
