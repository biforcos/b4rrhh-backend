package com.b4rrhh.payroll_engine.concept.domain.model;

/**
 * El nombre de un concepto del motor, en un idioma ({@code backend#109}).
 *
 * <p>No sustituye al {@link PayrollConcept#getConceptMnemonic() mnemonico}: lo acompana. El
 * mnemonico es un <b>identificador</b> —es lo que las reglas referencian para encontrar un
 * concepto— y esto es lo que el documento dice. Dos campos, dos trabajos.
 *
 * <p>El literal puede no existir. Un concepto recien anadido al que se le olvido el nombre no
 * rompe nada: lo que se ensena entonces es su mnemonico, y se nota que falta. Una ausencia
 * visible es mejor que una invisible.
 */
public record ConceptLabel(
        String conceptCode,
        String languageCode,
        String label
) {

    public ConceptLabel {
        if (conceptCode == null || conceptCode.isBlank()) {
            throw new IllegalArgumentException("conceptCode is required");
        }
        if (languageCode == null || languageCode.isBlank()) {
            throw new IllegalArgumentException("languageCode is required");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label is required");
        }
        conceptCode = conceptCode.trim();
        languageCode = languageCode.trim();
        label = label.trim();
        if (label.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("label exceeds max length " + MAX_LENGTH);
        }
    }

    /** Lo que cabe en la columna, y lo que cabe en la linea del recibo que lo congela. */
    public static final int MAX_LENGTH = 200;
}
