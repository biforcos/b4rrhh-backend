package com.b4rrhh.payroll.infrastructure.web.dto;

import java.time.LocalDateTime;

public record PayrollCalculationRunMessageResponse(
        String messageCode,
        String messageCodeName,
        String severityCode,
        String message,
        String detailsJson,
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String payrollPeriodCode,
        String payrollTypeCode,
        Integer presenceNumber,
        LocalDateTime createdAt
) {
}