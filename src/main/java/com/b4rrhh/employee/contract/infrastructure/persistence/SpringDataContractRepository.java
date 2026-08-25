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
     * Consulta nativa, y no JPQL, por el ultimo parametro.
     *
     * En JPQL, ":excludeStartDate is null" se traduce a un "? is null" suelto.
     * Postgres analiza cada marcador por separado y ahi no tiene con que
     * deducir el tipo, asi que se niega a preparar la sentencia:
     *   ERROR: could not determine data type of parameter $5
     * H2 se lo tragaba, de modo que esto llevaba roto contra el motor de
     * produccion siempre que no se excluyera ninguna fecha.
     *
     * El cast explicito lo resuelve. Es exactamente la misma forma que ya
     * tenia el gemelo de clasificacion laboral, que si se arreglo en su dia:
     * ver SpringDataLaborClassificationRepository.existsOverlappingPeriod.
     */
    @Query(value = """
            select case when count(*) > 0 then true else false end
            from employee.contract l
            where l.employee_id = :employeeId
              and l.start_date <= :effectiveEndDate
              and :startDate <= coalesce(l.end_date, :maxDate)
              and (cast(:excludeStartDate as date) is null or l.start_date <> :excludeStartDate)
            """, nativeQuery = true)
    boolean existsOverlappingPeriod(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("effectiveEndDate") LocalDate effectiveEndDate,
            @Param("maxDate") LocalDate maxDate,
            @Param("excludeStartDate") LocalDate excludeStartDate
    );
}
