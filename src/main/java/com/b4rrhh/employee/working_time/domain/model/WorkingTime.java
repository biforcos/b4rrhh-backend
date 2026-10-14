package com.b4rrhh.employee.working_time.domain.model;

import com.b4rrhh.employee.working_time.domain.exception.InvalidWorkingTimeDateRangeException;
import com.b4rrhh.employee.working_time.domain.exception.InvalidWorkingTimePercentageException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeAlreadyClosedException;
import com.b4rrhh.employee.working_time.domain.service.WorkingTimeDerivationPolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class WorkingTime {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal MAX_PERCENTAGE = new BigDecimal("100");

    private final Long id;
    private final Long employeeId;
    private final Integer workingTimeNumber;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final BigDecimal workingTimePercentage;
    private final BigDecimal weeklyHours;
    private final BigDecimal dailyHours;
    private final BigDecimal monthlyHours;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private WorkingTime(
            Long id,
            Long employeeId,
            Integer workingTimeNumber,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal workingTimePercentage,
            BigDecimal weeklyHours,
            BigDecimal dailyHours,
            BigDecimal monthlyHours,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.employeeId = employeeId;
        this.workingTimeNumber = workingTimeNumber;
        this.startDate = startDate;
        this.endDate = endDate;
        this.workingTimePercentage = workingTimePercentage;
        this.weeklyHours = weeklyHours;
        this.dailyHours = dailyHours;
        this.monthlyHours = monthlyHours;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static WorkingTime create(
            Long employeeId,
            Integer workingTimeNumber,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal workingTimePercentage,
            WorkingTimeDerivedHours derivedHours,
            BigDecimal annualHours,
            WorkingTimeDerivationPolicy derivationPolicy
    ) {
        return buildForCreate(
                null,
                employeeId,
                workingTimeNumber,
                startDate,
                endDate,
                workingTimePercentage,
                derivedHours,
                annualHours,
                null,
                null,
                derivationPolicy
        );
    }

    public static WorkingTime rehydrate(
            Long id,
            Long employeeId,
            Integer workingTimeNumber,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal workingTimePercentage,
            WorkingTimeDerivedHours derivedHours,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return buildForRehydrate(
                id,
                employeeId,
                workingTimeNumber,
                startDate,
                endDate,
                workingTimePercentage,
                derivedHours,
                createdAt,
                updatedAt
        );
    }

    private static WorkingTime buildForCreate(
            Long id,
            Long employeeId,
            Integer workingTimeNumber,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal workingTimePercentage,
            WorkingTimeDerivedHours derivedHours,
            BigDecimal annualHours,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            WorkingTimeDerivationPolicy derivationPolicy
    ) {
        validateDateRange(startDate, endDate);
        Integer normalizedWorkingTimeNumber = normalizeWorkingTimeNumber(workingTimeNumber);
        BigDecimal normalizedWorkingTimePercentage = normalizeWorkingTimePercentage(workingTimePercentage);
        WorkingTimeDerivedHours normalizedDerivedHours = normalizeDerivedHours(derivedHours);
        validateDerivedHours(normalizedDerivedHours);
        validateDerivedHoursConsistency(normalizedWorkingTimePercentage, normalizedDerivedHours, annualHours, derivationPolicy);

        return new WorkingTime(
                id,
                employeeId,
                normalizedWorkingTimeNumber,
                startDate,
                endDate,
                normalizedWorkingTimePercentage,
                normalizedDerivedHours.weeklyHours(),
                normalizedDerivedHours.dailyHours(),
                normalizedDerivedHours.monthlyHours(),
                createdAt,
                updatedAt
        );
    }

    private static WorkingTime buildForRehydrate(
            Long id,
            Long employeeId,
            Integer workingTimeNumber,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal workingTimePercentage,
            WorkingTimeDerivedHours derivedHours,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        validateDateRange(startDate, endDate);
        Integer normalizedWorkingTimeNumber = normalizeWorkingTimeNumber(workingTimeNumber);
        BigDecimal normalizedWorkingTimePercentage = normalizeWorkingTimePercentage(workingTimePercentage);
        WorkingTimeDerivedHours normalizedDerivedHours = normalizeDerivedHours(derivedHours);
        validateDerivedHours(normalizedDerivedHours);

        return new WorkingTime(
                id,
                employeeId,
                normalizedWorkingTimeNumber,
                startDate,
                endDate,
                normalizedWorkingTimePercentage,
                normalizedDerivedHours.weeklyHours(),
                normalizedDerivedHours.dailyHours(),
                normalizedDerivedHours.monthlyHours(),
                createdAt,
                updatedAt
        );
    }

    /**
     * Las fechas que tiene que tener porque su presencia se rectificó (backend#172): el alta o el cese
     * estaban mal tecleados. Todo lo demás se conserva.
     */
    public WorkingTime rectifyDates(LocalDate newStartDate, LocalDate newEndDate) {
        return new WorkingTime(id, employeeId, workingTimeNumber, newStartDate, newEndDate, workingTimePercentage,
                weeklyHours, dailyHours, monthlyHours, createdAt, updatedAt);
    }

    public WorkingTime close(LocalDate closeDate) {
        if (!isActive()) {
            throw new WorkingTimeAlreadyClosedException(workingTimeNumber);
        }
        if (closeDate == null || closeDate.isBefore(startDate)) {
            throw new InvalidWorkingTimeDateRangeException("endDate must be greater than or equal to startDate");
        }

        return new WorkingTime(
                id,
                employeeId,
                workingTimeNumber,
                startDate,
                closeDate,
                workingTimePercentage,
                weeklyHours,
                dailyHours,
                monthlyHours,
                createdAt,
                updatedAt
        );
    }

    public boolean isActive() {
        return endDate == null;
    }

    public WorkingTime adjustEndDate(LocalDate newEndDate) {
        return WorkingTime.rehydrate(
                id,
                employeeId,
                workingTimeNumber,
                startDate,
                newEndDate,
                workingTimePercentage,
                new WorkingTimeDerivedHours(weeklyHours, dailyHours, monthlyHours),
                createdAt,
                null
        );
    }

    private static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new InvalidWorkingTimeDateRangeException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidWorkingTimeDateRangeException("endDate must be greater than or equal to startDate");
        }
    }

    private static Integer normalizeWorkingTimeNumber(Integer value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("workingTimeNumber must be a positive integer");
        }

        return value;
    }

    private static BigDecimal normalizeWorkingTimePercentage(BigDecimal value) {
        if (value == null) {
            throw new InvalidWorkingTimePercentageException("workingTimePercentage is required");
        }
        if (value.compareTo(ZERO) <= 0 || value.compareTo(MAX_PERCENTAGE) > 0) {
            throw new InvalidWorkingTimePercentageException(
                    "workingTimePercentage must be greater than 0 and less than or equal to 100"
            );
        }

        return value.stripTrailingZeros();
    }

    private static WorkingTimeDerivedHours normalizeDerivedHours(WorkingTimeDerivedHours value) {
        if (value == null) {
            throw new InvalidWorkingTimePercentageException("derivedHours are required");
        }

        return new WorkingTimeDerivedHours(
                normalizeDerivedHour("weeklyHours", value.weeklyHours()),
                normalizeDerivedHour("dailyHours", value.dailyHours()),
                normalizeDerivedHour("monthlyHours", value.monthlyHours())
        );
    }

    private static BigDecimal normalizeDerivedHour(String fieldName, BigDecimal value) {
        if (value == null) {
            throw new InvalidWorkingTimePercentageException(fieldName + " is required");
        }

        return value;
    }

    private static void validateDerivedHours(WorkingTimeDerivedHours derivedHours) {
        if (derivedHours.weeklyHours().compareTo(ZERO) <= 0) {
            throw new InvalidWorkingTimePercentageException("weeklyHours must be greater than 0");
        }
        if (derivedHours.dailyHours().compareTo(ZERO) <= 0) {
            throw new InvalidWorkingTimePercentageException("dailyHours must be greater than 0");
        }
        if (derivedHours.monthlyHours().compareTo(ZERO) <= 0) {
            throw new InvalidWorkingTimePercentageException("monthlyHours must be greater than 0");
        }
    }

    private static void validateDerivedHoursConsistency(
            BigDecimal workingTimePercentage,
            WorkingTimeDerivedHours actualDerivedHours,
            BigDecimal annualHours,
            WorkingTimeDerivationPolicy derivationPolicy
    ) {
        if (derivationPolicy == null) {
            throw new IllegalArgumentException("workingTimeDerivationPolicy is required");
        }
        if (annualHours == null) {
            throw new IllegalArgumentException("annualHours is required for derived hours consistency check");
        }

        WorkingTimeDerivedHours expectedDerivedHours = normalizeDerivedHours(
                derivationPolicy.derive(workingTimePercentage, annualHours));

        boolean matches = expectedDerivedHours.weeklyHours().compareTo(actualDerivedHours.weeklyHours()) == 0
                && expectedDerivedHours.dailyHours().compareTo(actualDerivedHours.dailyHours()) == 0
                && expectedDerivedHours.monthlyHours().compareTo(actualDerivedHours.monthlyHours()) == 0;

        if (!matches) {
            throw new InvalidWorkingTimePercentageException(
                    "derived hours must match workingTimePercentage according to the active derivation policy"
            );
        }
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Integer getWorkingTimeNumber() {
        return workingTimeNumber;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getWorkingTimePercentage() {
        return workingTimePercentage;
    }

    public BigDecimal getWeeklyHours() {
        return weeklyHours;
    }

    public BigDecimal getDailyHours() {
        return dailyHours;
    }

    public BigDecimal getMonthlyHours() {
        return monthlyHours;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}