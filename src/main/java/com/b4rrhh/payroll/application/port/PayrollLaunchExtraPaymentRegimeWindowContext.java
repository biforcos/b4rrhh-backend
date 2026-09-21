package com.b4rrhh.payroll.application.port;

import com.b4rrhh.payroll.application.usecase.PayrollPeriodSegmentation;

import java.time.LocalDate;

/**
 * Un tramo de regimen de pagas extras del empleado dentro del periodo ({@code backend#118}).
 *
 * <p>Rompe el periodo como lo rompen la jornada, la clasificacion y el contrato: cambiar de
 * regimen a mitad de mes es cambiar las condiciones con las que se calcula, y la prorrata de cada
 * tramo entra por una puerta distinta ({@code backend#119}). Si el periodo no se partiera, no
 * habria donde poner las dos.
 */
public record PayrollLaunchExtraPaymentRegimeWindowContext(
        LocalDate startDate,
        LocalDate endDate,
        boolean prorated
) implements PayrollPeriodSegmentation.DatedWindow {
}
