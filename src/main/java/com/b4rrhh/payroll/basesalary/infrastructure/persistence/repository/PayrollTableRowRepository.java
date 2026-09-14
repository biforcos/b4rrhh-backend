package com.b4rrhh.payroll.basesalary.infrastructure.persistence.repository;

import com.b4rrhh.payroll.basesalary.infrastructure.persistence.entity.PayrollTableRowEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for PayrollTableRowEntity.
 * Persistence port.
 */
public interface PayrollTableRowRepository extends JpaRepository<PayrollTableRowEntity, Long> {

    /**
     * Find the active table row valid for the given date.
     * Returns the most recent row where start_date <= effectiveDate and (end_date is null or end_date >= effectiveDate).
     */
    @Query("""
        select r from PayrollTableRowEntity r
        where r.ruleSystemCode = :ruleSystemCode
          and r.tableCode = :tableCode
          and r.searchCode = :searchCode
          and r.active = true
          and r.startDate <= :effectiveDate
          and (r.endDate is null or r.endDate >= :effectiveDate)
        order by r.startDate desc
        """)
    List<PayrollTableRowEntity> findLatestValidByRuleSystemCodeAndTableCodeAndSearchCodeAndEffectiveDate(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("tableCode") String tableCode,
            @Param("searchCode") String searchCode,
            @Param("effectiveDate") LocalDate effectiveDate,
            Pageable pageable
    );

    List<PayrollTableRowEntity> findByRuleSystemCodeAndTableCodeOrderBySearchCodeAscStartDateAsc(
            String ruleSystemCode, String tableCode);

    boolean existsByRuleSystemCodeAndTableCodeAndSearchCodeAndStartDate(
            String ruleSystemCode, String tableCode, String searchCode, LocalDate startDate);

    /**
     * Cuantas filas y cuantas activas tiene cada codigo de tabla de un sistema
     * de reglas. Es la mitad de "que tablas hay" que saben las filas: la otra
     * la saben las vinculaciones (backend#95).
     */
    @Query("""
        select r.tableCode, count(r), sum(case when r.active = true then 1L else 0L end)
        from PayrollTableRowEntity r
        where r.ruleSystemCode = :ruleSystemCode
        group by r.tableCode
        """)
    List<Object[]> countRowsByTableCode(@Param("ruleSystemCode") String ruleSystemCode);
}
