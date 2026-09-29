package com.b4rrhh.rulesystem.employeeaddresstypeprofile.infrastructure.web.dto;

import java.util.List;

public record EmployeeAddressTypeProfilesResponse(
        String ruleSystemCode,
        List<Item> items
) {
    /** @param coverage {@code MANDATORY} mientras haya presencia, u {@code OPTIONAL} */
    public record Item(String addressTypeCode, String coverage) {
    }
}
