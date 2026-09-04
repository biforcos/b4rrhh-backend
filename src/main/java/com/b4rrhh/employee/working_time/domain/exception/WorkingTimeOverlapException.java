package com.b4rrhh.employee.working_time.domain.exception;

import com.b4rrhh.employee.working_time.domain.model.WorkingTimePeriod;

import java.time.LocalDate;
import java.util.List;

public class WorkingTimeOverlapException extends RuntimeException {

    private final List<WorkingTimePeriod> overlaps;

    public WorkingTimeOverlapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this(ruleSystemCode, employeeTypeCode, employeeNumber, startDate, endDate, List.of());
    }

    public WorkingTimeOverlapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate startDate,
            LocalDate endDate,
            List<WorkingTimePeriod> overlaps
    ) {
        super("Working time period overlaps for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ", periodStart="
                + startDate
                + ", periodEnd="
                + endDate
                + ", overlaps="
                + overlaps);
        this.overlaps = List.copyOf(overlaps);
    }

    /** The stretches of dates the rejected occurrence would share with existing ones. */
    public List<WorkingTimePeriod> overlaps() {
        return overlaps;
    }
}