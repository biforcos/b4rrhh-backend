package com.b4rrhh.employee.contract.domain.port;

import com.b4rrhh.employee.contract.domain.model.Contract;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContractRepository {

    Optional<Contract> findByEmployeeIdAndStartDate(Long employeeId, LocalDate startDate);

    List<Contract> findByEmployeeIdOrderByStartDate(Long employeeId);

    boolean existsOverlappingPeriod(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate excludeStartDateOrNull
    );

    void save(Contract contract);

    void update(Contract contract, LocalDate originalStartDate);

    /** Removes the contract by its functional identity: employee and start date. */
    void delete(Contract contract);
}
