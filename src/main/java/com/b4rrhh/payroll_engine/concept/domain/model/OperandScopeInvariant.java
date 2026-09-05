package com.b4rrhh.payroll_engine.concept.domain.model;

import com.b4rrhh.payroll_engine.concept.domain.exception.OperandCrossesSegmentToPeriodException;

/**
 * No operand crosses from SEGMENT to PERIOD (ADR-058).
 *
 * <p>The graph has two kinds of edge and they do not mean the same thing. A feed relation
 * from a SEGMENT concept into a PERIOD one <em>adds up</em>, and adding up is defined:
 * 15 days + 15 days = 30. An operand ({@link OperandRole}: quantity, rate, base,
 * percentage, left, right) read by a PERIOD concept from a SEGMENT one has no answer —
 * "the daily rate" in a month with two rates is not a number. So the rule is on the edge,
 * not on the concept, and it is one-directional: a SEGMENT concept reading a PERIOD value
 * is fine, because that value is a single number, the same in every segment.
 *
 * <p>This is checked when the graph is saved and by a test that walks the seeded graph.
 * A rule that only lives in the ADR protects whoever remembers it.
 */
public final class OperandScopeInvariant {

    private OperandScopeInvariant() {
    }

    /** True when {@code target} is PERIOD and {@code source} is SEGMENT: the forbidden edge. */
    public static boolean crossesSegmentToPeriod(PayrollConcept target, PayrollConcept source) {
        return target.getExecutionScope() == ExecutionScope.PERIOD
                && source.getExecutionScope() == ExecutionScope.SEGMENT;
    }

    /**
     * Rejects the operand edge {@code target.role <- source} when it crosses from SEGMENT
     * to PERIOD.
     *
     * @throws OperandCrossesSegmentToPeriodException naming the role and both concepts
     */
    public static void check(PayrollConcept target, OperandRole operandRole, PayrollConcept source) {
        if (crossesSegmentToPeriod(target, source)) {
            throw new OperandCrossesSegmentToPeriodException(operandRole, target, source);
        }
    }
}
