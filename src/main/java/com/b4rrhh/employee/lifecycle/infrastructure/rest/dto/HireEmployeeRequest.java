package com.b4rrhh.employee.lifecycle.infrastructure.rest.dto;

import java.time.LocalDate;
import java.util.List;

public record HireEmployeeRequest(
        String ruleSystemCode,
        String employeeTypeCode,
        String firstName,
        String lastName1,
        String lastName2,
        String preferredName,
        LocalDate hireDate,
        String entryReasonCode,
        String companyCode,
        String workCenterCode,
        HireEmployeeCostCenterDistributionRequest costCenterDistribution,
        HireContractRequest contract,
        HireLaborClassificationRequest laborClassification,
        HireEmployeeWorkingTimeRequest workingTime,
        HireIdentifierRequest identifier
) {
    public record HireIdentifierRequest(
            String identifierTypeCode,
            String identifierValue,
            String issuingCountryCode,
            LocalDate expirationDate
    ) {}

    public record HireEmployeeCostCenterDistributionRequest(
            List<HireEmployeeCostCenterItemRequest> items
    ) {}

    public record HireEmployeeCostCenterItemRequest(
            String costCenterCode,
            Double allocationPercentage
    ) {}
}
