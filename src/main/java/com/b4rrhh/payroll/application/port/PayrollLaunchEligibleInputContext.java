package com.b4rrhh.payroll.application.port;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que el lanzador sabe del empleado y del periodo cuando va a calcular una unidad.
 *
 * @param agreementCode el convenio vigente al final de la presencia dentro del periodo, que es lo
 *        que decide que conceptos entran en el plan. Para saber que precio se paga cada dia no
 *        basta: eso lo dicen los tramos de {@code agreementWindows} (backend#47)
 * @param agreementCategoryCode la categoria vigente al final de la presencia, con la misma
 *        salvedad
 * @param agreementWindows los tramos de clasificacion laboral que tocan el periodo, en orden.
 *        Rompen el periodo igual que la jornada, porque la categoria decide de que fila de tabla
 *        sale el precio del dia (backend#47)
 * @param contractWindows los tramos de contrato que tocan el periodo, en orden. Rompen el periodo
 *        aunque hoy no los lea ningun concepto: el dia que alguno los lea, ya estara partido
 * @param extraPaymentRegimeWindows los tramos de regimen de pagas extras que tocan el periodo, en
 *        orden. Rompen el periodo por la misma razon: la prorrata de un tramo prorrateado y la de
 *        uno que no lo es entran por puertas distintas (backend#118, backend#119)
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
        List<PayrollLaunchAgreementWindowContext> agreementWindows,
        List<PayrollLaunchContractWindowContext> contractWindows,
        List<PayrollLaunchExtraPaymentRegimeWindowContext> extraPaymentRegimeWindows,
        LocalDate presenceStartDate,
        LocalDate presenceEndDate,
        String workCenterCode,
        LocalDate seniorityDate
) {
}
