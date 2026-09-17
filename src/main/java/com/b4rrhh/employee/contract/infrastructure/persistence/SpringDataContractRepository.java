package com.b4rrhh.employee.contract.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataContractRepository extends JpaRepository<ContractEntity, Long> {

    Optional<ContractEntity> findByEmployeeIdAndStartDate(Long employeeId, LocalDate startDate);

    List<ContractEntity> findByEmployeeIdOrderByStartDateAsc(Long employeeId);

    /**
     * Los tramos de contrato que tocan el periodo, en orden ({@code backend#47}).
     *
     * <p>Rompen el periodo aunque hoy no los lea ningun concepto: un cambio de contrato a mitad de
     * mes es un cambio de las condiciones con las que se calcula, y si no parte, el dia que algun
     * concepto lo lea no habra donde ponerlo.
     */
    @Query("""
            select c
            from ContractEntity c
            where c.employeeId = :employeeId
              and c.startDate <= :periodEnd
              and (c.endDate is null or c.endDate >= :periodStart)
            order by c.startDate asc
            """)
    List<ContractEntity> findOverlappingByEmployeeIdAndPeriodOrdered(
            @Param("employeeId") Long employeeId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );
}
