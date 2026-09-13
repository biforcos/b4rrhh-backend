package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptDependencyGraph;
import com.b4rrhh.payroll_engine.execution.domain.model.ConceptExecutionPlanEntry;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;

import java.util.List;

/**
 * Translates a structural {@link ConceptDependencyGraph} into an ordered
 * {@link ConceptExecutionPlanEntry} list ready for {@link SegmentExecutionEngine}.
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>Output order is determined by {@code graph.topologicalOrder()}.
 *       Dependencies appear before the concepts that depend on them.</li>
 *   <li>Output includes exactly the nodes present in the graph — no more, no less.</li>
 *   <li>Each entry's {@code CalculationType} is sourced from the matching
 *       {@code PayrollConcept} in the supplied list, matched by
 *       {@code (ruleSystemCode, conceptCode)}.</li>
 *   <li>If a graph node has no matching concept, fail with
 *       {@link com.b4rrhh.payroll_engine.execution.domain.exception.MissingConceptDefinitionException}.</li>
 *   <li>If the concept list contains duplicate identities, fail with
 *       {@link com.b4rrhh.payroll_engine.execution.domain.exception.DuplicateConceptIdentityException}.</li>
 * </ul>
 */
public interface ExecutionPlanBuilder {

    /**
     * Builds an ordered execution plan from the given graph and concept definitions.
     *
     * @param graph     the dependency graph defining structural order and nodes
     * @param concepts  the concept definitions providing {@code CalculationType}
     * @param metamodel the execution's metamodel, supplying operand definitions and the
     *                  feed relations already filtered to the execution's reference date
     * @return entries in topological order, one per graph node
     */
    List<ConceptExecutionPlanEntry> build(ConceptDependencyGraph graph, List<PayrollConcept> concepts, RuleSystemMetamodel metamodel);
}
