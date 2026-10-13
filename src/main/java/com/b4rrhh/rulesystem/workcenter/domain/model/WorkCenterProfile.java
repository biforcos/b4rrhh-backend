package com.b4rrhh.rulesystem.workcenter.domain.model;

import com.b4rrhh.geo.address.domain.model.PostalAddress;

public class WorkCenterProfile {

    private static final int COMPANY_CODE_MAX_LENGTH = 30;

    private final String companyCode;
    /** La dirección, la misma estructura que la del empleado y la empresa; {@code null} si no tiene (backend#155). */
    private final PostalAddress address;

    public WorkCenterProfile(String companyCode, PostalAddress address) {
        this.companyCode = normalizeOptionalCode("companyCode", companyCode, COMPANY_CODE_MAX_LENGTH);
        this.address = address;
    }

    public WorkCenterProfile update(String companyCode, PostalAddress address) {
        return new WorkCenterProfile(companyCode, address);
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public PostalAddress getAddress() {
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