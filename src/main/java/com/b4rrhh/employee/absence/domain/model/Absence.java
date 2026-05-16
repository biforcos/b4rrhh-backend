package com.b4rrhh.employee.absence.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Absence {

    private final Long id;
    private final Long employeeId;
    private final String absenceTypeCode;
    private final LocalDate startDate;
    private final int startTime;
    private final LocalDate endDate;
    private final Integer endTime;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Absence(Long id, Long employeeId, String absenceTypeCode,
                    LocalDate startDate, int startTime,
                    LocalDate endDate, Integer endTime,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.absenceTypeCode = absenceTypeCode;
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Absence create(Long employeeId, String absenceTypeCode,
                                  LocalDate startDate, int startTime,
                                  LocalDate endDate, Integer endTime) {
        LocalDateTime now = LocalDateTime.now();
        return new Absence(null, employeeId, absenceTypeCode,
            startDate, startTime, endDate, endTime, now, now);
    }

    public static Absence rehydrate(Long id, Long employeeId, String absenceTypeCode,
                                     LocalDate startDate, int startTime,
                                     LocalDate endDate, Integer endTime,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Absence(id, employeeId, absenceTypeCode,
            startDate, startTime, endDate, endTime, createdAt, updatedAt);
    }

    public Absence update(LocalDate newEndDate, Integer newEndTime) {
        return new Absence(id, employeeId, absenceTypeCode,
            startDate, startTime, newEndDate, newEndTime, createdAt, LocalDateTime.now());
    }

    public Absence closeAt(LocalDate terminationDate) {
        return new Absence(id, employeeId, absenceTypeCode,
            startDate, startTime, terminationDate, null, createdAt, LocalDateTime.now());
    }

    public boolean isOpen() {
        return endDate == null;
    }

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public String getAbsenceTypeCode() { return absenceTypeCode; }
    public LocalDate getStartDate() { return startDate; }
    public int getStartTime() { return startTime; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getEndTime() { return endTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
