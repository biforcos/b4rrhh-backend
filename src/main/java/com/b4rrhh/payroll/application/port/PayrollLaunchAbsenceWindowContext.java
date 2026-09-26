package com.b4rrhh.payroll.application.port;

import com.b4rrhh.payroll.application.usecase.PayrollPeriodSegmentation;

import java.time.LocalDate;

/**
 * Una ausencia del empleado que toca el periodo ({@code backend#127}).
 *
 * <p>Es la <b>cuarta causa de corte</b> del periodo, tras la jornada, la clasificacion/contrato y el
 * regimen de pagas extras. Hasta este issue {@code employee.employee_absence} existia y no llegaba
 * al motor: un empleado de vacaciones del 15 al 25 cobraba 30 dias, y uno con un dia de baja por
 * enfermedad comun, tambien.
 *
 * <p>Vienen <b>todas</b> las que solapan el periodo, del tipo que sean, y quien decide cuales
 * parten es {@code CalculatePayrollUnitService}. Filtrarlas aqui repartiria esa decision entre la
 * consulta y el calculo, y es una sola: <b>parte la ausencia que cambia lo que se paga</b>
 * (ADR-073).
 *
 * @param endDate nulo es una ausencia sin cerrar: llega hasta donde llegue el periodo
 * @param benefitEntitled si la baja lleva derecho a prestacion ({@code backend#129}). Sin el, la baja
 *        quita dias igual y no paga nada ni cotiza: es dato y no regla, y lo pone quien registra la
 *        baja con la resolucion del INSS
 */
public record PayrollLaunchAbsenceWindowContext(
        LocalDate startDate,
        LocalDate endDate,
        String absenceTypeCode,
        boolean benefitEntitled
) implements PayrollPeriodSegmentation.DatedWindow {
}
