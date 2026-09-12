package com.b4rrhh.employee.lifecycle.application.model;

import java.time.LocalDate;
import java.util.List;

public record RehireEmployeeResult(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        LocalDate rehireDate,
        String status,
        Integer newPresenceNumber,
        String newPresenceCompanyCode,
        String newPresenceEntryReasonCode,
        LocalDate newPresenceStartDate,
        String newContractTypeCode,
        String newContractSubtypeCode,
        LocalDate newContractStartDate,
        String newAgreementCode,
        String newAgreementCategoryCode,
        LocalDate newLaborClassificationStartDate,
        Integer newWorkCenterAssignmentNumber,
        String newWorkCenterCode,
        LocalDate newWorkCenterStartDate,
        CostCenterSummary newCostCenter,
        WorkingTimeSummary newWorkingTime,
        boolean created
) {
    public record CostCenterSummary(
            LocalDate startDate,
            Double totalAllocationPercentage,
            List<CostCenterItemSummary> items
    ) {}

    /** Sin nombre: el literal del centro de coste lo resuelve la capa web de su vertical, con el
     * idioma de la respuesta, y en los endpoints de comando va a null (backend#27, backend#36). */
    public record CostCenterItemSummary(
            String costCenterCode,
            Double allocationPercentage
    ) {}

    public record WorkingTimeSummary(
            Integer workingTimeNumber,
            java.math.BigDecimal workingTimePercentage,
            java.math.BigDecimal weeklyHours,
            java.math.BigDecimal dailyHours,
            java.math.BigDecimal monthlyHours,
            LocalDate startDate,
            LocalDate endDate
    ) {}
}
