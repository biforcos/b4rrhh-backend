package com.b4rrhh.employee.lifecycle.infrastructure.rest.dto;

import java.time.LocalDate;

/**
 * Si el documento está bien escrito y, si lo está y tiene dueño, quién es (backend#149, backend#171).
 * Los campos del dueño van vacíos cuando no lo hay.
 */
public record IdentifierOwnerResponse(
        boolean valid,
        String reason,
        String employeeTypeCode,
        String employeeNumber,
        Boolean active,
        LocalDate ceasedOn,
        String message
) {
}
