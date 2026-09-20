package com.b4rrhh.payroll_engine.concept.infrastructure.web;

/**
 * El nombre que se le pone al concepto.
 *
 * <p>Obligatorio y no vacio: un literal en blanco no es «sin nombre», es un hueco. Un concepto
 * sin nombre se queda sin fila en la tabla de literales y ensena su mnemonico ({@code backend#109}).
 */
public record UpdateConceptLabelRequest(String label) {}
