package com.b4rrhh.employee.lifecycle.application.service;

import com.b4rrhh.employee.contract.application.command.ListEmployeeContractsCommand;
import com.b4rrhh.employee.contract.application.usecase.ListEmployeeContractsUseCase;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.presence.domain.model.EmployeeStanding;
import com.b4rrhh.employee.presence.domain.model.PresencePeriod;
import com.b4rrhh.employee.labor_classification.application.command.ListEmployeeLaborClassificationsCommand;
import com.b4rrhh.employee.labor_classification.application.usecase.ListEmployeeLaborClassificationsUseCase;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.TerminateEmployeeResult;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeEmployeeNotFoundException;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeRequestInvalidException;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.workcenter.application.usecase.ListEmployeeWorkCentersUseCase;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesCommand;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesUseCase;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class TerminationPreConditionValidator {

    private final GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKey;
    private final ListEmployeePresencesUseCase listPresences;
    private final ListEmployeeContractsUseCase listContracts;
    private final ListEmployeeLaborClassificationsUseCase listLaborClassifications;
    private final ListEmployeeWorkCentersUseCase listWorkCenters;
    private final ListEmployeeWorkingTimesUseCase listWorkingTimes;

    public TerminationPreConditionValidator(
            GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKey,
            ListEmployeePresencesUseCase listPresences,
            ListEmployeeContractsUseCase listContracts,
            ListEmployeeLaborClassificationsUseCase listLaborClassifications,
            ListEmployeeWorkCentersUseCase listWorkCenters,
            ListEmployeeWorkingTimesUseCase listWorkingTimes) {
        this.getEmployeeByBusinessKey = getEmployeeByBusinessKey;
        this.listPresences = listPresences;
        this.listContracts = listContracts;
        this.listLaborClassifications = listLaborClassifications;
        this.listWorkCenters = listWorkCenters;
        this.listWorkingTimes = listWorkingTimes;
    }

    private static String statusToday(List<Presence> presences) {
        return EmployeeStanding.on(LocalDate.now(), presences.stream()
                        .map(p -> new PresencePeriod(p.getStartDate(), p.getEndDate()))
                        .toList())
                .status().name();
    }

    public TerminationContext validateAndLookup(TerminateEmployeeCommand command) {
        String ruleSystemCode = requireNonBlank(command.ruleSystemCode(), "ruleSystemCode");
        String employeeTypeCode = requireNonBlank(command.employeeTypeCode(), "employeeTypeCode");
        String employeeNumber = requireNonBlank(command.employeeNumber(), "employeeNumber");
        if (command.terminationDate() == null) {
            throw new TerminateEmployeeRequestInvalidException("terminationDate is required");
        }
        LocalDate terminationDate = command.terminationDate();
        String exitReasonCode = requireNonBlank(command.exitReasonCode(), "exitReasonCode");

        Employee employee = getEmployeeByBusinessKey
                .getByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .orElseThrow(() -> new TerminateEmployeeEmployeeNotFoundException(
                        ruleSystemCode, employeeTypeCode, employeeNumber));

        if (employee.isTerminated()) {
            return buildIdempotentContext(ruleSystemCode, employeeTypeCode, employeeNumber,
                    terminationDate, exitReasonCode, employee);
        }

        return new TerminationContext(ruleSystemCode, employeeTypeCode, employeeNumber,
                terminationDate, exitReasonCode, employee);
    }

    private TerminationContext buildIdempotentContext(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate terminationDate,
            String exitReasonCode,
            Employee employee) {

        List<Presence> presences = listPresences
                .listByEmployeeBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber);
        Presence closedPresence = presences.stream()
                .filter(p -> terminationDate.equals(p.getEndDate())
                        && exitReasonCode.equals(p.getExitReasonCode()))
                .findFirst().orElse(null);

        List<Contract> contracts = listContracts
                .listByEmployeeBusinessKey(new ListEmployeeContractsCommand(
                        ruleSystemCode, employeeTypeCode, employeeNumber));
        Contract closedContract = contracts.stream()
                .filter(c -> terminationDate.equals(c.getEndDate()))
                .findFirst().orElse(null);

        List<LaborClassification> laborClassifications = listLaborClassifications
                .listByEmployeeBusinessKey(new ListEmployeeLaborClassificationsCommand(
                        ruleSystemCode, employeeTypeCode, employeeNumber));
        LaborClassification closedLaborClassification = laborClassifications.stream()
                .filter(lc -> terminationDate.equals(lc.getEndDate()))
                .findFirst().orElse(null);

        List<WorkCenter> workCenters = listWorkCenters
                .listByEmployeeBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber);
        WorkCenter closedWorkCenter = workCenters.stream()
                .filter(wc -> terminationDate.equals(wc.getEndDate()))
                .findFirst().orElse(null);

        List<WorkingTime> workingTimes = listWorkingTimes
                .listByEmployeeBusinessKey(new ListEmployeeWorkingTimesCommand(
                        ruleSystemCode, employeeTypeCode, employeeNumber));
        WorkingTime closedWorkingTime = workingTimes.stream()
                .filter(wt -> terminationDate.equals(wt.getEndDate()))
                .findFirst().orElse(null);

        TerminateEmployeeResult idempotentResult = new TerminateEmployeeResult(
                ruleSystemCode, employeeTypeCode, employeeNumber,
                terminationDate, exitReasonCode, statusToday(presences),
                closedPresence != null ? closedPresence.getPresenceNumber() : null,
                closedPresence != null ? closedPresence.getCompanyCode() : null,
                closedPresence != null ? closedPresence.getEntryReasonCode() : null,
                closedPresence != null ? closedPresence.getExitReasonCode() : null,
                closedPresence != null ? closedPresence.getStartDate() : null,
                closedPresence != null ? closedPresence.getEndDate() : null,
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

        return new TerminationContext(ruleSystemCode, employeeTypeCode, employeeNumber,
                terminationDate, exitReasonCode, employee, idempotentResult);
    }

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new TerminateEmployeeRequestInvalidException(fieldName + " is required");
        }
        return value.trim().toUpperCase();
    }
}
