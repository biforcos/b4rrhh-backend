package com.b4rrhh.rulesystem.domain.model;

import com.b4rrhh.rulesystem.domain.exception.RuleEntityAlreadyClosedException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityInvalidDateRangeException;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RuleEntity {

    private final Long id;
    private final String ruleSystemCode;
    private final String layerCode;
    private final Integer level;
    private final String ruleEntityTypeCode;
    private final String code;
    private String name;
    private String description;
    private boolean active;
    private final LocalDate startDate;
    private LocalDate endDate;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    /**
     * Una entidad que todavía no sabe de qué capa viene: la que se va a crear, o la que un test
     * monta a mano. La capa la pone el puerto al resolverla (ADR-077 §4).
     */
    public RuleEntity(
            Long id,
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            String name,
            String description,
            boolean active,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, ruleSystemCode, null, null, ruleEntityTypeCode, code, name, description, active,
                startDate, endDate, createdAt, updatedAt);
    }

    /**
     * Una entidad resuelta: {@code ruleSystemCode} es la reglamentación desde la que se pidió y
     * {@code layerCode}/{@code level}, de dónde viene (backend#157). Una entidad de {@code INT}
     * pedida desde ESP y desde FRA es la misma fila con dos {@code ruleSystemCode}.
     */
    public RuleEntity(
            Long id,
            String ruleSystemCode,
            String layerCode,
            Integer level,
            String ruleEntityTypeCode,
            String code,
            String name,
            String description,
            boolean active,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.ruleSystemCode = ruleSystemCode;
        this.layerCode = layerCode;
        this.level = level;
        this.ruleEntityTypeCode = ruleEntityTypeCode;
        this.code = code;
        this.name = name;
        this.description = description;
        this.active = active;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getRuleSystemCode() { return ruleSystemCode; }
    public String getLayerCode() { return layerCode; }
    public Integer getLevel() { return level; }
    public String getRuleEntityTypeCode() { return ruleEntityTypeCode; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void correct(String name, String description, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        this.name = name;
        this.description = normalizeDescription(description);
        this.endDate = endDate;
        this.active = endDate == null;
    }

    public void close(LocalDate endDate) {
        if (this.endDate != null) {
            throw new RuleEntityAlreadyClosedException(ruleSystemCode, ruleEntityTypeCode, code);
        }

        validateDateRange(startDate, endDate);
        this.endDate = endDate;
        this.active = false;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new RuleEntityInvalidDateRangeException(startDate, endDate);
        }
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
