package com.b4rrhh.payroll_engine.planning.application.service;

import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.dependency.application.service.ConceptDependencyGraphService;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptDependencyGraph;
import com.b4rrhh.payroll_engine.eligibility.application.service.ResolveApplicableConceptsUseCase;
import com.b4rrhh.payroll_engine.eligibility.domain.model.EmployeeAssignmentContext;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ResolvedConceptAssignment;
import com.b4rrhh.payroll_engine.execution.application.service.ExecutionPlanBuilder;
import com.b4rrhh.payroll_engine.execution.domain.model.ConceptExecutionPlanEntry;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import com.b4rrhh.payroll_engine.planning.domain.exception.MissingEligibleConceptDefinitionException;
import com.b4rrhh.payroll_engine.planning.domain.model.EligibleExecutionPlanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link BuildEligibleExecutionPlanUseCase}.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li><strong>Resolve applicable assignments</strong> — calls {@link ResolveApplicableConceptsUseCase}
 *       to obtain one winning assignment per applicable concept code for the given context
 *       and reference date.</li>
 *   <li><strong>Take eligible concept definitions</strong> — reads the concept definitions
 *       for all resolved codes from the execution's metamodel. Fails fast with
 *       {@link MissingEligibleConceptDefinitionException} if any code lacks a definition.</li>
 *   <li><strong>Expand dependencies</strong> — delegates to {@link EligibleConceptExpansionService}
 *       to iteratively discover and load all structural dependencies within the same rule system.
 *       Technical concepts (e.g. T_DIAS_PRESENCIA_SEGMENTO) are pulled in here, not via
 *       eligibility assignments.</li>
 *   <li><strong>Build dependency graph</strong> — calls {@link ConceptDependencyGraphService}
 *       over the full expanded concept set. At this point, all nodes are present, so no
 *       feed relation edge will be silently dropped.</li>
 *   <li><strong>Build execution plan</strong> — calls {@link ExecutionPlanBuilder} to produce
 *       the topologically ordered plan.</li>
 *   <li><strong>Return auditable result</strong> — wraps all intermediate layers into
 *       {@link EligibleExecutionPlanResult}.</li>
 * </ol>
 *
 * <h3>Design note</h3>
 * <p>Eligibility decides WHICH concepts apply from a business perspective.
 * Dependency expansion decides WHICH additional concepts are structurally required for calculation.
 * These concerns are intentionally kept separate.
 */
@Service
public class DefaultEligibleExecutionPlanBuilder implements BuildEligibleExecutionPlanUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultEligibleExecutionPlanBuilder.class);

    private final ResolveApplicableConceptsUseCase eligibilityResolver;
    private final EligibleConceptExpansionService expansionService;
    private final ConceptDependencyGraphService graphService;
    private final ExecutionPlanBuilder planBuilder;

    public DefaultEligibleExecutionPlanBuilder(
            ResolveApplicableConceptsUseCase eligibilityResolver,
            EligibleConceptExpansionService expansionService,
            ConceptDependencyGraphService graphService,
            ExecutionPlanBuilder planBuilder
    ) {
        this.eligibilityResolver = eligibilityResolver;
        this.expansionService = expansionService;
        this.graphService = graphService;
        this.planBuilder = planBuilder;
    }

    @Override
    public EligibleExecutionPlanResult build(EmployeeAssignmentContext context, RuleSystemMetamodel metamodel) {
        metamodel.requireSameRuleSystem(context.getRuleSystemCode());
        log.debug("[ENGINE] ── Paso 1/5 ELEGIBILIDAD | RS={} empresa={} convenio={} tipo={} ref={}",
                context.getRuleSystemCode(), context.getCompanyCode(),
                context.getAgreementCode(), context.getEmployeeTypeCode(), metamodel.referenceDate());

        List<ResolvedConceptAssignment> applicableAssignments =
                eligibilityResolver.resolve(context, metamodel);

        Set<String> eligibleCodes = applicableAssignments.stream()
                .map(ResolvedConceptAssignment::getConceptCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        log.debug("[ENGINE] ✓ Paso 1/5 | {} conceptos elegibles → [{}]",
                eligibleCodes.size(), String.join(", ", eligibleCodes));

        log.debug("[ENGINE] ── Paso 2/5 DEFINICIONES | tomando {} definiciones de concepto", eligibleCodes.size());
        List<PayrollConcept> eligibleConcepts = metamodel.findConcepts(eligibleCodes);

        Set<String> foundCodes = eligibleConcepts.stream()
                .map(PayrollConcept::getConceptCode)
                .collect(Collectors.toSet());
        for (String code : eligibleCodes) {
            if (!foundCodes.contains(code)) {
                throw new MissingEligibleConceptDefinitionException(context.getRuleSystemCode(), code);
            }
        }
        log.debug("[ENGINE] ✓ Paso 2/5 | {} definiciones cargadas: [{}]",
                eligibleConcepts.size(),
                eligibleConcepts.stream()
                        .map(c -> c.getConceptCode() + "(" + c.getCalculationType() + ")")
                        .collect(Collectors.joining(", ")));

        log.debug("[ENGINE] ── Paso 3/5 EXPANSIÓN BFS | descubriendo dependencias transitivas");
        List<PayrollConcept> expandedConcepts = expansionService.expand(eligibleConcepts, metamodel);
        log.debug("[ENGINE] ✓ Paso 3/5 | {} conceptos tras expansión → [{}]",
                expandedConcepts.size(),
                expandedConcepts.stream().map(PayrollConcept::getConceptCode).collect(Collectors.joining(", ")));

        log.debug("[ENGINE] ── Paso 4/5 GRAFO | construyendo grafo de dependencias sobre {} nodos", expandedConcepts.size());
        ConceptDependencyGraph dependencyGraph = graphService.build(expandedConcepts, metamodel);
        log.debug("[ENGINE] ✓ Paso 4/5 | grafo construido");

        log.debug("[ENGINE] ── Paso 5/5 PLAN | ordenación topológica y wiring de operandos");
        List<ConceptExecutionPlanEntry> executionPlan = planBuilder.build(dependencyGraph, expandedConcepts, metamodel);
        log.debug("[ENGINE] ✓ Paso 5/5 | {} entradas en plan → [{}]",
                executionPlan.size(),
                executionPlan.stream().map(e -> e.identity().getConceptCode()).collect(Collectors.joining(" → ")));

        return new EligibleExecutionPlanResult(
                applicableAssignments,
                List.copyOf(eligibleConcepts),
                List.copyOf(expandedConcepts),
                dependencyGraph,
                executionPlan
        );
    }
}
