package com.b4rrhh.employee.address.infrastructure.web.dto;

import java.time.LocalDate;

/** A stretch of dates named by a plan or an error: a gap, or an overlap. Open end date means onwards. */
public record AddressPeriodResponse(
        LocalDate startDate,
        LocalDate endDate
) {
}
