package com.b4rrhh.payroll_engine.eligibility.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataConceptAssignmentRepository extends JpaRepository<ConceptAssignmentEntity, Long> {

    /**
     * Returns all concept assignment entities valid on the reference date and matching the
     * given context dimensions using the following wildcard semantics:
     *
     * <ul>
     *   <li>{@code assignment.companyCode IS NULL} → wildcard, matches any context value
     *       including null.</li>
     *   <li>{@code assignment.companyCode IS NOT NULL} → must equal the context value exactly;
     *       if the context value is null (unknown), the assignment is <strong>excluded</strong>.</li>
     * </ul>
     *
     * <p>The same rule applies to {@code agreementCode} and {@code employeeTypeCode}.
     *
     * <p>The JPQL expresses this explicitly as:
     * {@code (a.dim IS NULL OR (:param IS NOT NULL AND a.dim = :param))} — removing
     * any ambiguity around Hibernate's treatment of null parameter equality.
     */
    @Query("""
            SELECT a FROM PayrollEngineConceptAssignmentEntity a
            WHERE a.ruleSystemCode = :ruleSystemCode
              AND a.validFrom <= :referenceDate
              AND (a.validTo IS NULL OR a.validTo >= :referenceDate)
              AND (a.companyCode IS NULL OR (:companyCode IS NOT NULL AND a.companyCode = :companyCode))
              AND (a.agreementCode IS NULL OR (:agreementCode IS NOT NULL AND a.agreementCode = :agreementCode))
              AND (a.employeeTypeCode IS NULL OR (:employeeTypeCode IS NOT NULL AND a.employeeTypeCode = :employeeTypeCode))
            """)
    List<ConceptAssignmentEntity> findCandidates(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("referenceDate") LocalDate referenceDate,
            @Param("companyCode") String companyCode,
            @Param("agreementCode") String agreementCode,
            @Param("employeeTypeCode") String employeeTypeCode
    );

    /**
     * Todas las asignaciones del sistema de reglas vigentes en la fecha dada, sin filtrar por
     * las dimensiones del empleado: el filtrado por comodín lo hace el metamodelo en memoria,
     * una vez cargado, en lugar de una consulta por unidad de cálculo (backend#87).
     */
    @Query("""
            SELECT a FROM PayrollEngineConceptAssignmentEntity a
            WHERE a.ruleSystemCode = :ruleSystemCode
              AND a.validFrom <= :referenceDate
              AND (a.validTo IS NULL OR a.validTo >= :referenceDate)
            ORDER BY a.conceptCode ASC, a.priority DESC, a.id ASC
            """)
    List<ConceptAssignmentEntity> findAllValidByRuleSystemCode(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("referenceDate") LocalDate referenceDate
    );

    @Query("""
            SELECT a FROM PayrollEngineConceptAssignmentEntity a
            WHERE a.ruleSystemCode = :ruleSystemCode
            ORDER BY a.conceptCode ASC, a.priority DESC
            """)
    List<ConceptAssignmentEntity> findAllByRuleSystemCode(
            @Param("ruleSystemCode") String ruleSystemCode
    );

    @Query("""
            SELECT a FROM PayrollEngineConceptAssignmentEntity a
            WHERE a.ruleSystemCode = :ruleSystemCode
              AND a.conceptCode = :conceptCode
            ORDER BY a.priority DESC
            """)
    List<ConceptAssignmentEntity> findAllByRuleSystemCodeAndConceptCode(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("conceptCode") String conceptCode
    );

    boolean existsByIdAndRuleSystemCode(Long id, String ruleSystemCode);

    Optional<ConceptAssignmentEntity> findByIdAndRuleSystemCode(Long id, String ruleSystemCode);
}
