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
 *
 * <p>{@code payslipLineNumber} dice <b>en qué línea</b> quedó ({@code backend#103}). Dos pasos con el
 * mismo número son los que esa línea funde: mismo concepto, mismo precio, y el folio los suma en una
 * sola fila aunque sus tramos no sean contiguos. Es lo que permite que desde un paso repetido se
 * pueda decir por qué es dos —precios distintos, o el mismo precio por caminos distintos— en vez de
 * dejarlo a que alguien compare las tarifas a ojo.
 *
 * <p>Es nulo exactamente cuando {@code payslipOrderCode} lo es, salvo en los recibos calculados antes
 * del {@code backend#103}, que se quedan sin él hasta que se recalculen.
 *
 * <p>{@code sourceTableCode} y {@code sourceTableRowId} dicen <b>de qué fila de tabla</b> leyó este
 * paso su valor ({@code backend#107}). Van juntos o no van, y lo normal es que no vayan: de los 38
 * pasos de un recibo ESP leen una fila dos. Son la dirección que el motor tenía delante al
 * calcular, no la que la misma búsqueda daría hoy, y por eso la fila puede haber desaparecido:
 * quien ofrezca el salto tiene que saber tratar una dirección que ya no lleva a ninguna parte.
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
        String payslipOrderCode,
        Integer payslipLineNumber,
        String sourceTableCode,
        Long sourceTableRowId
) {
}
