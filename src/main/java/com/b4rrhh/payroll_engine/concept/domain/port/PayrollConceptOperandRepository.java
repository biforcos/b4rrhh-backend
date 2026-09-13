package com.b4rrhh.payroll_engine.concept.domain.port;

import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;

import java.util.List;

public interface PayrollConceptOperandRepository {

    PayrollConceptOperand save(PayrollConceptOperand operand);

    /**
     * Returns all operand definitions where the target concept matches the given business key.
     *
     * @deprecated Deprecated — use {@link #findByRuleSystemCodeAndConceptCode} instead.
     */
    @Deprecated
    List<PayrollConceptOperand> findByTarget(String ruleSystemCode, String conceptCode);

    /**
     * Returns every operand defined for the concept identified by the given business key,
     * ordered by operand role. Returns an empty list when no operand is configured.
     *
     * <p>Equivalent to {@link #findByTarget(String, String)}; exposed under a more explicit
     * naming for the wiring management endpoints so that adapters/handlers can express
     * intent without overloading the original execution-time helper.
     */
    List<PayrollConceptOperand> findByRuleSystemCodeAndConceptCode(String ruleSystemCode, String conceptCode);

    /**
     * Returns every operand whose target concept belongs to the given rule system, ordered by
     * concept code and then operand role. This is the bulk read the execution metamodel is
     * loaded with: one call per execution instead of one per concept and unit.
     */
    List<PayrollConceptOperand> findAllByRuleSystemCode(String ruleSystemCode);

    /**
     * Removes every operand attached to the concept identified by the given business key.
     * The operation is a no-op when no operand exists; it never raises.
     */
    void deleteAllByRuleSystemCodeAndConceptCode(String ruleSystemCode, String conceptCode);
}
