package com.b4rrhh.employee.lifecycle.infrastructure.rest.dto;

import java.time.LocalDate;

public record IdentifierOwnerResponse(
        String employeeTypeCode,
        String employeeNumber,
        boolean active,
        LocalDate ceasedOn,
        String message
) {
}
