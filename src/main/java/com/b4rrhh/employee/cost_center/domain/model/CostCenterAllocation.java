package com.b4rrhh.employee.cost_center.domain.model;

import com.b4rrhh.employee.cost_center.domain.exception.CostCenterAlreadyClosedException;
import com.b4rrhh.employee.cost_center.domain.exception.InvalidAllocationPercentageException;
import com.b4rrhh.employee.cost_center.domain.exception.InvalidCostCenterDateRangeException;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CostCenterAllocation {

    private static final BigDecimal MIN_PERCENTAGE = BigDecimal.ZERO;
    private static final BigDecimal MAX_PERCENTAGE = BigDecimal.valueOf(100);

    private final Long employeeId;
    private final String costCenterCode;
    private final BigDecimal allocationPercentage;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public CostCenterAllocation(
            Long employeeId,
            String costCenterCode,
            BigDecimal allocationPercentage,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        this.employeeId = normalizeRequiredEmployeeId(employeeId);
        this.costCenterCode = normalizeRequiredCode(costCenterCode);
        this.allocationPercentage = normalizeAndValidatePercentage(allocationPercentage);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public CostCenterAllocation updateAllocationPercentage(BigDecimal newAllocationPercentage) {
        if (!isActive()) {
            throw new CostCenterAlreadyClosedException(costCenterCode, startDate);
        }

        return new CostCenterAllocation(
                employeeId,
                costCenterCode,
                newAllocationPercentage,
                startDate,
                endDate
        );
    }

    /**
     * Las fechas que tiene que tener porque su presencia se rectificó (backend#172): el alta o el cese
     * estaban mal tecleados. Todo lo demás se conserva.
     */
    public CostCenterAllocation rectifyDates(LocalDate newStartDate, LocalDate newEndDate) {
        return new CostCenterAllocation(employeeId, costCenterCode, allocationPercentage, newStartDate, newEndDate);
    }

    public CostCenterAllocation close(LocalDate closeDate) {
        if (!isActive()) {
            throw new CostCenterAlreadyClosedException(costCenterCode, startDate);
        }
        if (closeDate == null || closeDate.isBefore(startDate)) {
            throw new InvalidCostCenterDateRangeException("endDate must be greater than or equal to startDate");
        }

        return new CostCenterAllocation(
                employeeId,
                costCenterCode,
                allocationPercentage,
                startDate,
                closeDate
        );
    }

    public boolean isActive() {
        return endDate == null;
    }

    private Long normalizeRequiredEmployeeId(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("employeeId is required");
        }

        return value;
    }

    private String normalizeRequiredCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("costCenterCode is required");
        }

        return value.trim().toUpperCase();
    }

    private BigDecimal normalizeAndValidatePercentage(BigDecimal value) {
        if (value == null) {
            throw new InvalidAllocationPercentageException("allocationPercentage is required");
        }

        if (value.compareTo(MIN_PERCENTAGE) <= 0 || value.compareTo(MAX_PERCENTAGE) > 0) {
            throw new InvalidAllocationPercentageException(
                    "allocationPercentage must be greater than 0 and less than or equal to 100"
            );
        }

        return value;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new InvalidCostCenterDateRangeException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidCostCenterDateRangeException("endDate must be greater than or equal to startDate");
        }
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getCostCenterCode() {
        return costCenterCode;
    }

    public BigDecimal getAllocationPercentage() {
        return allocationPercentage;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
