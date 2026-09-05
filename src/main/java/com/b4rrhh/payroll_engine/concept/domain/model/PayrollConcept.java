package com.b4rrhh.payroll_engine.concept.domain.model;

import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;

import java.time.LocalDateTime;

/**
 * A PayrollConcept is a semantic subtype of PayrollObject.
 * Its business key is inherited: (ruleSystemCode, CONCEPT, conceptCode).
 * 'conceptCode' is a semantic alias of objectCode in the concept context.
 */
public class PayrollConcept {

    private final PayrollObject object;
    private final String conceptMnemonic;
    private final CalculationType calculationType;
    private final FunctionalNature functionalNature;
    private final String payslipOrderCode;
    private final ExecutionScope executionScope;
    private final String summary;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public PayrollConcept(
            PayrollObject object,
            String conceptMnemonic,
            CalculationType calculationType,
            FunctionalNature functionalNature,
            String payslipOrderCode,
            ExecutionScope executionScope,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(object, conceptMnemonic, calculationType, functionalNature,
                payslipOrderCode, executionScope, null, createdAt, updatedAt);
    }

    public PayrollConcept(
            PayrollObject object,
            String conceptMnemonic,
            CalculationType calculationType,
            FunctionalNature functionalNature,
            String payslipOrderCode,
            ExecutionScope executionScope,
            String summary,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        if (object == null) {
            throw new IllegalArgumentException("PayrollConcept requires a base PayrollObject");
        }
        if (object.getObjectTypeCode() != com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode.CONCEPT) {
            throw new IllegalArgumentException(
                    "PayrollConcept requires a PayrollObject with objectTypeCode=CONCEPT, but got: "
                            + object.getObjectTypeCode()
            );
        }
        if (conceptMnemonic == null || conceptMnemonic.isBlank()) {
            throw new IllegalArgumentException("conceptMnemonic is required");
        }
        if (calculationType == null) {
            throw new IllegalArgumentException("calculationType is required");
        }
        if (functionalNature == null) {
            throw new IllegalArgumentException("functionalNature is required");
        }
        if (executionScope == null) {
            throw new IllegalArgumentException("executionScope is required");
        }
        this.object = object;
        this.conceptMnemonic = conceptMnemonic;
        this.calculationType = calculationType;
        this.functionalNature = functionalNature;
        this.payslipOrderCode = payslipOrderCode;
        this.executionScope = executionScope;
        this.summary = summary;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public PayrollObject getObject() {
        return object;
    }

    public String getRuleSystemCode() {
        return object.getRuleSystemCode();
    }

    public String getConceptCode() {
        return object.getObjectCode();
    }

    public String getConceptMnemonic() {
        return conceptMnemonic;
    }

    public CalculationType getCalculationType() {
        return calculationType;
    }

    public FunctionalNature getFunctionalNature() {
        return functionalNature;
    }

    public String getPayslipOrderCode() {
        return payslipOrderCode;
    }

    public ExecutionScope getExecutionScope() {
        return executionScope;
    }

    public String getSummary() {
        return summary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
