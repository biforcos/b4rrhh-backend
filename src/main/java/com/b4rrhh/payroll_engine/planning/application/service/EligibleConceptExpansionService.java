package com.b4rrhh.payroll_engine.planning.application.service;

import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import com.b4rrhh.payroll_engine.planning.domain.exception.MissingDependencyConceptDefinitionException;

import java.util.List;

/**
 * Expands a pre-loaded set of directly eligible concepts by transitively discovering
 * and loading all structural dependencies within the same rule system.
 *
 * <h3>Expansion semantics</h3>
 * <p>Starting from the given eligible concepts, this service follows active feed relations
 * to discover which additional concepts are structurally required for calculation.
 * The process is iterative: newly discovered dependency concepts are themselves expanded
 * until no new concepts are found.
 *
 * <h3>Rule-system boundary</h3>
 * <p>Only dependencies within the same {@code ruleSystemCode} as the input concepts
 * are followed. Cross-rule-system feed relations are silently skipped.
 *
 * <h3>Return value</h3>
 * <p>Returns the full expanded set, including the input {@code eligibleConcepts}.
 *
 * @throws MissingDependencyConceptDefinitionException if a discovered dependency has no
 *         matching concept definition in the metamodel
 */
public interface EligibleConceptExpansionService {

    /**
     * Expands the given eligible concepts by walking all required transitive dependencies.
     *
     * @param eligibleConcepts the pre-loaded directly applicable concepts (must not be null)
     * @param metamodel        the execution's rule system metamodel: it supplies the concept
     *                         definitions, the operands and the feed relations already filtered
     *                         to the execution's reference date
     * @return the full expanded concept set in discovery order, including the input concepts
     */
    List<PayrollConcept> expand(List<PayrollConcept> eligibleConcepts, RuleSystemMetamodel metamodel);
}
