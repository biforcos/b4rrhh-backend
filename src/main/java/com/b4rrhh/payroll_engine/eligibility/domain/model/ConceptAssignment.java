package com.b4rrhh.payroll_engine.eligibility.domain.model;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate root representing a concept assignment rule.
 *
 * <p>A {@code ConceptAssignment} declares that a given payroll concept ({@code conceptCode})
 * is applicable within a rule system, optionally scoped by company, collective agreement,
 * and employee type. It is valid during a defined date range.
 *
 * <h3>Scope semantics</h3>
 * <p>The three optional dimensions ({@code companyCode}, {@code agreementCode},
 * {@code employeeTypeCode}) act as <strong>wildcards when null</strong>: a null value
 * matches any value in that dimension during eligibility resolution.
 *
 * <h3>Priority</h3>
 * <p>When multiple assignments apply to the same concept in a given context,
 * the one with the highest {@code priority} wins. Priority is an explicit integer
 * declared in data — it is never calculated automatically from the number of non-null
 * dimensions.
 *
 * <h3>Employee type vs. agreement category</h3>
 * <p>{@code employeeTypeCode} is a structural employee classification (e.g. indefinite,
 * temporary, intern). It is NOT the collective-agreement professional category, which
 * belongs to the {@code labor_classification} vertical.
 */
public class ConceptAssignment {

    private final Long id;
    private final String ruleSystemCode;
    private final String conceptCode;
    private final String companyCode;
    private final String agreementCode;
    private final String employeeTypeCode;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final int priority;
    private final LocalDateTime createdAt;
    private final Instant updatedAt;

    public ConceptAssignment(
            Long id,
            String ruleSystemCode,
            String conceptCode,
            String companyCode,
            String agreementCode,
            String employeeTypeCode,
            LocalDate validFrom,
            LocalDate validTo,
            int priority,
            LocalDateTime createdAt,
            Instant updatedAt
    ) {
        if (ruleSystemCode == null || ruleSystemCode.isBlank()) {
            throw new IllegalArgumentException("ruleSystemCode is required");
        }
        if (conceptCode == null || conceptCode.isBlank()) {
            throw new IllegalArgumentException("conceptCode is required");
        }
        if (validFrom == null) {
            throw new IllegalArgumentException("validFrom is required");
        }
        if (validTo != null && validTo.isBefore(validFrom)) {
            throw new IllegalArgumentException(
                    "validTo must not be before validFrom: validFrom=" + validFrom + ", validTo=" + validTo);
        }
        this.id = id;
        this.ruleSystemCode = ruleSystemCode;
        this.conceptCode = conceptCode;
        this.companyCode = blankToNull(companyCode);
        this.agreementCode = blankToNull(agreementCode);
        this.employeeTypeCode = blankToNull(employeeTypeCode);
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.priority = priority;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /**
     * Returns true if this assignment is valid on the given reference date.
     *
     * <p>Valid means: {@code validFrom <= referenceDate} and
     * ({@code validTo == null} OR {@code validTo >= referenceDate}).
     */
    public boolean isValidOn(LocalDate referenceDate) {
        Objects.requireNonNull(referenceDate, "referenceDate is required");
        return !validFrom.isAfter(referenceDate)
                && (validTo == null || !validTo.isBefore(referenceDate));
    }

    public Long getId() { return id; }
    public String getRuleSystemCode() { return ruleSystemCode; }
    public String getConceptCode() { return conceptCode; }
    public String getCompanyCode() { return companyCode; }
    public String getAgreementCode() { return agreementCode; }
    public String getEmployeeTypeCode() { return employeeTypeCode; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public int getPriority() { return priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConceptAssignment other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ConceptAssignment{id=" + id
                + ", ruleSystem=" + ruleSystemCode
                + ", concept=" + conceptCode
                + ", company=" + companyCode
                + ", agreement=" + agreementCode
                + ", employeeType=" + employeeTypeCode
                + ", validFrom=" + validFrom
                + ", validTo=" + validTo
                + ", priority=" + priority + '}';
    }
}
