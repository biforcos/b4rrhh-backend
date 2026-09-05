package com.b4rrhh.payroll_engine.concept.domain.exception;

import com.b4rrhh.payroll_engine.concept.domain.model.OperandRole;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;

/**
 * A PERIOD concept declared an operand whose source is a SEGMENT concept (ADR-058).
 *
 * <p>The message names the operand role and both concepts, so the caller can see which
 * edge was refused without re-reading the graph.
 */
public class OperandCrossesSegmentToPeriodException extends RuntimeException {

    private final OperandRole operandRole;
    private final PayrollConcept target;
    private final PayrollConcept source;

    public OperandCrossesSegmentToPeriodException(
            OperandRole operandRole, PayrollConcept target, PayrollConcept source) {
        super("Operand " + operandRole + " of " + target.getExecutionScope() + " concept "
                + describe(target) + " cannot read " + source.getExecutionScope() + " concept "
                + describe(source) + ": no operand crosses from SEGMENT to PERIOD (ADR-058); "
                + "a feed relation may");
        this.operandRole = operandRole;
        this.target = target;
        this.source = source;
    }

    private static String describe(PayrollConcept concept) {
        return concept.getRuleSystemCode() + "/" + concept.getConceptCode()
                + " (" + concept.getConceptMnemonic() + ")";
    }

    public OperandRole getOperandRole() { return operandRole; }
    public PayrollConcept getTarget() { return target; }
    public PayrollConcept getSource() { return source; }
}
