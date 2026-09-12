package com.b4rrhh.employee.lifecycle.application.model;

import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;

import java.time.LocalDate;
import java.util.Objects;

public class HireContext {

    // Normalized inputs — set once by HireEmployeePreConditionValidator
    private final String ruleSystemCode;
    private final String employeeTypeCode;
    private final String firstName;
    private final String lastName1;
    private final String lastName2;
    private final String preferredName;
    private final LocalDate hireDate;
    private final String companyCode;
    private final String entryReasonCode;
    private final String workCenterCode;
    private final HireEmployeeCommand.HireEmployeeContractCommand contract;
    private final HireEmployeeCommand.HireEmployeeLaborClassificationCommand laborClassification;
    private final HireEmployeeCommand.HireEmployeeCostCenterDistributionCommand costCenterDistribution;
    private final HireEmployeeCommand.HireEmployeeWorkingTimeCommand workingTime;

    // Set after nextEmployeeNumberPort.consumeNext()
    private String employeeNumber;

    // Participant results — written by each participant in order
    private Employee employee;
    private Presence presence;
    private WorkCenter workCenter;
    private CostCenterDistributionWindow costCenter;
    private Contract contractResult;
    private LaborClassification laborClassificationResult;
    private WorkingTime workingTimeResult;

    public HireContext(
            String ruleSystemCode, String employeeTypeCode,
            String firstName, String lastName1, String lastName2, String preferredName,
            LocalDate hireDate, String companyCode, String entryReasonCode, String workCenterCode,
            HireEmployeeCommand.HireEmployeeContractCommand contract,
            HireEmployeeCommand.HireEmployeeLaborClassificationCommand laborClassification,
            HireEmployeeCommand.HireEmployeeCostCenterDistributionCommand costCenterDistribution,
            HireEmployeeCommand.HireEmployeeWorkingTimeCommand workingTime) {
        this.ruleSystemCode = ruleSystemCode;
        this.employeeTypeCode = employeeTypeCode;
        this.firstName = firstName;
        this.lastName1 = lastName1;
        this.lastName2 = lastName2;
        this.preferredName = preferredName;
        this.hireDate = hireDate;
        this.companyCode = companyCode;
        this.entryReasonCode = entryReasonCode;
        this.workCenterCode = workCenterCode;
        this.contract = contract;
        this.laborClassification = laborClassification;
        this.costCenterDistribution = costCenterDistribution;
        this.workingTime = workingTime;
    }

    // Accessors for normalized inputs
    public String ruleSystemCode() { return ruleSystemCode; }
    public String employeeTypeCode() { return employeeTypeCode; }
    public String firstName() { return firstName; }
    public String lastName1() { return lastName1; }
    public String lastName2() { return lastName2; }
    public String preferredName() { return preferredName; }
    public LocalDate hireDate() { return hireDate; }
    public String companyCode() { return companyCode; }
    public String entryReasonCode() { return entryReasonCode; }
    public String workCenterCode() { return workCenterCode; }
    public HireEmployeeCommand.HireEmployeeContractCommand contract() { return contract; }
    public HireEmployeeCommand.HireEmployeeLaborClassificationCommand laborClassification() { return laborClassification; }
    public HireEmployeeCommand.HireEmployeeCostCenterDistributionCommand costCenterDistribution() { return costCenterDistribution; }
    public HireEmployeeCommand.HireEmployeeWorkingTimeCommand workingTime() { return workingTime; }

    public String employeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) {
        if (this.employeeNumber != null) {
            throw new IllegalStateException("employeeNumber is already set");
        }
        this.employeeNumber = employeeNumber;
    }

    public Employee employee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public Presence presence() { return presence; }
    public void setPresence(Presence presence) { this.presence = presence; }
    public WorkCenter workCenter() { return workCenter; }
    public void setWorkCenter(WorkCenter workCenter) { this.workCenter = workCenter; }
    public CostCenterDistributionWindow costCenter() { return costCenter; }
    public void setCostCenter(CostCenterDistributionWindow costCenter) { this.costCenter = costCenter; }
    public Contract contractResult() { return contractResult; }
    public void setContractResult(Contract contractResult) { this.contractResult = contractResult; }
    public LaborClassification laborClassificationResult() { return laborClassificationResult; }
    public void setLaborClassificationResult(LaborClassification laborClassificationResult) { this.laborClassificationResult = laborClassificationResult; }
    public WorkingTime workingTimeResult() { return workingTimeResult; }
    public void setWorkingTimeResult(WorkingTime workingTimeResult) { this.workingTimeResult = workingTimeResult; }

    public HireEmployeeResult toResult() {
        Objects.requireNonNull(employee, "employee must be set by EmployeeCoreParticipant before calling toResult()");
        Objects.requireNonNull(presence, "presence must be set by PresenceParticipant before calling toResult()");
        Objects.requireNonNull(workCenter, "workCenter must be set by WorkCenterParticipant before calling toResult()");
        Objects.requireNonNull(contractResult, "contractResult must be set by ContractParticipant before calling toResult()");
        Objects.requireNonNull(laborClassificationResult, "laborClassificationResult must be set by LaborClassificationParticipant before calling toResult()");
        Objects.requireNonNull(workingTimeResult, "workingTimeResult must be set by WorkingTimeParticipant before calling toResult()");
        return new HireEmployeeResult(
                new HireEmployeeResult.EmployeeSummary(
                        employee.getRuleSystemCode(),
                        employee.getEmployeeTypeCode(),
                        employee.getEmployeeNumber(),
                        employee.getFirstName(),
                        employee.getLastName1(),
                        employee.getLastName2(),
                        employee.getPreferredName(),
                        formatDisplayName(employee),
                        employee.getStatus(),
                        hireDate
                ),
                new HireEmployeeResult.PresenceSummary(
                        presence.getPresenceNumber(),
                        presence.getStartDate(),
                        presence.getCompanyCode(),
                        presence.getEntryReasonCode()
                ),
                new HireEmployeeResult.WorkCenterSummary(
                        workCenter.getStartDate(),
                        workCenter.getWorkCenterCode()
                ),
                costCenter != null ? new HireEmployeeResult.CostCenterSummary(
                        costCenter.getStartDate(),
                        costCenter.getTotalAllocationPercentage().doubleValue(),
                        costCenter.getItems().stream()
                                .map(item -> new HireEmployeeResult.CostCenterItemSummary(
                                        item.getCostCenterCode(),
                                        item.getAllocationPercentage().doubleValue()
                                ))
                                .toList()
                ) : null,
                new HireEmployeeResult.ContractSummary(
                        contractResult.getStartDate(),
                        contractResult.getContractCode(),
                        contractResult.getContractSubtypeCode()
                ),
                new HireEmployeeResult.LaborClassificationSummary(
                        laborClassificationResult.getStartDate(),
                        laborClassificationResult.getAgreementCode(),
                        laborClassificationResult.getAgreementCategoryCode()
                ),
                new HireEmployeeResult.WorkingTimeSummary(
                        workingTimeResult.getWorkingTimeNumber(),
                        workingTimeResult.getWorkingTimePercentage(),
                        workingTimeResult.getWeeklyHours(),
                        workingTimeResult.getDailyHours(),
                        workingTimeResult.getMonthlyHours(),
                        workingTimeResult.getStartDate(),
                        workingTimeResult.getEndDate()
                )
        );
    }

    // Note: this bypasses DisplayNameComputationService (per-rule-system format). Known limitation
    // carried forward from HireEmployeeService. Track as tech debt: compute via DisplayNameComputationService
    // in EmployeeCoreParticipant and store displayName as a context field.
    private static String formatDisplayName(Employee employee) {
        StringBuilder sb = new StringBuilder()
                .append(employee.getFirstName())
                .append(" ")
                .append(employee.getLastName1());
        if (employee.getLastName2() != null && !employee.getLastName2().isEmpty()) {
            sb.append(" ").append(employee.getLastName2());
        }
        return sb.toString();
    }
}
