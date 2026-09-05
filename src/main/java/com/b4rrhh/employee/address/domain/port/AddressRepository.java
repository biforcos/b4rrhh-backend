package com.b4rrhh.employee.address.domain.port;

import com.b4rrhh.employee.address.domain.model.Address;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AddressRepository {

    Optional<Address> findByEmployeeIdAndAddressNumber(Long employeeId, Integer addressNumber);

    List<Address> findByEmployeeIdOrderByStartDate(Long employeeId);

    /** The series the temporal component judges (ADR-057, decision 0): the employee's addresses of one type. */
    List<Address> findByEmployeeIdAndAddressTypeCodeOrderByStartDate(Long employeeId, String addressTypeCode);

        boolean existsOverlappingPeriodByAddressType(
            Long employeeId,
            String addressTypeCode,
            LocalDate startDate,
            LocalDate endDate
        );

    Optional<Integer> findMaxAddressNumberByEmployeeId(Long employeeId);

    Address save(Address address);

    void delete(Address address);
}
