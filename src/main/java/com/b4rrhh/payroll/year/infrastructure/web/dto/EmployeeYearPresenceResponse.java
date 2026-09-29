package com.b4rrhh.payroll.year.infrastructure.web.dto;

import java.time.LocalDate;

public record EmployeeYearPresenceResponse(int presenceNumber, LocalDate startDate, LocalDate endDate) {
}
