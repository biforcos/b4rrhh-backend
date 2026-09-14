package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.PayrollCalculationStep;

import java.util.List;
import java.util.Optional;

/**
 * Los pasos con los que se calculó un recibo, en orden de ejecución ({@code backend#97}).
 *
 * <p><b>El {@code Optional} separa los dos vacíos</b>, que es de lo que va este caso de uso:
 *
 * <ul>
 *   <li>{@code Optional.empty()} — no hay recibo en esa dirección. La web lo traduce a {@code 404}.
 *   <li>{@code Optional.of(List.of())} — el recibo existe y no tiene ni un paso. Es un caso real y
 *       frecuente: todo recibo calculado antes de la {@code V129} está así, y recibos viejos sin
 *       pasos van a existir siempre. La web lo sirve como {@code 200} con lista vacía.
 *   <li>{@code Optional.of(lista)} — el recibo existe y aquí está lo que hizo el motor.
 * </ul>
 *
 * <p>Devolver una lista pelada juntaría los dos primeros en el mismo vacío mudo, que es la forma
 * que llevamos toda la semana quitando.
 */
public interface ListPayrollCalculationStepsUseCase {

    Optional<List<PayrollCalculationStep>> listByPayrollBusinessKey(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode,
            Integer presenceNumber
    );
}
