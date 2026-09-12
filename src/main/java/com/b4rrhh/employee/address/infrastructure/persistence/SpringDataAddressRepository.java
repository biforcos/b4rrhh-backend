package com.b4rrhh.employee.address.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataAddressRepository extends JpaRepository<AddressEntity, Long> {

    Optional<AddressEntity> findByEmployeeIdAndAddressNumber(Long employeeId, Integer addressNumber);

    List<AddressEntity> findByEmployeeIdOrderByStartDateAsc(Long employeeId);

    List<AddressEntity> findByEmployeeIdAndAddressTypeCodeOrderByStartDateAsc(Long employeeId, String addressTypeCode);

    @Query("""
            select max(a.addressNumber)
            from AddressEntity a
            where a.employeeId = :employeeId
            """)
    Integer findMaxAddressNumberByEmployeeId(@Param("employeeId") Long employeeId);
}
