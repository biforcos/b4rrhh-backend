package com.b4rrhh.payroll.infrastructure.web.dto;

import java.math.BigDecimal;

/**
 * Una linea del recibo, tal y como se sirve.
 *
 * <p>{@code mergedStepCount} dice de cuantos pasos del motor viene ({@code backend#103}). Vale uno
 * casi siempre; mas de uno cuando el folio ha fundido varios tramos del mismo concepto al mismo
 * precio, que pueden <b>no ser contiguos</b>. Sin ese numero la linea es correcta y cuenta una
 * historia falsa, y la pantalla del recibo y la de «Calculo» discrepan en el numero de filas sin que
 * nada explique por que.
 *
 * <p>Para llegar de la linea a sus pasos: los pasos de ese recibo cuyo {@code payslipLineNumber} sea
 * este {@code lineNumber}. No hay que reconstruir ninguna agrupacion.
 *
 * <p>{@code conceptMnemonic} y {@code conceptLabel} son dos cosas y las dos estan
 * ({@code backend#109}). El mnemonico es el <b>identificador</b> del concepto en el motor —lo que
 * las reglas y el grafo referencian— y el literal es como se llamaba el concepto <b>cuando se
 * calculo esta linea</b>. El literal no se resuelve al leer: viene congelado, porque el recibo es
 * un documento y no una vista del catalogo.
 */
public record PayrollConceptResponse(
        Integer lineNumber,
        String conceptCode,
        String conceptMnemonic,
        String conceptLabel,
        BigDecimal amount,
        BigDecimal quantity,
        BigDecimal rate,
        String conceptNatureCode,
        String originPeriodCode,
        Integer displayOrder,
        Integer mergedStepCount
) {
}