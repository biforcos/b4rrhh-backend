package com.b4rrhh.payroll.application.port;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que el lanzador sabe del empleado y del periodo cuando va a calcular una unidad.
 *
 * @param presenceStartDate el arranque de la presencia de ESTA unidad
 * @param seniorityDate la antiguedad del empleado, como fecha: el arranque de su presencia mas
 *        antigua, cortes incluidos, asi que quien se readmite la conserva del primer alta
 *        (backend#91). No es {@code presenceStartDate}: para un readmitido son dos fechas
 *        distintas, y usar la segunda daria un numero que contradice al de la ficha. Viaja como
 *        fecha y no como duracion porque una antiguedad es un punto de partida y no un
 *        cronometro: la duracion depende de hasta cuando se cuente, y la ficha cuenta hasta hoy
 *        mientras un recibo de abril tiene que contar hasta abril.
 */
public record PayrollLaunchEligibleInputContext(
        String companyCode,
        String agreementCode,
        String agreementCategoryCode,
        List<PayrollLaunchWorkingTimeWindowContext> workingTimeWindows,
        LocalDate presenceStartDate,
        LocalDate presenceEndDate,
        String workCenterCode,
        LocalDate seniorityDate
) {
}
