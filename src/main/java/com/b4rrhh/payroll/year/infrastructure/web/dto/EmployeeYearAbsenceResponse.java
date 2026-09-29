package com.b4rrhh.payroll.year.infrastructure.web.dto;

import java.time.LocalDate;

public record EmployeeYearAbsenceResponse(
        String absenceTypeCode,
        LocalDate startDate,
        LocalDate endDate,
        boolean benefitEntitled
) {
}
