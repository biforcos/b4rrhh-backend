package com.b4rrhh.payroll_engine.concept.application.usecase;

import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;

import java.util.List;

/**
 * El grafo entero de un sistema de reglas: sus conceptos y todas las aristas entre ellos
 * ({@code designer#15}).
 *
 * <p>Las aristas van planas y no agrupadas por concepto porque quien las pide dibuja aristas, no
 * arboles: agruparlas obligaria al cliente a deshacer lo que el servidor acababa de hacer. Cada
 * operando y cada alimentacion sabe a que concepto apunta, que es lo que los extremos por
 * concepto no pueden decir —alli el destino esta en la ruta.
 */
public record ConceptGraph(
        String ruleSystemCode,
        List<PayrollConcept> concepts,
        List<PayrollConceptOperand> operands,
        List<PayrollConceptFeedRelation> feeds
) {
}
