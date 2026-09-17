package com.b4rrhh.employee.labor_classification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataLaborClassificationRepository extends JpaRepository<LaborClassificationEntity, Long> {

    Optional<LaborClassificationEntity> findByEmployeeIdAndStartDate(Long employeeId, LocalDate startDate);

    List<LaborClassificationEntity> findByEmployeeIdOrderByStartDateAsc(Long employeeId);

    /**
     * Los tramos de clasificacion que tocan el periodo, en orden ({@code backend#47}).
     *
     * <p>El calculo de nomina necesita los TRAMOS y no el vigente a una fecha: un cambio de
     * categoria a mitad de mes parte el periodo, porque la categoria decide de que fila de tabla
     * sale el precio del dia.
     */
    @Query("""
            select l
            from LaborClassificationEntity l
            where l.employeeId = :employeeId
              and l.startDate <= :periodEnd
              and (l.endDate is null or l.endDate >= :periodStart)
            order by l.startDate asc
            """)
    List<LaborClassificationEntity> findOverlappingByEmployeeIdAndPeriodOrdered(
            @Param("employeeId") Long employeeId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );
}
