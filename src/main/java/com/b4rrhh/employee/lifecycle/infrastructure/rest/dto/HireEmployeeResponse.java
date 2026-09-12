package com.b4rrhh.employee.lifecycle.infrastructure.rest.dto;

import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterDistributionWindowResponse;
import java.time.LocalDate;

public record HireEmployeeResponse(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String firstName,
        String lastName1,
        String lastName2,
        String preferredName,
        String displayName,
        String status,
        LocalDate hireDate,
        PresenceSummary initialPresence,
        WorkCenterSummary initialWorkCenter,
        CostCenterDistributionWindowResponse costCenter,
        ContractSummary initialContract,
        LaborClassificationSummary initialLaborClassification,
        HiredWorkingTimeResponse workingTime
) {
    public record PresenceSummary(
            Integer presenceNumber,
            LocalDate startDate,
            String companyCode,
            String entryReasonCode
    ) {}

    public record WorkCenterSummary(
            LocalDate startDate,
            String workCenterCode
    ) {}

    public record ContractSummary(
            LocalDate startDate,
            String contractTypeCode,
            String contractSubtypeCode
    ) {}

    public record LaborClassificationSummary(
            LocalDate startDate,
            String agreementCode,
            String agreementCategoryCode
    ) {}
}
