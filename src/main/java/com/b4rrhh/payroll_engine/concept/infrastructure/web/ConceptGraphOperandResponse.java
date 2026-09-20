package com.b4rrhh.payroll_engine.concept.infrastructure.web;

/**
 * Una arista de operando, con el concepto al que apunta ({@code designer#15}).
 *
 * <p>Lleva {@code conceptCode} y {@link ConceptOperandResponse} no, y no es duplicacion: alli el
 * destino esta en la ruta y aqui vienen todos juntos, asi que sin el la arista no sabria de donde
 * a donde va.
 */
public record ConceptGraphOperandResponse(
        String conceptCode,
        String operandRole,
        String sourceObjectCode
) {
}
