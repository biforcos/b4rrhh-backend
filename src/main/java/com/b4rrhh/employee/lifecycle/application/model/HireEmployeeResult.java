package com.b4rrhh.employee.lifecycle.application.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HireEmployeeResult(
        EmployeeSummary employee,
        PresenceSummary presence,
        WorkCenterSummary workCenter,
        CostCenterSummary costCenter,
        ContractSummary contract,
        LaborClassificationSummary laborClassification,
        WorkingTimeSummary workingTime
) {
    public record EmployeeSummary(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String firstName,
            String lastName1,
            String lastName2,
            String preferredName,
            String displayName,
            String status,
            LocalDate hireDate
    ) {}

    public record PresenceSummary(
            Integer presenceNumber,
            LocalDate startDate,
            String companyCode,
            String entryReasonCode
    ) {}

    /** El centro que el alta acaba de asignar. Sin nombre: el ciclo de vida no lo tiene, y la
     * respuesta de un comando dice lo que ha creado, no la ficha (backend#36). */
    public record WorkCenterSummary(
            LocalDate startDate,
            String workCenterCode
    ) {}

    public record CostCenterSummary(
            LocalDate startDate,
            Double totalAllocationPercentage,
            List<CostCenterItemSummary> items
    ) {}

    /** Sin nombre, por lo mismo: el literal del centro de coste lo resuelve la capa web de su
     * vertical, con el idioma de la respuesta, y en los endpoints de comando va a null (backend#27). */
    public record CostCenterItemSummary(
            String costCenterCode,
            Double allocationPercentage
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

    public record WorkingTimeSummary(
            Integer workingTimeNumber,
            BigDecimal workingTimePercentage,
            BigDecimal weeklyHours,
            BigDecimal dailyHours,
            BigDecimal monthlyHours,
            LocalDate startDate,
            LocalDate endDate
    ) {}
}
