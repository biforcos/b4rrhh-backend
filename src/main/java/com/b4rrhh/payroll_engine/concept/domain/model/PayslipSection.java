package com.b4rrhh.payroll_engine.concept.domain.model;

/**
 * Un bloque del modelo oficial de recibo de salarios ({@code backend#109}).
 *
 * <p>Devengos, deducciones, bases, liquido y aportacion empresarial. Estaban implicitos en que
 * los devengos van por el 1xx y las deducciones por el 7xx, y un rango numerico no es una
 * declaracion: aguanta hasta que alguien numera un devengo en el 750 porque le tocaba ahi por
 * orden, y entonces se rompe en silencio.
 *
 * <p>A que seccion va una linea no lo decide su codigo: lo decide su
 * {@link FunctionalNature naturaleza}, que ya dice lo que el concepto es.
 */
public record PayslipSection(
        String sectionCode,
        String label,
        int displayOrder
) {

    public PayslipSection {
        if (sectionCode == null || sectionCode.isBlank()) {
            throw new IllegalArgumentException("sectionCode is required");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label is required");
        }
        sectionCode = sectionCode.trim();
        label = label.trim();
    }
}
