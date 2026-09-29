package com.b4rrhh.employee.lifecycle.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RehireEmployeeCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        LocalDate rehireDate,
        String entryReasonCode,
        String companyCode,
        String agreementCode,
        String agreementCategoryCode,
        String contractTypeCode,
        String contractSubtypeCode,
        String workCenterCode,
        RehireEmployeeCostCenterDistributionCommand costCenterDistribution,
        RehireEmployeeWorkingTimeCommand workingTime,
        RehireEmployeeIdentifierCommand identifier
) {
    /** La readmision sin documento, que sigue valiendo: el documento es opcional aqui. */
    public RehireEmployeeCommand(
            String ruleSystemCode, String employeeTypeCode, String employeeNumber,
            LocalDate rehireDate, String entryReasonCode, String companyCode,
            String agreementCode, String agreementCategoryCode,
            String contractTypeCode, String contractSubtypeCode, String workCenterCode,
            RehireEmployeeCostCenterDistributionCommand costCenterDistribution,
            RehireEmployeeWorkingTimeCommand workingTime) {
        this(ruleSystemCode, employeeTypeCode, employeeNumber, rehireDate, entryReasonCode, companyCode,
                agreementCode, agreementCategoryCode, contractTypeCode, contractSubtypeCode, workCenterCode,
                costCenterDistribution, workingTime, null);
    }

    /** El documento del readmitido, si llega (b4rrhh/backend#141): tiene que ser el suyo. */
    public record RehireEmployeeIdentifierCommand(
            String identifierTypeCode,
            String identifierValue,
            String issuingCountryCode
    ) {}

    public record RehireEmployeeCostCenterDistributionCommand(
            List<RehireEmployeeCostCenterItemCommand> items
    ) {}

    public record RehireEmployeeCostCenterItemCommand(
            String costCenterCode,
            BigDecimal allocationPercentage
    ) {}

    public record RehireEmployeeWorkingTimeCommand(
            BigDecimal workingTimePercentage
    ) {}
}
