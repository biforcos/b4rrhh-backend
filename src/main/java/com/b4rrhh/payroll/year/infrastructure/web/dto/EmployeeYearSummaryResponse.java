package com.b4rrhh.payroll.year.infrastructure.web.dto;

import java.util.List;

public record EmployeeYearSummaryResponse(
        int year,
        List<EmployeeYearPresenceResponse> presences,
        List<EmployeeYearMonthResponse> months,
        List<EmployeeYearAbsenceResponse> absences
) {
}
