package com.b4rrhh.employee.workcenter.domain.exception;

import com.b4rrhh.employee.workcenter.domain.model.WorkCenterPeriod;

import java.time.LocalDate;
import java.util.List;

public class WorkCenterOverlapException extends RuntimeException {

    private final List<WorkCenterPeriod> overlaps;

    public WorkCenterOverlapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        super("Work center period overlaps for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber);
        this.overlaps = List.of();
    }

    public WorkCenterOverlapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate startDate,
            LocalDate endDate,
            List<WorkCenterPeriod> overlaps
    ) {
        super("Work center period overlaps for ruleSystemCode="
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

    /** The stretches of dates the rejected assignment would share with existing ones (ADR-057). */
    public List<WorkCenterPeriod> overlaps() {
        return overlaps;
    }
}
