package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SpringDataPayrollConceptFeedRelationRepository
        extends JpaRepository<PayrollConceptFeedRelationEntity, Long> {

    @Query("""
        select r from PayrollEngineFeedRelationEntity r
        where r.targetObject.id = :targetObjectId
          and r.effectiveFrom <= :referenceDate
          and (r.effectiveTo is null or r.effectiveTo >= :referenceDate)
        """)
    List<PayrollConceptFeedRelationEntity> findActiveByTargetObjectId(
            @Param("targetObjectId") Long targetObjectId,
            @Param("referenceDate") LocalDate referenceDate
    );

    @Query("""
        select r from PayrollEngineFeedRelationEntity r
        where r.targetObject.ruleSystemCode = :ruleSystemCode
          and r.targetObject.objectCode = :conceptCode
        order by r.sourceObject.objectCode asc
        """)
    List<PayrollConceptFeedRelationEntity> findByRuleSystemCodeAndTargetConceptCode(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("conceptCode") String conceptCode
    );

    /**
     * Todas las alimentaciones del sistema de reglas vigentes en la fecha dada, con sus dos
     * objetos en la misma consulta. Es la carga del metamodelo de una ejecución: sustituye a
     * una llamada a {@link #findActiveByTargetObjectId} por concepto y por unidad (backend#87).
     */
    @Query("""
        select r from PayrollEngineFeedRelationEntity r
        join fetch r.targetObject t
        join fetch r.sourceObject
        where t.ruleSystemCode = :ruleSystemCode
          and r.effectiveFrom <= :referenceDate
          and (r.effectiveTo is null or r.effectiveTo >= :referenceDate)
        order by t.objectCode asc, r.id asc
        """)
    List<PayrollConceptFeedRelationEntity> findAllActiveByRuleSystemCodeFetchingObjects(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("referenceDate") LocalDate referenceDate
    );

    /**
     * Todas las alimentaciones del sistema de reglas, vigentes o no, con sus dos objetos en la
     * misma consulta. Es la lectura del grafo del disenador (designer#15).
     */
    @Query("""
        select r from PayrollEngineFeedRelationEntity r
        join fetch r.targetObject t
        join fetch r.sourceObject
        where t.ruleSystemCode = :ruleSystemCode
        order by t.objectCode asc, r.id asc
        """)
    List<PayrollConceptFeedRelationEntity> findAllByRuleSystemCodeFetchingObjects(
            @Param("ruleSystemCode") String ruleSystemCode
    );

    @Modifying
    @Transactional
    @Query("""
        delete from PayrollEngineFeedRelationEntity r
        where r.targetObject.ruleSystemCode = :ruleSystemCode
          and r.targetObject.objectCode = :conceptCode
        """)
    void deleteAllByRuleSystemCodeAndTargetConceptCode(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("conceptCode") String conceptCode
    );
}
