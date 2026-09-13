package com.b4rrhh.payroll_engine.dependency.application.service;

import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptDependencyGraph;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;

import java.util.List;

/**
 * Application service that builds a {@link ConceptDependencyGraph} from a list of
 * {@link PayrollConcept}s and their feed relations.
 *
 * <p>Feed relations are time-bounded (effective-from / effective-to), so only those
 * active on a reference date belong in the graph. That date is not a parameter here:
 * it is the one the {@link RuleSystemMetamodel} was loaded with, and the metamodel
 * already holds only the relations active on it.
 *
 * <p>Only concepts present in the input list become nodes. Relations whose
 * source concept is not in the input list are silently ignored — the execution
 * plan builder will fail explicitly if a required dependency is absent.
 */
public interface ConceptDependencyGraphService {

    /**
     * Builds a dependency graph from the given concepts and their active feed relations.
     *
     * @param concepts  the full set of concepts to include as graph nodes
     * @param metamodel the execution's metamodel, supplying the active feed relations and
     *                  the operand definitions the edges are built from
     * @return a validated, cycle-free {@link ConceptDependencyGraph}
     */
    ConceptDependencyGraph build(List<PayrollConcept> concepts, RuleSystemMetamodel metamodel);
}
