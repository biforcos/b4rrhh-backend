package com.b4rrhh.payroll_engine.concept.domain.model;

/**
 * Una parte de un bloque del recibo ({@code backend#121}).
 *
 * <p>El modelo oficial tiene un bloque —«Determinación de las bases de cotización»— con cuatro
 * apartados numerados dentro, y cada uno se lee de arriba abajo. La {@link PayslipSection} dice
 * en qué bloque va una línea; esto dice en qué parte del bloque.
 *
 * <p>A qué apartado va una línea <b>no se deduce de su naturaleza</b>, que es la diferencia con
 * la sección: los diez conceptos del recuadro de bases son todos {@code BASE} y van en cuatro
 * apartados distintos. Es una propiedad del concepto y por eso la declara el concepto.
 */
public record PayslipSubsection(
        String subsectionCode,
        String label,
        int displayOrder
) {

    public PayslipSubsection {
        if (subsectionCode == null || subsectionCode.isBlank()) {
            throw new IllegalArgumentException("subsectionCode is required");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label is required");
        }
        subsectionCode = subsectionCode.trim();
        label = label.trim();
    }
}
