package com.b4rrhh.payroll_engine.object.domain.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

public class PayrollObject {

    private final Long id;
    private final String ruleSystemCode;
    private final PayrollObjectTypeCode objectTypeCode;
    private final String objectCode;
    private final LocalDateTime createdAt;
    private final Instant updatedAt;

    public PayrollObject(
            Long id,
            String ruleSystemCode,
            PayrollObjectTypeCode objectTypeCode,
            String objectCode,
            LocalDateTime createdAt,
            Instant updatedAt
    ) {
        if (ruleSystemCode == null || ruleSystemCode.isBlank()) {
            throw new IllegalArgumentException("ruleSystemCode is required");
        }
        if (objectTypeCode == null) {
            throw new IllegalArgumentException("objectTypeCode is required");
        }
        if (objectCode == null || objectCode.isBlank()) {
            throw new IllegalArgumentException("objectCode is required");
        }
        this.id = id;
        this.ruleSystemCode = ruleSystemCode;
        this.objectTypeCode = objectTypeCode;
        this.objectCode = objectCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getRuleSystemCode() {
        return ruleSystemCode;
    }

    public PayrollObjectTypeCode getObjectTypeCode() {
        return objectTypeCode;
    }

    public String getObjectCode() {
        return objectCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PayrollObject other)) return false;
        return Objects.equals(ruleSystemCode, other.ruleSystemCode)
                && Objects.equals(objectTypeCode, other.objectTypeCode)
                && Objects.equals(objectCode, other.objectCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleSystemCode, objectTypeCode, objectCode);
    }
}
