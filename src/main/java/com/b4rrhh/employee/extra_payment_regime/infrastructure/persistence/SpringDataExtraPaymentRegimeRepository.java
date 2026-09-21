package com.b4rrhh.employee.extra_payment_regime.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataExtraPaymentRegimeRepository extends JpaRepository<ExtraPaymentRegimeEntity, Long> {

    Optional<ExtraPaymentRegimeEntity> findByEmployeeIdAndExtraPaymentRegimeNumber(
            Long employeeId,
            Integer extraPaymentRegimeNumber
    );

    List<ExtraPaymentRegimeEntity> findByEmployeeIdOrderByStartDateAsc(Long employeeId);

    @Query("""
            select max(r.extraPaymentRegimeNumber)
            from ExtraPaymentRegimeEntity r
            where r.employeeId = :employeeId
            """)
    Integer findMaxExtraPaymentRegimeNumberByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("""
            select r
            from ExtraPaymentRegimeEntity r
            where r.employeeId = :employeeId
              and r.startDate <= :periodEnd
              and (r.endDate is null or r.endDate >= :periodStart)
            order by r.startDate asc, r.extraPaymentRegimeNumber asc
            """)
    List<ExtraPaymentRegimeEntity> findOverlappingByEmployeeIdAndPeriodOrdered(
            @Param("employeeId") Long employeeId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );
}
