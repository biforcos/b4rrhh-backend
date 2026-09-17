package com.b4rrhh.payroll.application.port;

import com.b4rrhh.payroll.application.usecase.PayrollPeriodSegmentation;

import java.time.LocalDate;

/**
 * Un tramo de clasificacion laboral del empleado dentro del periodo ({@code backend#47}).
 *
 * <p>Es el convenio y la categoria que estaban vigentes entre dos fechas, y hace falta entero —no
 * solo el ultimo— porque la categoria decide de que fila de tabla sale el precio del dia. Con un
 * cambio a mitad de mes, resolverla una vez para todo el periodo pone un precio en dias que se
 * pagaron a otro, que es justo la mentira que el colapso por {@code concepto|tarifa} esta pensado
 * para impedir.
 */
public record PayrollLaunchAgreementWindowContext(
        LocalDate startDate,
        LocalDate endDate,
        String agreementCode,
        String agreementCategoryCode
) implements PayrollPeriodSegmentation.DatedWindow {
}
