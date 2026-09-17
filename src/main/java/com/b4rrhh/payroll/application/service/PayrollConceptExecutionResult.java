package com.b4rrhh.payroll.application.service;

import com.b4rrhh.payroll.application.port.TableRowOrigin;

import java.math.BigDecimal;

/**
 * @param sourceTableRow la fila de tabla de la que salio el importe, o {@code null} si no salio de
 *                       ninguna ({@code backend#107}). Viaja con el resultado porque es el unico
 *                       momento en que se sabe: quien lo reciba puede guardarla, y quien la
 *                       buscara despues ya no estaria contestando la misma pregunta.
 */
public record PayrollConceptExecutionResult(
        String conceptCode,
        BigDecimal amount,
        BigDecimal quantity,
        BigDecimal rate,
        TableRowOrigin sourceTableRow
) {

    /** El resultado de un concepto cuyo valor no se leyo de ninguna fila de tabla. */
    public PayrollConceptExecutionResult(
            String conceptCode,
            BigDecimal amount,
            BigDecimal quantity,
            BigDecimal rate
    ) {
        this(conceptCode, amount, quantity, rate, null);
    }
}
