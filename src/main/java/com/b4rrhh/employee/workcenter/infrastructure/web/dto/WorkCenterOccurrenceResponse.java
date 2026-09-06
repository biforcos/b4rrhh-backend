package com.b4rrhh.employee.workcenter.infrastructure.web.dto;

import java.time.LocalDate;

/** An assignment as a plan or an error names it. The number is null only for the one a plan would add. */
public record WorkCenterOccurrenceResponse(
        Integer workCenterAssignmentNumber,
        LocalDate startDate,
        LocalDate endDate
) {
}
