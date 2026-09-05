package com.b4rrhh.employee.contract.application.usecase;

import com.b4rrhh.employee.contract.application.command.PlanContractChangeCommand;
import com.b4rrhh.employee.contract.application.model.ContractPlan;
import com.b4rrhh.employee.contract.application.port.EmployeeContractContext;
import com.b4rrhh.employee.contract.application.port.EmployeeContractLookupPort;
import com.b4rrhh.employee.contract.application.service.ContractTimelineService;
import com.b4rrhh.employee.contract.domain.exception.ContractEmployeeNotFoundException;
import com.b4rrhh.employee.contract.domain.exception.ContractNotFoundException;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.contract.domain.port.ContractRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers what an add, a removal or a correction would do to the series
 * without applying it (ADR-057, decision 6). Rejected plans come back as
 * plans, not as errors: the screen shows the gap, the overlap or the
 * contract an add would correct, and the user decides.
 */
@Service
public class PlanContractChangeService implements PlanContractChangeUseCase {

    private final ContractRepository contractRepository;
    private final EmployeeContractLookupPort employeeContractLookupPort;
    private final ContractTimelineService contractTimelineService;

    public PlanContractChangeService(
            ContractRepository contractRepository,
            EmployeeContractLookupPort employeeContractLookupPort,
            ContractTimelineService contractTimelineService
    ) {
        this.contractRepository = contractRepository;
        this.employeeContractLookupPort = employeeContractLookupPort;
        this.contractTimelineService = contractTimelineService;
    }

    @Override
    @Transactional(readOnly = true)
    public ContractPlan plan(PlanContractChangeCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        if (command.operation() == null) {
            throw new IllegalArgumentException("operation is required");
        }

        EmployeeContractContext employee = employeeContractLookupPort
                .findByBusinessKey(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                )
                .orElseThrow(() -> new ContractEmployeeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                ));

        return switch (command.operation()) {
            case ADD -> contractTimelineService.planAdd(
                    employee.employeeId(),
                    requireDates(command)
            );
            case REMOVE -> contractTimelineService.planRemove(
                    employee.employeeId(),
                    requireContract(command, employee)
            );
            case CORRECT -> contractTimelineService.planCorrect(
                    employee.employeeId(),
                    requireContract(command, employee),
                    requireDates(command)
            );
        };
    }

    private Contract requireContract(PlanContractChangeCommand command, EmployeeContractContext employee) {
        if (command.contractStartDate() == null) {
            throw new IllegalArgumentException("contractStartDate is required");
        }

        return contractRepository
                .findByEmployeeIdAndStartDate(employee.employeeId(), command.contractStartDate())
                .orElseThrow(() -> new ContractNotFoundException(
                        employee.ruleSystemCode(),
                        employee.employeeTypeCode(),
                        employee.employeeNumber(),
                        command.contractStartDate()
                ));
    }

    private static DateRange requireDates(PlanContractChangeCommand command) {
        if (command.startDate() == null) {
            throw new IllegalArgumentException("startDate is required");
        }

        return new DateRange(command.startDate(), command.endDate());
    }

    private String normalizeRuleSystemCode(String ruleSystemCode) {
        if (ruleSystemCode == null || ruleSystemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("ruleSystemCode is required");
        }

        return ruleSystemCode.trim().toUpperCase();
    }

    private String normalizeEmployeeTypeCode(String employeeTypeCode) {
        if (employeeTypeCode == null || employeeTypeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("employeeTypeCode is required");
        }

        return employeeTypeCode.trim().toUpperCase();
    }

    private String normalizeEmployeeNumber(String employeeNumber) {
        if (employeeNumber == null || employeeNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("employeeNumber is required");
        }

        return employeeNumber.trim();
    }
}
