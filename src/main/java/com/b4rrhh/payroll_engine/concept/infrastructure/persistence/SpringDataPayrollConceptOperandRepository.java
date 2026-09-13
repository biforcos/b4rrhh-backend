package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataPayrollConceptOperandRepository
        extends JpaRepository<PayrollConceptOperandEntity, Long> {

    List<PayrollConceptOperandEntity> findByTargetObject_RuleSystemCodeAndTargetObject_ObjectCode(
            String ruleSystemCode, String objectCode);

    @Query("""
            select o from PayrollEngineConceptOperandEntity o
            where o.targetObject.ruleSystemCode = :ruleSystemCode
              and o.targetObject.objectCode = :conceptCode
            order by o.operandRole asc
            """)
    List<PayrollConceptOperandEntity> findByRuleSystemCodeAndConceptCode(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("conceptCode") String conceptCode
    );

    /**
     * Todos los operandos del sistema de reglas, con su objeto destino y su objeto origen
     * en la misma consulta. Es la carga del metamodelo de una ejecución: una consulta en
     * lugar de una por concepto más un select por objeto (backend#87).
     */
    @Query("""
            select o from PayrollEngineConceptOperandEntity o
            join fetch o.targetObject t
            join fetch o.sourceObject
            where t.ruleSystemCode = :ruleSystemCode
            order by t.objectCode asc, o.operandRole asc
            """)
    List<PayrollConceptOperandEntity> findAllByRuleSystemCodeFetchingObjects(
            @Param("ruleSystemCode") String ruleSystemCode
    );

    @Modifying
    @Transactional
    @Query("""
            delete from PayrollEngineConceptOperandEntity o
            where o.targetObject.ruleSystemCode = :ruleSystemCode
              and o.targetObject.objectCode = :conceptCode
            """)
    void deleteAllByRuleSystemCodeAndConceptCode(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("conceptCode") String conceptCode
    );
}
