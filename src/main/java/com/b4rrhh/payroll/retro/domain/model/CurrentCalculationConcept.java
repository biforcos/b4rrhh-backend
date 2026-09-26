package com.b4rrhh.payroll.retro.domain.model;

import java.math.BigDecimal;

/**
 * Una linea del calculo vigente de un mes ({@code backend#131}).
 *
 * <p>Las mismas columnas que una linea de recibo en lo que hace falta para construir una linea de
 * atraso, y ni una mas. <b>No lleva periodo de origen</b>: el vigente es de un mes y todo lo suyo es
 * de ese mes. Un atraso dentro de un vigente seria el atraso de un atraso, y eso no existe.
 */
public record CurrentCalculationConcept(
        Integer lineNumber,
        String conceptCode,
        String conceptMnemonic,
        String conceptLabel,
        BigDecimal amount,
        BigDecimal quantity,
        BigDecimal rate,
        String conceptNatureCode,
        Integer displayOrder,
        String payslipSectionCode,
        String payslipSubsectionCode
) {
}
