package com.b4rrhh.payroll.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un paso del cálculo de un recibo, tal y como se sirve ({@code backend#97}).
 *
 * <p>{@code executionOrder} es la clave de fila, no {@code conceptCode}: un concepto de ámbito
 * {@code SEGMENT} se evalúa una vez por segmento, así que en un empleado del mes partido el
 * {@code 101} llega dos veces, con dos segmentos y dos precios. Indexar por concepto enseña uno y
 * se come el otro.
 *
 * <p>{@code payslipOrderCode} nulo significa que ese paso no llegó al folio. Viaja aquí para que el
 * cliente marque las líneas del recibo sin cruzar dos listas ni adivinarlo por la naturaleza.
 */
public record PayrollCalculationStepResponse(
        Integer executionOrder,
        String conceptCode,
        String conceptMnemonic,
        String calculationType,
        String functionalNature,
        String executionScope,
        LocalDate segmentStartDate,
        LocalDate segmentEndDate,
        BigDecimal amount,
        BigDecimal quantity,
        BigDecimal rate,
        String payslipOrderCode
) {
}
