package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataPayslipSectionRepository
        extends JpaRepository<PayslipSectionEntity, PayslipSectionEntity.Key> {

    @Query("""
        select s from PayrollEnginePayslipSectionEntity s
        where s.ruleSystemCode = :ruleSystemCode
        order by s.displayOrder
        """)
    List<PayslipSectionEntity> findOrdered(@Param("ruleSystemCode") String ruleSystemCode);
}
