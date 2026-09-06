package com.b4rrhh.employee.cost_center.domain.port;

import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;

import java.time.LocalDate;
import java.util.List;

public interface CostCenterRepository {

    List<CostCenterAllocation> findByEmployeeIdOrderByStartDate(Long employeeId);

    /** Returns all allocations for the employee whose period includes the given date. */
    List<CostCenterAllocation> findActiveAtDate(Long employeeId, LocalDate date);

    /** Returns all allocations belonging to the window identified by (employeeId, startDate). */
    List<CostCenterAllocation> findByEmployeeIdAndStartDate(Long employeeId, LocalDate startDate);

    void saveAll(List<CostCenterAllocation> allocations);

    /** Closes all open allocations in a window (identified by employeeId + startDate) with the given endDate. */
    void closeAllForWindow(Long employeeId, LocalDate windowStartDate, LocalDate closeDate);

    /**
     * Gives every line of the window the same end date, open when {@code endDate}
     * is null. It is how a plan's one automatic consequence is applied (ADR-057):
     * the window in force is closed the day before a new one, or the previous
     * window is reopened when the one that closed it is removed. Closed lines
     * move too: the occurrence is the window, and it moves whole.
     */
    void adjustWindowEndDate(Long employeeId, LocalDate windowStartDate, LocalDate endDate);

    /** Removes every line of the window identified by (employeeId, startDate). */
    void deleteAllForWindow(Long employeeId, LocalDate windowStartDate);
}
