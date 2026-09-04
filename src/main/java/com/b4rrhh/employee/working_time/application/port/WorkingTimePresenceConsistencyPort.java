package com.b4rrhh.employee.working_time.application.port;

import com.b4rrhh.employee.temporal.support.DateRange;

import java.time.LocalDate;
import java.util.List;

public interface WorkingTimePresenceConsistencyPort {

    boolean existsPresenceContainingPeriod(Long employeeId, LocalDate startDate, LocalDate endDate);

    /** Every presence period of the employee, oldest first: the frame the working time series must stay inside. */
    List<DateRange> findPresencePeriodsByEmployeeIdOrderByStartDate(Long employeeId);
}