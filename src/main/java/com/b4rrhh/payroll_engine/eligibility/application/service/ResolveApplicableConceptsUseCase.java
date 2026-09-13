package com.b4rrhh.payroll_engine.eligibility.application.service;

import com.b4rrhh.payroll_engine.eligibility.domain.model.EmployeeAssignmentContext;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ResolvedConceptAssignment;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;

import java.util.List;

/**
 * Input port for resolving the set of applicable payroll concepts for a given
 * employee context, against the metamodel loaded for the execution.
 *
 * <p>The returned list contains exactly one {@link ResolvedConceptAssignment} per
 * applicable concept (the winner of the priority resolution).
 *
 * <p>Results are sorted by:
 * <ol>
 *   <li>priority descending</li>
 *   <li>conceptCode ascending (for deterministic output)</li>
 * </ol>
 *
 * @throws com.b4rrhh.payroll_engine.eligibility.domain.exception.DuplicateConceptAssignmentException
 *         if two assignments for the same conceptCode share the same highest priority
 */
public interface ResolveApplicableConceptsUseCase {

    /**
     * @param context   the employee dimensions the assignments are matched against
     * @param metamodel the rule system's assignments as loaded when the execution started;
     *                  it also carries the reference date their validity was resolved on
     */
    List<ResolvedConceptAssignment> resolve(EmployeeAssignmentContext context, RuleSystemMetamodel metamodel);
}
