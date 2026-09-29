package com.b4rrhh.employee.lifecycle.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HireEmployeeCommand(
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
        HireEmployeeContractCommand contract,
        HireEmployeeLaborClassificationCommand laborClassification,
        HireEmployeeCostCenterDistributionCommand costCenterDistribution,
        HireEmployeeWorkingTimeCommand workingTime,
        HireEmployeeIdentifierCommand identifier
) {
    /** El alta sin documento, tal como era antes del b4rrhh/backend#141: la rechaza el validador. */
    public HireEmployeeCommand(
            String ruleSystemCode, String employeeTypeCode,
            String firstName, String lastName1, String lastName2, String preferredName,
            LocalDate hireDate, String entryReasonCode, String companyCode, String workCenterCode,
            HireEmployeeContractCommand contract,
            HireEmployeeLaborClassificationCommand laborClassification,
            HireEmployeeCostCenterDistributionCommand costCenterDistribution,
            HireEmployeeWorkingTimeCommand workingTime) {
        this(ruleSystemCode, employeeTypeCode, firstName, lastName1, lastName2, preferredName,
                hireDate, entryReasonCode, companyCode, workCenterCode,
                contract, laborClassification, costCenterDistribution, workingTime, null);
    }

    /** El documento que identifica a la persona (b4rrhh/backend#141). */
    public record HireEmployeeIdentifierCommand(
            String identifierTypeCode,
            String identifierValue,
            String issuingCountryCode,
            LocalDate expirationDate
    ) {
        public HireEmployeeIdentifierCommand(String identifierTypeCode, String identifierValue, String issuingCountryCode) {
            this(identifierTypeCode, identifierValue, issuingCountryCode, null);
        }
    }

    public record HireEmployeeContractCommand(
            String contractTypeCode,
            String contractSubtypeCode
    ) {}

    public record HireEmployeeLaborClassificationCommand(
            String agreementCode,
            String agreementCategoryCode
    ) {}

    public record HireEmployeeCostCenterDistributionCommand(
            List<HireEmployeeCostCenterItemCommand> items
    ) {}

    public record HireEmployeeCostCenterItemCommand(
            String costCenterCode,
            Double allocationPercentage
    ) {}

    public record HireEmployeeWorkingTimeCommand(
            BigDecimal workingTimePercentage
    ) {}
}
