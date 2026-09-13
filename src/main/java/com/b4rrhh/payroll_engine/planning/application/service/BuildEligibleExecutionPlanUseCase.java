package com.b4rrhh.payroll_engine.planning.application.service;

import com.b4rrhh.payroll_engine.eligibility.domain.model.EmployeeAssignmentContext;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import com.b4rrhh.payroll_engine.planning.domain.model.EligibleExecutionPlanResult;

/**
 * Input port for building an eligible execution plan from an employee context and the
 * metamodel loaded for the execution.
 *
 * <p>The result includes all intermediate layers (applicable assignments, eligible concepts,
 * expanded concepts, dependency graph, execution plan) for full auditability.
 *
 * <p>This use case integrates:
 * <ol>
 *   <li>Eligibility resolution — which concepts apply from a business perspective.</li>
 *   <li>Concept definition loading — the structural definition of each applicable concept.</li>
 *   <li>Dependency expansion — transitive inclusion of all structurally required upstream
 *       concepts (e.g. technical concepts such as T_DIAS_PRESENCIA_SEGMENTO).</li>
 *   <li>Execution plan construction — topologically ordered plan ready for the execution engine.</li>
 * </ol>
 *
 * <p>This use case does NOT execute the payroll calculation. It only prepares the plan.
 */
public interface BuildEligibleExecutionPlanUseCase {

    /**
     * Builds the eligible execution plan for the given context against the execution's metamodel.
     *
     * <p>There is no reference date parameter on purpose: the date is the one the metamodel was
     * loaded with. Two units of the same execution cannot be planned against different dates
     * because there is nowhere left to say a different one.
     *
     * @param context   the employee context carrying rule system and optional scope dimensions
     * @param metamodel the rule system metamodel loaded when the execution started
     * @return an auditable result with all intermediate layers
     */
    EligibleExecutionPlanResult build(EmployeeAssignmentContext context, RuleSystemMetamodel metamodel);
}
