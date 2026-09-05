package com.b4rrhh.employee.address.application.port;

import com.b4rrhh.employee.temporal.support.DateRange;

import java.util.List;

/**
 * The presence periods that frame an address series (ADR-057). An address
 * may outlive the presence, so they never bound it: they only say where the
 * domicile has to be covered.
 */
public interface AddressPresenceLookupPort {

    /** Every presence period of the employee, oldest first. */
    List<DateRange> findPresencePeriodsByEmployeeIdOrderByStartDate(Long employeeId);
}
