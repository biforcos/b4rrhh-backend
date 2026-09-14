package com.b4rrhh.payroll.application.port;

import java.util.List;

/**
 * Lee los pasos con los que se calculó un recibo.
 *
 * <p><b>Existe porque ya hay quien los lee, y se sabe quién.</b> Es el comentario del
 * {@link PayrollCalculationStepWritePort} al revés: allí no hay método de lectura a propósito,
 * porque un {@code findByPayrollId} que nadie llamara devolvería lista vacía siempre y «vacío» no
 * se distinguiría de «este recibo no tiene pasos». La condición que aquel puerto se puso era que el
 * de lectura llegara con su consumidor delante; el consumidor es el endpoint {@code /steps} de
 * {@code PayrollController}, que sirve la pestaña «Cálculo» de la Valorización
 * ({@code backend#97}, {@code frontend#65}).
 *
 * <p>Y la lista vacía sigue sin ser ambigua, porque quien pregunta ya sabe que el recibo existe:
 * el caso «no hay recibo» lo resuelve {@code ListPayrollCalculationStepsService} antes de llegar
 * aquí, con un {@code Optional} vacío que la web traduce a {@code 404}. Una lista vacía de este
 * puerto significa una sola cosa: ese recibo se calculó antes de la {@code V129} y sus pasos no se
 * guardaron nunca.
 */
public interface PayrollCalculationStepReadPort {

    /**
     * Los pasos de un recibo, <b>en orden de ejecución</b>.
     *
     * <p>El orden no es un detalle de presentación y no se cambia: la lista de importes ordenada
     * por folio ya existe —es el recibo—, y lo único que estos pasos aportan es en qué orden los
     * fue sacando el motor. Ordenar por {@code payslipOrderCode}, por naturaleza o por código
     * destruye el contenido entero de la explicación ({@code backend#97}).
     *
     * <p>Devuelve lista vacía cuando el recibo no tiene pasos, y eso <b>no</b> se rellena
     * derivándolos de {@code payroll.payroll_concept}: serían las 14 líneas del folio disfrazadas
     * de 35 pasos de cálculo, que es exactamente la mentira que el {@code backend#93} se abrió
     * para no contar.
     */
    List<PayrollCalculationStep> findStepsOf(long payrollId);
}
