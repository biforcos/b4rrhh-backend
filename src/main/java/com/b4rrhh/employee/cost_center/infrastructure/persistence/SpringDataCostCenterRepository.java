package com.b4rrhh.employee.cost_center.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataCostCenterRepository extends JpaRepository<CostCenterEntity, Long> {

    Optional<CostCenterEntity> findByEmployeeIdAndCostCenterCodeAndStartDate(
            Long employeeId,
            String costCenterCode,
            LocalDate startDate
    );

    List<CostCenterEntity> findByEmployeeIdOrderByStartDateAsc(Long employeeId);

    List<CostCenterEntity> findByEmployeeIdAndStartDate(Long employeeId, LocalDate startDate);

    @Query("""
            select c from CostCenterEntity c
            where c.employeeId = :employeeId
              and c.startDate <= :date
              and (c.endDate is null or c.endDate >= :date)
            order by c.startDate asc
            """)
    List<CostCenterEntity> findActiveAtDate(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date
    );

    @Query("""
            select case when count(c) > 0 then true else false end
            from CostCenterEntity c
            where c.employeeId = :employeeId
              and c.costCenterCode = :costCenterCode
              and c.startDate <= :effectiveEndDate
              and :startDate <= coalesce(c.endDate, :maxDate)
            """)
    boolean existsOverlappingPeriodByCostCenterCode(
            @Param("employeeId") Long employeeId,
            @Param("costCenterCode") String costCenterCode,
            @Param("startDate") LocalDate startDate,
            @Param("effectiveEndDate") LocalDate effectiveEndDate,
            @Param("maxDate") LocalDate maxDate
    );

    @Modifying(clearAutomatically = true)
    @Query("""
            update CostCenterEntity c
            set c.endDate = :closeDate
            where c.employeeId = :employeeId
              and c.startDate = :windowStartDate
              and c.endDate is null
            """)
    void closeAllOpenForWindow(
            @Param("employeeId") Long employeeId,
            @Param("windowStartDate") LocalDate windowStartDate,
            @Param("closeDate") LocalDate closeDate
    );

    /** Every line of the window takes the end date, closed or not; null reopens the window (ADR-057). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update CostCenterEntity c
            set c.endDate = :endDate
            where c.employeeId = :employeeId
              and c.startDate = :windowStartDate
            """)
    void setEndDateForWindow(
            @Param("employeeId") Long employeeId,
            @Param("windowStartDate") LocalDate windowStartDate,
            @Param("endDate") LocalDate endDate
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from CostCenterEntity c
            where c.employeeId = :employeeId
              and c.startDate = :windowStartDate
            """)
    void deleteAllForWindow(
            @Param("employeeId") Long employeeId,
            @Param("windowStartDate") LocalDate windowStartDate
    );
}
