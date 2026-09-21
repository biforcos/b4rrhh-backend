package com.b4rrhh.payroll_engine.concept.domain.model;

import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Declares that a RATE_BY_QUANTITY concept depends on another concept in a specific operand role.
 *
 * <p>Both {@code targetObject} and {@code sourceObject} must be of type {@code CONCEPT}.
 * The target and source must differ.
 */
public class PayrollConceptOperand {

    private final Long id;
    private final PayrollObject targetObject;
    private final OperandRole operandRole;
    private final PayrollObject sourceObject;
    private final LocalDateTime createdAt;
    private final Instant updatedAt;

    public PayrollConceptOperand(
            Long id,
            PayrollObject targetObject,
            OperandRole operandRole,
            PayrollObject sourceObject,
            LocalDateTime createdAt,
            Instant updatedAt
    ) {
        Objects.requireNonNull(targetObject, "targetObject is required");
        Objects.requireNonNull(operandRole, "operandRole is required");
        Objects.requireNonNull(sourceObject, "sourceObject is required");
        if (targetObject.getObjectTypeCode() != PayrollObjectTypeCode.CONCEPT) {
            throw new IllegalArgumentException(
                    "targetObject must be of type CONCEPT, got: " + targetObject.getObjectTypeCode());
        }
        if (sourceObject.getObjectTypeCode() != PayrollObjectTypeCode.CONCEPT) {
            throw new IllegalArgumentException(
                    "sourceObject must be of type CONCEPT, got: " + sourceObject.getObjectTypeCode());
        }
        if (Objects.equals(targetObject.getId(), sourceObject.getId())) {
            throw new IllegalArgumentException("targetObject and sourceObject must differ");
        }
        this.id = id;
        this.targetObject = targetObject;
        this.operandRole = operandRole;
        this.sourceObject = sourceObject;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public PayrollObject getTargetObject() { return targetObject; }
    public OperandRole getOperandRole() { return operandRole; }
    public PayrollObject getSourceObject() { return sourceObject; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
