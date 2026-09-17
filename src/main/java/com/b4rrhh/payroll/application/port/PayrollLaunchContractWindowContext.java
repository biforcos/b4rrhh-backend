package com.b4rrhh.payroll.application.port;

import com.b4rrhh.payroll.application.usecase.PayrollPeriodSegmentation;

import java.time.LocalDate;

/**
 * Un tramo de contrato del empleado dentro del periodo ({@code backend#47}).
 *
 * <p><b>Hoy no lo lee ningun concepto</b>, y aun asi rompe el periodo. No es un descuido: un cambio
 * de contrato a mitad de mes es un cambio de las condiciones con las que se calcula, y si el
 * periodo no se parte, el dia en que algun concepto lo lea no habra donde ponerlo. Lo que un
 * segmento de mas cuesta hoy es un paso repetido en la pestana de calculo; el folio no cambia,
 * porque el colapso por {@code concepto|tarifa} vuelve a juntar dos tramos que valen lo mismo.
 */
public record PayrollLaunchContractWindowContext(
        LocalDate startDate,
        LocalDate endDate,
        String contractCode,
        String contractSubtypeCode
) implements PayrollPeriodSegmentation.DatedWindow {
}
