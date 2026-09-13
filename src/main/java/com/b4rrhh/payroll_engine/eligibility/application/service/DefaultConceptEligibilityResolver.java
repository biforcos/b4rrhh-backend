package com.b4rrhh.payroll_engine.eligibility.application.service;

import com.b4rrhh.payroll_engine.eligibility.domain.exception.DuplicateConceptAssignmentException;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment;
import com.b4rrhh.payroll_engine.eligibility.domain.model.EmployeeAssignmentContext;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ResolvedConceptAssignment;
import com.b4rrhh.payroll_engine.eligibility.domain.port.ConceptAssignmentRepository;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ResolveApplicableConceptsUseCase}.
 *
 * <h3>Resolution algorithm</h3>
 * <ol>
 *   <li>Take the candidate assignments from the execution's {@link RuleSystemMetamodel},
 *       which applies wildcard matching on optional dimensions over the assignments already
 *       filtered by validity date when it was loaded. Before backend#87 this was one
 *       {@link ConceptAssignmentRepository} query per calculation unit.</li>
 *   <li>Group candidates by {@code conceptCode}.</li>
 *   <li>For each {@code conceptCode} group, find the maximum priority. If more than one
 *       assignment shares that maximum priority, fail fast with
 *       {@link DuplicateConceptAssignmentException}.</li>
 *   <li>Return the winning assignments as {@link ResolvedConceptAssignment} objects,
 *       sorted by priority descending then conceptCode ascending.</li>
 * </ol>
 */
@Service
public class DefaultConceptEligibilityResolver implements ResolveApplicableConceptsUseCase {

    @Override
    public List<ResolvedConceptAssignment> resolve(EmployeeAssignmentContext context, RuleSystemMetamodel metamodel) {
        List<ConceptAssignment> candidates = metamodel.applicableAssignments(context);

        Map<String, List<ConceptAssignment>> byConceptCode = candidates.stream()
                .collect(Collectors.groupingBy(ConceptAssignment::getConceptCode));

        List<ResolvedConceptAssignment> resolved = new ArrayList<>();

        for (Map.Entry<String, List<ConceptAssignment>> entry : byConceptCode.entrySet()) {
            String conceptCode = entry.getKey();
            List<ConceptAssignment> group = entry.getValue();

            int maxPriority = group.stream()
                    .mapToInt(ConceptAssignment::getPriority)
                    .max()
                    .orElseThrow();

            List<ConceptAssignment> winners = group.stream()
                    .filter(a -> a.getPriority() == maxPriority)
                    .toList();

            if (winners.size() > 1) {
                throw new DuplicateConceptAssignmentException(context.getRuleSystemCode(), conceptCode, maxPriority);
            }

            ConceptAssignment winner = winners.get(0);
            resolved.add(new ResolvedConceptAssignment(
                    conceptCode,
                    maxPriority,
                    winner.getRuleSystemCode(),
                    winner.getCompanyCode(),
                    winner.getAgreementCode(),
                    winner.getEmployeeTypeCode()
            ));
        }

        resolved.sort(
                Comparator.comparingInt(ResolvedConceptAssignment::getWinningPriority).reversed()
                        .thenComparing(ResolvedConceptAssignment::getConceptCode)
        );

        return resolved;
    }
}
