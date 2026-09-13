package com.b4rrhh.payroll.application.service;

import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;

public interface PayrollConceptGraphCalculator {

    /**
     * @param metamodel the rule system metamodel loaded for the execution: the concepts and
     *                  their feeds come from it, so every unit of the run resolves a
     *                  DIRECT_AMOUNT against the same rules
     */
    PayrollConceptExecutionResult calculateConceptResult(
            String conceptCode,
            PayrollConceptExecutionContext context,
            RuleSystemMetamodel metamodel);
}