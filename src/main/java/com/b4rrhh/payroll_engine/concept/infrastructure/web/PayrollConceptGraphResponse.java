package com.b4rrhh.payroll_engine.concept.infrastructure.web;

import java.util.List;

/**
 * El grafo entero de un sistema de reglas ({@code designer#15}).
 *
 * <p>Las aristas van planas y no agrupadas por concepto: quien pide esto dibuja aristas, y
 * agruparlas le obligaria a deshacer el agrupamiento nada mas recibirlo.
 */
public record PayrollConceptGraphResponse(
        String ruleSystemCode,
        List<PayrollConceptDesignerResponse> concepts,
        List<ConceptGraphOperandResponse> operands,
        List<ConceptGraphFeedResponse> feeds
) {
}
