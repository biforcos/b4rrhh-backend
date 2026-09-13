package com.b4rrhh.employee.employee.infrastructure.web.dto;

public record EmployeeResponse(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String firstName,
        String lastName1,
        String lastName2,
        String preferredName,
        String displayName,
        String status,
        String photoUrl
) {
}
