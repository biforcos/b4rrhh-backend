package com.b4rrhh.payroll.infrastructure.web.dto;

import com.b4rrhh.payroll.domain.model.PayrollStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PayrollResponse(
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
        List<PayrollWarningResponse> warnings,
        List<PayrollConceptResponse> concepts,
        List<PayrollContextSnapshotResponse> contextSnapshots,
        PayrollCompanyProfileResponse companyProfile,
        PayrollEmployeeProfileResponse employeeProfile,
        PayrollAgreementProfileResponse agreementProfile,
        String presenceStartDate,
        String presenceEndDate,
        String workCenterCode,
        String workCenterName
) {
}