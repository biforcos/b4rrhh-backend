package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.model.PayrollConcept;
import com.b4rrhh.payroll.domain.model.PayrollContextSnapshot;
import com.b4rrhh.payroll.domain.model.PayrollSegment;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.model.PayrollWarning;

import java.time.LocalDateTime;
import java.util.List;

public record CalculatePayrollCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String payrollPeriodCode,
        String payrollTypeCode,
        Integer presenceNumber,
        PayrollStatus status,
        String statusReasonCode,
        LocalDateTime calculatedAt,
        String calculationEngineCode,
        String calculationEngineVersion,
        Long runId,
        List<PayrollWarning> warnings,
        List<PayrollConcept> concepts,
        List<PayrollContextSnapshot> contextSnapshots,
        List<PayrollSegment> segments
) {
}