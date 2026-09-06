package com.b4rrhh.employee.cost_center.application.port;

import java.time.LocalDate;
import java.util.List;

public interface CostCenterPresenceConsistencyPort {

    boolean existsPresenceContainingPeriod(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    /** The presence periods that frame the cost center series (ADR-057), oldest first. */
    List<PresencePeriod> findPresencePeriodsByEmployeeIdOrderByStartDate(Long employeeId);
}
