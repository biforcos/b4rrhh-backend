package com.b4rrhh.payroll.retro.application.usecase;

/**
 * Recalcular el tramo de meses cerrados de un empleado ({@code backend#131}).
 *
 * @param fromPeriodCode el mes mas antiguo que se recalcula, incluido
 * @param toPeriodCode   el ultimo que se recalcula, incluido. Es {@code P-1}: el periodo abierto se
 *                       calcula por el camino normal y escribe su recibo
 * @param runId          el run del periodo abierto que dispara la retro, para que cada vigente diga
 *                       quien lo escribio. Nulo en los escenarios que no vienen de un lanzamiento
 */
public record RecalculateClosedPeriodsCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        Integer presenceNumber,
        String payrollTypeCode,
        String fromPeriodCode,
        String toPeriodCode,
        String calculationEngineCode,
        String calculationEngineVersion,
        Long runId
) {
}
