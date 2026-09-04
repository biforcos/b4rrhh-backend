package com.b4rrhh.employee.working_time.infrastructure.web.dto;

import java.time.LocalDate;

/** An occurrence as a plan or an error names it. The number is null only for the one a plan would add. */
public record WorkingTimeOccurrenceResponse(
        Integer workingTimeNumber,
        LocalDate startDate,
        LocalDate endDate
) {
}
