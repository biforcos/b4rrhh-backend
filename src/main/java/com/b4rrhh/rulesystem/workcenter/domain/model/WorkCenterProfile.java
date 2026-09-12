package com.b4rrhh.rulesystem.workcenter.domain.model;

public class WorkCenterProfile {

    private static final int COMPANY_CODE_MAX_LENGTH = 30;

    private final String companyCode;
    private final WorkCenterAddress address;

    public WorkCenterProfile(String companyCode, WorkCenterAddress address) {
        this.companyCode = normalizeOptionalCode("companyCode", companyCode, COMPANY_CODE_MAX_LENGTH);
        this.address = address == null ? WorkCenterAddress.empty() : address;
    }

    public WorkCenterProfile update(String companyCode, WorkCenterAddress address) {
        return new WorkCenterProfile(companyCode, address);
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public WorkCenterAddress getAddress() {
        return address;
    }

    private String normalizeOptionalCode(String fieldName, String value, int maxLength) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        normalized = normalized.toUpperCase();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " exceeds max length " + maxLength);
        }

        return normalized;
    }
}