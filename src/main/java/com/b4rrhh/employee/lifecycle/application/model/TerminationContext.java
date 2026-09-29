package com.b4rrhh.employee.lifecycle.application.model;

import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class TerminationContext {

    private final String ruleSystemCode;
    private final String employeeTypeCode;
    private final String employeeNumber;
    private final LocalDate terminationDate;
    private final String exitReasonCode;
    private final Employee employee;
    private final boolean alreadyTerminated;
    private final TerminateEmployeeResult idempotentResult;

    private Presence closedPresence;
    private WorkCenter closedWorkCenter;
    private Contract closedContract;
    private LaborClassification closedLaborClassification;
    private WorkingTime closedWorkingTime;

    public TerminationContext(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate terminationDate,
            String exitReasonCode,
            Employee employee) {
        this.ruleSystemCode = ruleSystemCode;
        this.employeeTypeCode = employeeTypeCode;
        this.employeeNumber = employeeNumber;
        this.terminationDate = terminationDate;
        this.exitReasonCode = exitReasonCode;
        this.employee = employee;
        this.alreadyTerminated = false;
        this.idempotentResult = null;
    }

    public TerminationContext(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate terminationDate,
            String exitReasonCode,
            Employee employee,
            TerminateEmployeeResult idempotentResult) {
        this.ruleSystemCode = ruleSystemCode;
        this.employeeTypeCode = employeeTypeCode;
        this.employeeNumber = employeeNumber;
        this.terminationDate = terminationDate;
        this.exitReasonCode = exitReasonCode;
        this.employee = employee;
        this.alreadyTerminated = true;
        this.idempotentResult = idempotentResult;
    }

    public String ruleSystemCode() { return ruleSystemCode; }
    public String employeeTypeCode() { return employeeTypeCode; }
    public String employeeNumber() { return employeeNumber; }
    public LocalDate terminationDate() { return terminationDate; }
    public String exitReasonCode() { return exitReasonCode; }
    public Employee employee() { return employee; }
    public boolean isAlreadyTerminated() { return alreadyTerminated; }

    public void setClosedPresence(Presence p) { this.closedPresence = p; }
    public void setClosedWorkCenter(WorkCenter wc) { this.closedWorkCenter = wc; }
    public void setClosedContract(Contract c) { this.closedContract = c; }
    public void setClosedLaborClassification(LaborClassification lc) { this.closedLaborClassification = lc; }
    public void setClosedWorkingTime(WorkingTime wt) { this.closedWorkingTime = wt; }

    public Presence closedPresence() { return closedPresence; }
    public WorkCenter closedWorkCenter() { return closedWorkCenter; }
    public Contract closedContract() { return closedContract; }
    public LaborClassification closedLaborClassification() { return closedLaborClassification; }
    public WorkingTime closedWorkingTime() { return closedWorkingTime; }

    public TerminateEmployeeResult reconstructIdempotentResult() {
        if (!alreadyTerminated) {
            throw new IllegalStateException("Context is not in idempotent state");
        }
        return idempotentResult;
    }

    public void assertNoActivePresence() {
        if (closedPresence == null) {
            throw new TerminateEmployeeConflictException("No active presence found for employee");
        }
    }

    public Employee terminatedEmployee() {
        return new Employee(
                employee.getId(),
                employee.getRuleSystemCode(),
                employee.getEmployeeTypeCode(),
                employee.getEmployeeNumber(),
                employee.getFirstName(),
                employee.getLastName1(),
                employee.getLastName2(),
                employee.getPreferredName(),
                "TERMINATED",
                employee.getCreatedAt(),
                LocalDateTime.now(),
                employee.getPhotoUrl()
        );
    }

    public TerminateEmployeeResult toResult(String status) {
        Objects.requireNonNull(closedPresence, "closedPresence must be set before calling toResult()");
        return new TerminateEmployeeResult(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                terminationDate,
                exitReasonCode,
                status,
                closedPresence.getPresenceNumber(),
                closedPresence.getCompanyCode(),
                closedPresence.getEntryReasonCode(),
                closedPresence.getExitReasonCode(),
                closedPresence.getStartDate(),
                closedPresence.getEndDate(),
                closedContract != null ? closedContract.getContractCode() : null,
                closedContract != null ? closedContract.getContractSubtypeCode() : null,
                closedContract != null ? closedContract.getStartDate() : null,
                closedContract != null ? closedContract.getEndDate() : null,
                closedLaborClassification != null ? closedLaborClassification.getAgreementCode() : null,
                closedLaborClassification != null ? closedLaborClassification.getAgreementCategoryCode() : null,
                closedLaborClassification != null ? closedLaborClassification.getStartDate() : null,
                closedLaborClassification != null ? closedLaborClassification.getEndDate() : null,
                closedWorkCenter != null ? closedWorkCenter.getWorkCenterAssignmentNumber() : null,
                closedWorkCenter != null ? closedWorkCenter.getWorkCenterCode() : null,
                closedWorkCenter != null ? closedWorkCenter.getStartDate() : null,
                closedWorkCenter != null ? closedWorkCenter.getEndDate() : null,
                closedWorkingTime != null ? closedWorkingTime.getWorkingTimeNumber() : null,
                closedWorkingTime != null ? closedWorkingTime.getWorkingTimePercentage() : null,
                closedWorkingTime != null ? closedWorkingTime.getWeeklyHours() : null,
                closedWorkingTime != null ? closedWorkingTime.getDailyHours() : null,
                closedWorkingTime != null ? closedWorkingTime.getMonthlyHours() : null,
                closedWorkingTime != null ? closedWorkingTime.getStartDate() : null,
                closedWorkingTime != null ? closedWorkingTime.getEndDate() : null
        );
    }
}
