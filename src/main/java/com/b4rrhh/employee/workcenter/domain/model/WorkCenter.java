package com.b4rrhh.employee.workcenter.domain.model;

import com.b4rrhh.employee.workcenter.domain.exception.InvalidWorkCenterDateRangeException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterAlreadyClosedException;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class WorkCenter {

    private final Long id;
    private final Long employeeId;
    private final Integer workCenterAssignmentNumber;
    private final String workCenterCode;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public WorkCenter(
            Long id,
            Long employeeId,
            Integer workCenterAssignmentNumber,
            String workCenterCode,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        validateDateRange(startDate, endDate);

        this.id = id;
        this.employeeId = employeeId;
        this.workCenterAssignmentNumber = workCenterAssignmentNumber;
        this.workCenterCode = normalizeRequiredCode("workCenterCode", workCenterCode);
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public WorkCenter close(LocalDate closeDate) {
        if (!isActive()) {
            throw new WorkCenterAlreadyClosedException(workCenterAssignmentNumber);
        }
        if (closeDate == null || closeDate.isBefore(startDate)) {
            throw new InvalidWorkCenterDateRangeException("endDate must be greater than or equal to startDate");
        }

        return new WorkCenter(
                id,
                employeeId,
                workCenterAssignmentNumber,
                workCenterCode,
                startDate,
                closeDate,
                createdAt,
                updatedAt
        );
    }

    /**
     * The same assignment with another end date: the one automatic consequence
     * a plan applies (ADR-057), closed the day before a new one or reopened
     * when the one that closed it is removed. The start date, which the number
     * identifies, stays.
     */
    public WorkCenter adjustEndDate(LocalDate newEndDate) {
        return new WorkCenter(
                id,
                employeeId,
                workCenterAssignmentNumber,
                workCenterCode,
                startDate,
                newEndDate,
                createdAt,
                updatedAt
        );
    }

    public boolean isActive() {
        return endDate == null;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new InvalidWorkCenterDateRangeException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidWorkCenterDateRangeException("endDate must be greater than or equal to startDate");
        }
    }

    private String normalizeRequiredCode(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return value.trim().toUpperCase();
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Integer getWorkCenterAssignmentNumber() {
        return workCenterAssignmentNumber;
    }

    public String getWorkCenterCode() {
        return workCenterCode;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}