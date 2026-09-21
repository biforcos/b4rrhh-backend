package com.b4rrhh.payroll_engine.concept.domain.model;

import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Represents the feed relationship from a source PayrollObject to a target PayrollObject.
 * In this iteration, only FEED_BY_SOURCE mode is supported.
 */
public class PayrollConceptFeedRelation {

    private final Long id;
    private final PayrollObject sourceObject;
    private final PayrollObject targetObject;
    private final FeedMode feedMode;
    private final BigDecimal feedValue;
    private final boolean invertSign;
    private final LocalDate effectiveFrom;
    private final LocalDate effectiveTo;
    private final LocalDateTime createdAt;
    private final Instant updatedAt;

    public PayrollConceptFeedRelation(
            Long id,
            PayrollObject sourceObject,
            PayrollObject targetObject,
            FeedMode feedMode,
            BigDecimal feedValue,
            boolean invertSign,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            LocalDateTime createdAt,
            Instant updatedAt
    ) {
        if (sourceObject == null) {
            throw new IllegalArgumentException("sourceObject is required");
        }
        if (targetObject == null) {
            throw new IllegalArgumentException("targetObject is required");
        }
        if (feedMode == null) {
            throw new IllegalArgumentException("feedMode is required");
        }
        if (effectiveFrom == null) {
            throw new IllegalArgumentException("effectiveFrom is required");
        }
        if (!isAllowedSourceType(sourceObject.getObjectTypeCode())) {
            throw new IllegalArgumentException(
                "Feed relation sourceObject must be one of [CONCEPT, TABLE, CONSTANT], but got: "
                            + sourceObject.getObjectTypeCode()
            );
        }
        if (targetObject.getObjectTypeCode() != PayrollObjectTypeCode.CONCEPT) {
            throw new IllegalArgumentException(
                    "Feed relation targetObject must be of type CONCEPT, but got: "
                            + targetObject.getObjectTypeCode()
            );
        }
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException(
                    "effectiveTo must be null or >= effectiveFrom, but got effectiveFrom="
                            + effectiveFrom + " and effectiveTo=" + effectiveTo
            );
        }
        this.id = id;
        this.sourceObject = sourceObject;
        this.targetObject = targetObject;
        this.feedMode = feedMode;
        this.feedValue = feedValue;
        this.invertSign = invertSign;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public PayrollObject getSourceObject() { return sourceObject; }
    public PayrollObject getTargetObject() { return targetObject; }
    public FeedMode getFeedMode() { return feedMode; }
    public BigDecimal getFeedValue() { return feedValue; }
    public boolean isInvertSign() { return invertSign; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    private boolean isAllowedSourceType(PayrollObjectTypeCode sourceType) {
        return sourceType == PayrollObjectTypeCode.CONCEPT
                || sourceType == PayrollObjectTypeCode.TABLE
                || sourceType == PayrollObjectTypeCode.CONSTANT;
    }

    public boolean isActiveAt(LocalDate referenceDate) {
        return !referenceDate.isBefore(effectiveFrom)
                && (effectiveTo == null || !referenceDate.isAfter(effectiveTo));
    }
}
