package com.b4rrhh.payroll.infrastructure.web.dto;

import java.time.Instant;
import java.time.LocalDateTime;

public record PayrollSummaryResponse(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String payrollPeriodCode,
        String payrollTypeCode,
        Integer presenceNumber,
        String status,
        Instant calculatedAt
) {}
