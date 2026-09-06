package com.b4rrhh.employee.workcenter.application.port;

import com.b4rrhh.employee.workcenter.domain.port.EmployeeActiveCompanyLookupPort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkCenterPresenceConsistencyPort extends EmployeeActiveCompanyLookupPort {

    boolean existsPresenceContainingPeriod(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    @Override
    Optional<String> findActiveCompanyCode(Long employeeId, LocalDate referenceDate);

    List<PresencePeriod> findPresencePeriodsByEmployeeIdOrderByStartDate(Long employeeId);
}