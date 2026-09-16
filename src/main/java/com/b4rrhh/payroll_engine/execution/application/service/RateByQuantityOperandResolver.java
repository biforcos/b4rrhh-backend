package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.concept.domain.model.OperandRole;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptNodeIdentity;
import com.b4rrhh.payroll_engine.execution.domain.exception.MissingPlannedOperandException;
import com.b4rrhh.payroll_engine.execution.domain.model.ConceptExecutionPlanEntry;
import com.b4rrhh.payroll_engine.execution.domain.model.SegmentExecutionState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Computes the result of a {@code RATE_BY_QUANTITY} execution from pre-resolved operand
 * wiring embedded in the plan entry and pre-computed amounts in the segment execution state.
 *
 * <h3>Contract</h3>
 * <p>Operand source identities (QUANTITY and RATE) must have been resolved and embedded into
 * the {@link ConceptExecutionPlanEntry#operands()} map by
 * {@link DefaultExecutionPlanBuilder} at plan-construction time. Per-segment execution
 * performs no repository access and no graph traversal.
 *
 * <h3>Fail-fast contract</h3>
 * <ul>
 *   <li>If the plan entry lacks operand wiring for QUANTITY or RATE, {@link MissingPlannedOperandException}
 *       is thrown — this indicates a plan-construction defect.</li>
 *   <li>If a required source amount is absent from state, {@link
 *       com.b4rrhh.payroll_engine.execution.domain.exception.MissingConceptResultException} is thrown via
 *       {@link SegmentExecutionState#getRequiredAmount} — this indicates the plan was not
 *       in topological order.</li>
 * </ul>
 *
 * <h3>Rounding</h3>
 * <p><b>No redondea.</b> Devuelve el producto exacto; el redondeo lo aplica una sola vez el motor
 * con los decimales que el concepto declara (backend#61, ADR-066). Aqui habia un
 * {@code setScale(2, HALF_UP)} que era uno de los tres sitios donde se redondeaba a ciegas.
 */
@Component
public class RateByQuantityOperandResolver {

    /**
     * Computes quantity × rate for the given plan entry using amounts from execution state.
     *
     * @param entry plan entry for the RATE_BY_QUANTITY concept; must carry QUANTITY and RATE
     *              operand wiring in {@link ConceptExecutionPlanEntry#operands()}
     * @param state current segment state; must already contain source concept amounts
     * @return quantity × rate, exacto y sin redondear
     * @throws MissingPlannedOperandException if operand wiring is absent from the entry
     */
    public BigDecimal resolve(ConceptExecutionPlanEntry entry, SegmentExecutionState state) {
        ConceptNodeIdentity quantityId = getPlannedOperand(entry, OperandRole.QUANTITY);
        ConceptNodeIdentity rateId     = getPlannedOperand(entry, OperandRole.RATE);

        BigDecimal quantity = state.getRequiredAmount(quantityId);
        BigDecimal rate     = state.getRequiredAmount(rateId);

        return quantity.multiply(rate);
    }

    private ConceptNodeIdentity getPlannedOperand(ConceptExecutionPlanEntry entry, OperandRole role) {
        ConceptNodeIdentity source = entry.operands().get(role);
        if (source == null) {
            throw new MissingPlannedOperandException(entry.identity(), role);
        }
        return source;
    }
}
