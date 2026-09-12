package com.b4rrhh.employee.workcenter.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataWorkCenterRepository extends JpaRepository<WorkCenterEntity, Long> {

    Optional<WorkCenterEntity> findByEmployeeIdAndWorkCenterAssignmentNumber(Long employeeId, Integer workCenterAssignmentNumber);

    List<WorkCenterEntity> findByEmployeeIdOrderByStartDateAsc(Long employeeId);

    @Query("""
            select max(w.workCenterAssignmentNumber)
            from WorkCenterEntity w
            where w.employeeId = :employeeId
            """)
    Integer findMaxWorkCenterAssignmentNumberByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("""
            select w from WorkCenterEntity w
            where w.employeeId = :employeeId
              and w.startDate <= :referenceDate
              and (w.endDate is null or w.endDate >= :referenceDate)
            order by w.workCenterAssignmentNumber desc
            """)
    List<WorkCenterEntity> findActiveByEmployeeIdAndReferenceDate(
            @Param("employeeId") Long employeeId,
            @Param("referenceDate") LocalDate referenceDate,
            Pageable pageable
    );
}