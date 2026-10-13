package com.b4rrhh.rulesystem.companyprofile.application.service;

import org.springframework.stereotype.Component;

@Component
public class CompanyProfileInputNormalizer {

    public String normalizeRequiredRuleSystemCode(String ruleSystemCode) {
        return normalizeRequiredCode("ruleSystemCode", ruleSystemCode, 5);
    }

    public String normalizeRequiredCompanyCode(String companyCode) {
        return normalizeRequiredCode("companyCode", companyCode, 30);
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
}