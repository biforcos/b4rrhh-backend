package com.b4rrhh.payroll_engine.concept.infrastructure.web;

/**
 * Un bloque del modelo oficial de recibo ({@code backend#109}).
 *
 * <p>Lo que coloca una linea en un bloque es su {@code conceptNatureCode}, no su codigo: el
 * bloque no se deduce de que el concepto sea 1xx o 7xx.
 */
public record PayslipSectionResponse(
        String sectionCode,
        String label,
        Integer displayOrder
) {
}
