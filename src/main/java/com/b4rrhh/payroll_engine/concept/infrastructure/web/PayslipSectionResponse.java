package com.b4rrhh.payroll_engine.concept.infrastructure.web;

import java.util.List;

/**
 * Un bloque del modelo oficial de recibo ({@code backend#109}).
 *
 * <p>Lo que coloca una linea en un bloque es su {@code conceptNatureCode}, no su codigo: el
 * bloque no se deduce de que el concepto sea 1xx o 7xx.
 */
public record PayslipSectionResponse(
        String sectionCode,
        String label,
        Integer displayOrder,
        /**
         * Las partes en las que se divide este bloque ({@code backend#121}).
         *
         * <p>Vacia en cuatro de los cinco, que es el caso normal. El recuadro de bases tiene
         * cuatro, y lo que coloca una linea en una de ellas es su {@code payslipSubsectionCode},
         * congelado con la linea igual que el del bloque.
         */
        List<PayslipSubsectionResponse> subsections
) {

    /** Una parte de un bloque: su nombre y su sitio dentro del bloque. */
    public record PayslipSubsectionResponse(
            String subsectionCode,
            String label,
            Integer displayOrder
    ) {
    }
}
