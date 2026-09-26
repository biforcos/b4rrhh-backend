package com.b4rrhh.employee.absence.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataAbsenceRepository extends JpaRepository<AbsenceEntity, Long> {

    /**
     * Las ausencias que solapan el periodo, en orden ({@code backend#127}).
     *
     * <p>Contra el periodo entero y no contra la presencia: el recorte contra la presencia lo hace
     * la particion, que es donde estan las dos fechas a la vez. Y una ausencia sin cerrar
     * ({@code endDate} nulo) solapa cualquier periodo que empiece despues de su inicio, que es lo
     * que dice el {@code coalesce}.
     */
    @Query("""
        select a from AbsenceEntity a
         where a.employeeId = :employeeId
           and a.startDate <= :periodEnd
           and (a.endDate is null or a.endDate >= :periodStart)
         order by a.startDate asc, a.startTime asc
        """)
    List<AbsenceEntity> findOverlappingByEmployeeIdAndPeriodOrdered(
            @Param("employeeId") Long employeeId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    Optional<AbsenceEntity> findByEmployeeIdAndAbsenceTypeCodeAndStartDateAndStartTime(
        Long employeeId, String absenceTypeCode, LocalDate startDate, int startTime);

    List<AbsenceEntity> findByEmployeeIdOrderByStartDateDescStartTimeDesc(Long employeeId);

    @Transactional
    void deleteByEmployeeIdAndAbsenceTypeCodeAndStartDateAndStartTime(
        Long employeeId, String absenceTypeCode, LocalDate startDate, int startTime);

    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
        FROM AbsenceEntity a
        WHERE a.employeeId = :employeeId
          AND a.startDate <= :effectiveEndDate
          AND COALESCE(a.endDate, :maxDate) >= :startDate
        """)
    boolean existsOverlappingAbsence(
        @Param("employeeId") Long employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("effectiveEndDate") LocalDate effectiveEndDate,
        @Param("maxDate") LocalDate maxDate);

    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
        FROM AbsenceEntity a
        WHERE a.employeeId = :employeeId
          AND a.startDate <= :effectiveEndDate
          AND COALESCE(a.endDate, :maxDate) >= :startDate
          AND a.id != :excludeId
        """)
    boolean existsOverlappingAbsenceExcluding(
        @Param("employeeId") Long employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("effectiveEndDate") LocalDate effectiveEndDate,
        @Param("excludeId") Long excludeId,
        @Param("maxDate") LocalDate maxDate);
}
