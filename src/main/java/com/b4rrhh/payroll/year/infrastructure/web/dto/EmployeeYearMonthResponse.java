package com.b4rrhh.payroll.year.infrastructure.web.dto;

public record EmployeeYearMonthResponse(
        String payrollPeriodCode,
        String payrollState,
        int payrollInputCount,
        int payrollInputConceptCount,
        int activeRetroMarkCount,
        int consumedRetroMarkCount
) {
}
