package com.b4rrhh.payroll.basesalary.domain;

import java.math.BigDecimal;

/**
 * La fila de tabla que la busqueda resolvio, y no solo su numero ({@code backend#107}).
 *
 * <p>La busqueda devuelve la <b>fila</b> porque quien la hace tiene que poder guardar de donde
 * salio el valor. Antes devolvia un {@code BigDecimal} y la procedencia se perdia ahi mismo: el
 * motor sabia que fila habia leido durante una linea de codigo y nadie volvia a saberlo.
 *
 * @param rowId        identidad de la fila en {@code payroll.payroll_table_row}
 * @param tableCode    tabla en la que vive, que es lo que la hace direccionable
 * @param monthlyValue valor mensual de la fila, o nulo si la fila no lo lleva
 * @param dailyValue   valor diario de la fila, o nulo si la fila no lo lleva
 */
public record PayrollTableRowRead(
        long rowId,
        String tableCode,
        BigDecimal monthlyValue,
        BigDecimal dailyValue
) {
}
