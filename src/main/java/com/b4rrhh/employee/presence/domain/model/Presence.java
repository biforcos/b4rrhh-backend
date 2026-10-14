package com.b4rrhh.employee.presence.domain.model;

import com.b4rrhh.employee.presence.domain.exception.InvalidPresenceDateRangeException;
import com.b4rrhh.employee.presence.domain.exception.PresenceAlreadyClosedException;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Presence {

    private final Long id;
    private final Long employeeId;
    private final Integer presenceNumber;
    private final String companyCode;
    private final String entryReasonCode;
    private final String exitReasonCode;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Presence(
            Long id,
            Long employeeId,
            Integer presenceNumber,
            String companyCode,
            String entryReasonCode,
            String exitReasonCode,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        validateDateRange(startDate, endDate);

        this.id = id;
        this.employeeId = employeeId;
        this.presenceNumber = presenceNumber;
        this.companyCode = companyCode;
        this.entryReasonCode = entryReasonCode;
        this.exitReasonCode = exitReasonCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * La presencia con su alta o su cese rectificados (backend#172): estaban mal tecleados. Todo lo demás
     * se conserva; las fechas ya vienen validadas por quien rectifica.
     */
    public Presence rectifyDates(LocalDate newStartDate, LocalDate newEndDate) {
        return new Presence(id, employeeId, presenceNumber, companyCode, entryReasonCode, exitReasonCode,
                newStartDate, newEndDate, createdAt, updatedAt);
    }

    public Presence close(LocalDate closeDate, String closeExitReasonCode) {
        if (!isActive()) {
            throw new PresenceAlreadyClosedException(presenceNumber);
        }
        if (closeDate == null || !closeDate.isAfter(startDate)) {
            throw new InvalidPresenceDateRangeException("endDate must be greater than startDate");
        }

        return new Presence(
                id,
                employeeId,
                presenceNumber,
                companyCode,
                entryReasonCode,
                closeExitReasonCode,
                startDate,
                closeDate,
                createdAt,
                updatedAt
        );
    }

    public boolean isActive() {
        return endDate == null;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new InvalidPresenceDateRangeException("startDate is required");
        }
        if (endDate != null && !endDate.isAfter(startDate)) {
            throw new InvalidPresenceDateRangeException("endDate must be greater than startDate");
        }
    }

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public Integer getPresenceNumber() { return presenceNumber; }
    public String getCompanyCode() { return companyCode; }
    public String getEntryReasonCode() { return entryReasonCode; }
    public String getExitReasonCode() { return exitReasonCode; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
