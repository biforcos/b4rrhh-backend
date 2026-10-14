package com.b4rrhh.employee.labor_classification.domain.model;

import com.b4rrhh.employee.labor_classification.domain.exception.InvalidLaborClassificationDateRangeException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAlreadyClosedException;

import java.time.LocalDate;

public class LaborClassification {

    private final Long employeeId;
    private final String agreementCode;
    private final String agreementCategoryCode;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public LaborClassification(
            Long employeeId,
            String agreementCode,
            String agreementCategoryCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        this.employeeId = normalizeRequiredEmployeeId(employeeId);
        this.agreementCode = normalizeRequiredCode("agreementCode", agreementCode);
        this.agreementCategoryCode = normalizeRequiredCode("agreementCategoryCode", agreementCategoryCode);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public LaborClassification correctStartDate(LocalDate newStartDate) {
        return new LaborClassification(employeeId, agreementCode, agreementCategoryCode, newStartDate, endDate);
    }

    public LaborClassification adjustEndDate(LocalDate newEndDate) {
        return new LaborClassification(employeeId, agreementCode, agreementCategoryCode, startDate, newEndDate);
    }

    public LaborClassification updateClassification(String newAgreementCode, String newAgreementCategoryCode) {
        if (!isActive()) {
            throw new LaborClassificationAlreadyClosedException(startDate);
        }

        return new LaborClassification(
                employeeId,
                newAgreementCode,
                newAgreementCategoryCode,
                startDate,
                endDate
        );
    }

    /**
     * Las fechas que tiene que tener porque su presencia se rectificó (backend#172): el alta o el cese
     * estaban mal tecleados. Todo lo demás se conserva.
     */
    public LaborClassification rectifyDates(LocalDate newStartDate, LocalDate newEndDate) {
        return new LaborClassification(employeeId, agreementCode, agreementCategoryCode, newStartDate, newEndDate);
    }

    public LaborClassification close(LocalDate closeDate) {
        if (!isActive()) {
            throw new LaborClassificationAlreadyClosedException(startDate);
        }
        if (closeDate == null || closeDate.isBefore(startDate)) {
            throw new InvalidLaborClassificationDateRangeException(
                    "endDate must be greater than or equal to startDate"
            );
        }

        return new LaborClassification(
                employeeId,
                agreementCode,
                agreementCategoryCode,
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

    private String normalizeRequiredCode(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return value.trim().toUpperCase();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new InvalidLaborClassificationDateRangeException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidLaborClassificationDateRangeException(
                    "endDate must be greater than or equal to startDate"
            );
        }
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getAgreementCode() {
        return agreementCode;
    }

    public String getAgreementCategoryCode() {
        return agreementCategoryCode;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
