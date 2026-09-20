package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataPayslipSectionNatureRepository
        extends JpaRepository<PayslipSectionNatureEntity, PayslipSectionNatureEntity.Key> {

    @Query("""
        select n from PayrollEnginePayslipSectionNatureEntity n
        where n.ruleSystemCode = :ruleSystemCode
        """)
    List<PayslipSectionNatureEntity> findByRuleSystemCode(@Param("ruleSystemCode") String ruleSystemCode);
}
