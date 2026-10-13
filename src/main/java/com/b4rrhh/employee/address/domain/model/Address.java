package com.b4rrhh.employee.address.domain.model;

import com.b4rrhh.employee.address.domain.exception.AddressAlreadyClosedException;
import com.b4rrhh.employee.address.domain.exception.InvalidAddressDateRangeException;
import com.b4rrhh.geo.address.domain.model.PostalAddress;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Address {

    private final Long id;
    private final Long employeeId;
    private final Integer addressNumber;
    private final String addressTypeCode;
    private final PostalAddress address;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Address(
            Long id,
            Long employeeId,
            Integer addressNumber,
            String addressTypeCode,
            PostalAddress address,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        validateDateRange(startDate, endDate);

        this.id = id;
        this.employeeId = employeeId;
        this.addressNumber = addressNumber;
        this.addressTypeCode = normalizeRequiredCode("addressTypeCode", addressTypeCode);
        if (address == null || address.countryCode() == null) {
            throw new IllegalArgumentException("address with its countryCode is required");
        }
        this.address = address;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Address close(LocalDate closeDate) {
        if (!isActive()) {
            throw new AddressAlreadyClosedException(addressNumber);
        }
        if (closeDate == null || closeDate.isBefore(startDate)) {
            throw new InvalidAddressDateRangeException("endDate must be greater than or equal to startDate");
        }

        return new Address(
                id,
                employeeId,
                addressNumber,
                addressTypeCode,
                address,
                startDate,
                closeDate,
                createdAt,
                updatedAt
        );
    }

    /**
     * The user's correction of this occurrence (ADR-057, decision 3): its
     * fields and its dates. The type never changes: it names the series the
     * occurrence belongs to. Whether the corrected dates leave a gap or an
     * overlap is judged by the series, not here.
     */
    public Address correct(
            PostalAddress address,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new Address(
                id,
                employeeId,
                addressNumber,
                addressTypeCode,
                address,
                startDate,
                endDate,
                createdAt,
                updatedAt
        );
    }

    /**
     * The one automatic consequence a plan applies to an existing occurrence
     * (ADR-057): closed the day before a new one, or reopened when the one
     * that closed it is removed. Only the end date moves.
     */
    public Address adjustEndDate(LocalDate newEndDate) {
        return new Address(
                id,
                employeeId,
                addressNumber,
                addressTypeCode,
                address,
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
            throw new InvalidAddressDateRangeException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidAddressDateRangeException("endDate must be greater than or equal to startDate");
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

    public Integer getAddressNumber() {
        return addressNumber;
    }

    public String getAddressTypeCode() {
        return addressTypeCode;
    }

    /** La dirección, la misma estructura que la de la empresa y el centro (backend#155). */
    public PostalAddress getAddress() {
        return address;
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
