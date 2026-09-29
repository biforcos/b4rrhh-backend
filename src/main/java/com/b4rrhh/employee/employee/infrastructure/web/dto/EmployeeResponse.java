package com.b4rrhh.employee.employee.infrastructure.web.dto;

import java.time.LocalDate;

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
        LocalDate statusDate,
        LocalDate statusSince,
        LocalDate plannedTerminationDate,
        LocalDate plannedHireDate,
        String photoUrl
) {
}
