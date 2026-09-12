package com.b4rrhh.employee.contract.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataContractRepository extends JpaRepository<ContractEntity, Long> {

    Optional<ContractEntity> findByEmployeeIdAndStartDate(Long employeeId, LocalDate startDate);

    List<ContractEntity> findByEmployeeIdOrderByStartDateAsc(Long employeeId);
}
