package com.b4rrhh.employee.cost_center.domain.exception;

import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionPeriod;

import java.time.LocalDate;
import java.util.List;

/**
 * The resulting series would have two distribution windows in force on the
 * same date (ADR-057). The lines of one window are never an overlap among
 * themselves: the occurrence is the window, not the line.
 */
public class CostCenterDistributionOverlapException extends RuntimeException {

    private final List<CostCenterDistributionPeriod> overlaps;

    public CostCenterDistributionOverlapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate startDate,
            LocalDate endDate,
            List<CostCenterDistributionPeriod> overlaps
    ) {
        super("Cost center distribution period overlaps for ruleSystemCode="
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

    /** The stretches of dates the rejected window would share with existing ones. */
    public List<CostCenterDistributionPeriod> overlaps() {
        return overlaps;
    }
}
