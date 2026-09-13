package com.b4rrhh.payroll_engine.eligibility.domain.port;

import com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment;
import com.b4rrhh.payroll_engine.eligibility.domain.model.EmployeeAssignmentContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Output port for persisting and querying {@link ConceptAssignment} records.
 *
 * <h3>Wildcard matching semantics for {@code findApplicableAssignments}</h3>
 * <p>Returns all candidate assignments that:
 * <ul>
 *   <li>have {@code ruleSystemCode} equal to {@code context.ruleSystemCode}</li>
 *   <li>are valid on {@code referenceDate}: {@code validFrom <= referenceDate}
 *       AND ({@code validTo IS NULL} OR {@code validTo >= referenceDate})</li>
 *   <li>match each optional dimension using the following rule:
 *       <strong>assignment dimension NULL = wildcard (matches any context value);
 *       assignment dimension non-null = must equal the context value exactly.</strong></li>
 * </ul>
 *
 * <h3>Null-context semantics (important)</h3>
 * <p>A null value in the <em>context</em> means the dimension is unknown/unspecified.
 * It does NOT act as a wildcard on the assignment side. Concretely:
 * <ul>
 *   <li>If {@code context.companyCode} is null, only assignments whose
 *       {@code companyCode} is also null (wildcard) will be returned.
 *       An assignment with {@code companyCode = 'EMP1'} will be <strong>excluded</strong>.</li>
 * </ul>
 * The same rule applies to {@code agreementCode} and {@code employeeTypeCode}.
 *
 * <p>Precedence resolution (choosing among candidates for the same {@code conceptCode})
 * is performed in the application service, not in the repository.
 */
public interface ConceptAssignmentRepository {

    ConceptAssignment save(ConceptAssignment assignment);

    /**
     * Returns all candidate assignments matching the given employee context and reference date,
     * using wildcard semantics for optional dimensions.
     */
    List<ConceptAssignment> findApplicableAssignments(EmployeeAssignmentContext context, LocalDate referenceDate);

    /**
     * Returns every concept assignment of the rule system that is valid on the reference date,
     * without filtering by the employee dimensions. Wildcard matching is then applied in memory
     * by the execution metamodel: this is the bulk read it is loaded with, one call per
     * execution instead of one {@link #findApplicableAssignments} per unit.
     */
    List<ConceptAssignment> findAllValidByRuleSystemCode(String ruleSystemCode, LocalDate referenceDate);

    /**
     * Returns every concept assignment registered under the given rule system, ordered by
     * concept code and priority. Returns an empty list when no assignment is found.
     */
    List<ConceptAssignment> findAllByRuleSystemCode(String ruleSystemCode);

    /**
     * Returns every concept assignment registered under the given rule system whose
     * {@code conceptCode} matches the supplied value, ordered by priority.
     * Returns an empty list when no assignment is found.
     */
    List<ConceptAssignment> findAllByRuleSystemCodeAndConceptCode(String ruleSystemCode, String conceptCode);

    /**
     * Removes the assignment identified by its surrogate id, if present. The operation is a
     * no-op when the id is null or does not match an existing row.
     *
     * <p>NOTE: the OpenAPI contract for the payroll designer feature exposes assignments via
     * an opaque {@code assignmentCode} (UUID business key). Once the {@link ConceptAssignment}
     * domain model gains an {@code assignmentCode} field, a {@code deleteByAssignmentCode}
     * variant should be added; the controller cannot use the surrogate id directly.
     */
    Optional<ConceptAssignment> findByIdAndRuleSystemCode(Long id, String ruleSystemCode);

    void deleteById(Long id);

    /**
     * Returns true when an assignment with the given id exists under the supplied rule system.
     * Used by the management layer to decide between 204 and 404 responses without fetching
     * the full aggregate.
     */
    boolean existsByIdAndRuleSystemCode(Long id, String ruleSystemCode);
}
