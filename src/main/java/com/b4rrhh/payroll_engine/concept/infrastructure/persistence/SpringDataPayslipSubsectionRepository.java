package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataPayslipSubsectionRepository
        extends JpaRepository<PayslipSubsectionEntity, PayslipSubsectionEntity.Key> {

    @Query("""
        select s from PayrollEnginePayslipSubsectionEntity s
        where s.ruleSystemCode = :ruleSystemCode
        order by s.displayOrder
        """)
    List<PayslipSubsectionEntity> findOrdered(@Param("ruleSystemCode") String ruleSystemCode);
}
