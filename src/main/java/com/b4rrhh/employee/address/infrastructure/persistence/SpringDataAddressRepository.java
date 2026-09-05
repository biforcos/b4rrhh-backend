package com.b4rrhh.employee.address.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataAddressRepository extends JpaRepository<AddressEntity, Long> {

    Optional<AddressEntity> findByEmployeeIdAndAddressNumber(Long employeeId, Integer addressNumber);

    List<AddressEntity> findByEmployeeIdOrderByStartDateAsc(Long employeeId);

    List<AddressEntity> findByEmployeeIdAndAddressTypeCodeOrderByStartDateAsc(Long employeeId, String addressTypeCode);

        @Query("""
            select case when count(a) > 0 then true else false end
            from AddressEntity a
            where a.employeeId = :employeeId
              and a.addressTypeCode = :addressTypeCode
              and a.startDate <= :effectiveEndDate
              and :startDate <= coalesce(a.endDate, :maxDate)
            """)
        boolean existsOverlappingPeriodByAddressType(
            @Param("employeeId") Long employeeId,
            @Param("addressTypeCode") String addressTypeCode,
            @Param("startDate") LocalDate startDate,
            @Param("effectiveEndDate") LocalDate effectiveEndDate,
            @Param("maxDate") LocalDate maxDate
        );

    @Query("""
            select max(a.addressNumber)
            from AddressEntity a
            where a.employeeId = :employeeId
            """)
    Integer findMaxAddressNumberByEmployeeId(@Param("employeeId") Long employeeId);
}
