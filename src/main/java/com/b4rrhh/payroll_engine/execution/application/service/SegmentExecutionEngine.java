package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.ConceptExecutionPlanEntry;
import com.b4rrhh.payroll_engine.execution.domain.model.SegmentExecutionState;
import com.b4rrhh.payroll_engine.segment.domain.model.SegmentCalculationContext;

import java.math.BigDecimal;
import java.util.List;

/**
 * Port: evaluates concepts against one calculation state and one calculation context.
 *
 * <p>The execution plan must be provided in topological order: all dependencies of a concept
 * must appear before that concept in the list.
 *
 * <p>Each concept is evaluated exactly once per state. Its result is stored in
 * {@link SegmentExecutionState} and is available to subsequent concepts that depend on it.
 *
 * <p>The engine does not know whether the state it is given belongs to one temporal segment
 * or to the whole period: that is decided by the concept's
 * {@link com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope} and orchestrated by
 * the caller (ADR-058). A {@code SEGMENT} concept is evaluated once per segment, against that
 * segment's state and context; a {@code PERIOD} concept is evaluated once, against the period
 * state and a context that spans the whole period. Composing segment results into the period
 * state is the caller's responsibility, and it is always a sum.
 *
 * <h3>In-memory execution</h3>
 * <p>Plan entries for {@code RATE_BY_QUANTITY} concepts carry pre-resolved operand wiring
 * (QUANTITY and RATE source identities) embedded by the plan builder. Evaluation
 * performs no repository access and no graph traversal.
 */
public interface SegmentExecutionEngine {

    /**
     * Evaluates one plan entry against the given state and context, without storing the result.
     *
     * @param entry   the concept to evaluate; its operands and aggregate sources must already be
     *                stored in {@code state}
     * @param state   amounts of the concepts evaluated so far for this segment (or period)
     * @param context calculation context providing technical values for this segment (or period)
     * @return the calculated amount
     */
    BigDecimal evaluate(
            ConceptExecutionPlanEntry entry,
            SegmentExecutionState state,
            SegmentCalculationContext context);

    /**
     * Evaluates a whole plan against a fresh state, storing each result as it goes.
     *
     * @param plan    concepts to execute, in topological order (dependencies before dependents);
     *                RATE_BY_QUANTITY entries must carry pre-resolved operand wiring
     * @param context calculation context providing technical values
     * @return state containing all calculated concept amounts
     */
    SegmentExecutionState execute(
            List<ConceptExecutionPlanEntry> plan,
            SegmentCalculationContext context);
}
