package com.b4rrhh.payroll_engine.concept.infrastructure.web;

/**
 * Un concepto del motor, tal y como lo ve el disenador.
 *
 * <p>{@code conceptMnemonic} y {@code label} son dos cosas distintas y las dos estan
 * ({@code backend#109}). El mnemonico es el <b>identificador</b> —lo que las reglas referencian
 * para encontrar el concepto— y el literal es como se llama. {@code label} es nulo cuando el
 * concepto no tiene nombre puesto todavia, y entonces lo que se ensena es el mnemonico: una
 * ausencia visible es mejor que una invisible.
 */
public record PayrollConceptDesignerResponse(
        String ruleSystemCode,
        String conceptCode,
        String conceptMnemonic,
        String label,
        String calculationType,
        String functionalNature,
        String executionScope,
        String payslipOrderCode,
        String summary
) {
}
