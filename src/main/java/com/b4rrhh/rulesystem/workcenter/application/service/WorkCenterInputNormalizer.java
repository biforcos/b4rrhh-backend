package com.b4rrhh.rulesystem.workcenter.application.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class WorkCenterInputNormalizer {

    public String normalizeRequiredRuleSystemCode(String ruleSystemCode) {
        return normalizeRequiredCode("ruleSystemCode", ruleSystemCode, 5);
    }

    public String normalizeRequiredWorkCenterCode(String workCenterCode) {
        return normalizeRequiredCode("workCenterCode", workCenterCode, 30);
    }

    /**
     * La empresa de un centro es obligatoria, en el alta y en la edición: la base la exige desde la
     * V40, y sin ella la escritura llegaba hasta la base y salía un 500 (backend#167).
     */
    public String normalizeRequiredCompanyCode(String companyCode) {
        return normalizeRequiredCode("companyCode", companyCode, 30);
    }

    public String normalizeRequiredName(String name) {
        return normalizeRequiredText("name", name, 100);
    }

    public String normalizeOptionalDescription(String description) {
        return normalizeOptionalText("description", description, 500);
    }

    public LocalDate requireStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }

        return startDate;
    }

    public Integer normalizeRequiredContactNumber(Integer contactNumber) {
        if (contactNumber == null || contactNumber <= 0) {
            throw new IllegalArgumentException("contactNumber must be greater than 0");
        }

        return contactNumber;
    }

    public String normalizeRequiredContactTypeCode(String contactTypeCode) {
        return normalizeRequiredCode("contactTypeCode", contactTypeCode, 30);
    }

    public String normalizeRequiredContactValue(String contactValue) {
        return normalizeRequiredText("contactValue", contactValue, 300);
    }

    private String normalizeRequiredCode(String fieldName, String value, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        String normalized = value.trim().toUpperCase();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " exceeds max length " + maxLength);
        }

        return normalized;
    }

    private String normalizeRequiredText(String fieldName, String value, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " exceeds max length " + maxLength);
        }

        return normalized;
    }

    private String normalizeOptionalText(String fieldName, String value, int maxLength) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " exceeds max length " + maxLength);
        }

        return normalized;
    }
}