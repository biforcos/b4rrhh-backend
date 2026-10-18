package com.b4rrhh.employee.employee.infrastructure.web.dto;

public record EmployeeDirectoryItemResponse(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String displayName,
        String firstName,
        String lastName1,
        String status,
        String workCenterCode,
        String photoUrl
) {
}
