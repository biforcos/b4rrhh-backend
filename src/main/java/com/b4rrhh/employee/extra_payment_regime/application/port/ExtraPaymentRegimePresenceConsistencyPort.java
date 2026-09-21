package com.b4rrhh.employee.extra_payment_regime.application.port;

import com.b4rrhh.employee.temporal.support.DateRange;

import java.time.LocalDate;
import java.util.List;

public interface ExtraPaymentRegimePresenceConsistencyPort {

    boolean existsPresenceContainingPeriod(Long employeeId, LocalDate startDate, LocalDate endDate);

    /** Every presence period of the employee, oldest first: the frame the extra payment regime series must stay inside. */
    List<DateRange> findPresencePeriodsByEmployeeIdOrderByStartDate(Long employeeId);
}