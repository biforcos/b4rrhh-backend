package com.b4rrhh.payroll.infrastructure.web.dto;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Un recibo en la lista.
 *
 * @param employeeName el nombre del empleado para la lista (b4rrhh/frontend#143); null si no se conoce
 */
public record PayrollSummaryResponse(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String payrollPeriodCode,
        String payrollTypeCode,
        Integer presenceNumber,
        String status,
        Instant calculatedAt,
        boolean sharesPeriodWithAnotherPresence,
        String employeeName
) {}
