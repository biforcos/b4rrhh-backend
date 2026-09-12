package com.b4rrhh.employee.contract.domain.model;

import com.b4rrhh.employee.contract.domain.exception.ContractInvalidException;
import com.b4rrhh.employee.contract.domain.exception.InvalidContractDateRangeException;
import com.b4rrhh.employee.contract.domain.exception.ContractAlreadyClosedException;
import com.b4rrhh.employee.contract.domain.exception.ContractSubtypeInvalidException;

import java.time.LocalDate;

public class Contract {

    private static final int CONTRACT_CODE_LENGTH = 3;

    private final Long employeeId;
    private final String contractCode;
    private final String contractSubtypeCode;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public Contract(
            Long employeeId,
            String contractCode,
            String contractSubtypeCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        this.employeeId = normalizeRequiredEmployeeId(employeeId);
        this.contractCode = normalizeRequiredCode("contractCode", contractCode);
        this.contractSubtypeCode = normalizeRequiredCode("contractSubtypeCode", contractSubtypeCode);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static Contract rehydrate(
            Long employeeId,
            String contractCode,
            String contractSubtypeCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new Contract(employeeId, contractCode, contractSubtypeCode, startDate, endDate);
    }

    public Contract adjustEndDate(LocalDate newEndDate) {
        return new Contract(employeeId, contractCode, contractSubtypeCode, startDate, newEndDate);
    }

    public Contract close(LocalDate closeDate) {
        if (!isActive()) {
            throw new ContractAlreadyClosedException(startDate);
        }
        if (closeDate == null || closeDate.isBefore(startDate)) {
            throw new InvalidContractDateRangeException(
                    "endDate must be greater than or equal to startDate"
            );
        }

        return new Contract(
                employeeId,
                contractCode,
                contractSubtypeCode,
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
        String normalized = value == null ? null : value.trim().toUpperCase();
        if (normalized == null || normalized.isEmpty()) {
            if ("contractCode".equals(fieldName)) {
                throw new ContractInvalidException(String.valueOf(value));
            }
            if ("contractSubtypeCode".equals(fieldName)) {
                throw new ContractSubtypeInvalidException(String.valueOf(value));
            }
            throw new IllegalArgumentException(fieldName + " is required");
        }

        if (normalized.length() != CONTRACT_CODE_LENGTH && "contractCode".equals(fieldName)) {
            throw new ContractInvalidException(normalized);
        }

        return normalized;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new InvalidContractDateRangeException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidContractDateRangeException(
                    "endDate must be greater than or equal to startDate"
            );
        }
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getContractCode() {
        return contractCode;
    }

    public String getContractSubtypeCode() {
        return contractSubtypeCode;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
