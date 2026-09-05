package com.b4rrhh.employee.address.infrastructure.web.dto;

import java.time.LocalDate;

/** An address as a plan or an error names it. The number is null only for the one a plan would add. */
public record AddressOccurrenceResponse(
        Integer addressNumber,
        LocalDate startDate,
        LocalDate endDate
) {
}
