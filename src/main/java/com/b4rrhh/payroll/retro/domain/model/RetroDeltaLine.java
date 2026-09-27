package com.b4rrhh.payroll.retro.domain.model;

import java.math.BigDecimal;

/**
 * Una linea de atraso: la diferencia de un concepto de un mes anterior ({@code backend#133}).
 *
 * <p><b>No hay cantidad ni tarifa</b>, y no es un olvido ({@code backend#135}): el importe de esta linea
 * es {@code vigente - pagado}, asi que las del vigente no lo multiplican. Copiarlas dejaba filas como
 * {@code 1.680,00 x 0,10 = 0,08}, tres numeros correctos por separado que no cuadran entre si, y un
 * recibo se lee linea a linea. Lo que explica una linea de atraso son los tres numeros del
 * {@code backend#134} -vigente, pagado y diferencia-, que estan en la explicacion y no en la fila.
 *
 * <p>La regla no admite el caso bueno: aunque la diferencia si fuera cantidad x tarifa -una tarifa que
 * no cambio y una cantidad que si-, la fila sigue sin llevarlas. Un recibo en el que unas filas de
 * atraso multiplican y otras no obliga al que lo lee a saber cual es cual.
 *
 * @param originPeriodCode el mes al que pertenece esta linea. Es lo que va a
 *        {@code payroll_concept.origin_period_code}, la columna que existia desde la V53 y que no llenaba
 *        nadie
 * @param amount la diferencia, que puede ser <b>negativa</b>: unas horas que se borraron son dinero que
 *        se cobro de mas
 */
public record RetroDeltaLine(
        String originPeriodCode,
        String conceptCode,
        String conceptMnemonic,
        String conceptLabel,
        BigDecimal amount,
        String conceptNatureCode,
        Integer displayOrder,
        String payslipSectionCode,
        String payslipSubsectionCode
) {
}
