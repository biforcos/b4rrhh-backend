package com.b4rrhh.employee.extra_payment_regime.infrastructure.persistence;

import com.b4rrhh.employee.employee.infrastructure.persistence.EmployeeEntity;
import com.b4rrhh.employee.labor_classification.infrastructure.persistence.LaborClassificationEntity;
import com.b4rrhh.employee.extra_payment_regime.application.port.ExtraPaymentRegimeAgreementContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExtraPaymentRegimeAgreementContextRepository extends JpaRepository<LaborClassificationEntity, Long> {

    @Query("""
            select new com.b4rrhh.employee.extra_payment_regime.application.port.ExtraPaymentRegimeAgreementContext(
                e.ruleSystemCode,
                l.agreementCode
            )
            from LaborClassificationEntity l, EmployeeEntity e
            where l.employeeId = e.id
              and l.employeeId = :employeeId
              and l.startDate <= :effectiveDate
              and (l.endDate is null or l.endDate >= :effectiveDate)
            order by l.startDate desc
            """)
    List<ExtraPaymentRegimeAgreementContext> findLatestValidByEmployeeIdAndEffectiveDate(
            @Param("employeeId") Long employeeId,
            @Param("effectiveDate") LocalDate effectiveDate,
            Pageable pageable
    );
}
