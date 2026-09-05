package com.b4rrhh.employee.contract.application.usecase;

import com.b4rrhh.employee.contract.application.command.UpdateContractCommand;
import com.b4rrhh.employee.contract.application.model.ContractPlan;
import com.b4rrhh.employee.contract.application.port.EmployeeContractContext;
import com.b4rrhh.employee.contract.application.port.EmployeeContractLookupPort;
import com.b4rrhh.employee.contract.application.service.ContractSubtypeRelationValidator;
import com.b4rrhh.employee.contract.application.service.ContractCatalogValidator;
import com.b4rrhh.employee.contract.application.service.ContractTimelineService;
import com.b4rrhh.employee.contract.domain.exception.ContractEmployeeNotFoundException;
import com.b4rrhh.employee.contract.domain.exception.ContractNotFoundException;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.contract.domain.port.ContractRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Corrects a contract: its codes, its dates, or both. Nothing else moves
 * (ADR-057, decision 3): if the corrected dates leave a gap or an overlap,
 * the plan rejects them and names what the user would have to stretch
 * instead. A closed contract can be corrected too: what identifies it is the
 * day it starts, and a wrong type on a past contract is still wrong.
 */
@Service
public class UpdateContractService implements UpdateContractUseCase {

    private final ContractRepository contractRepository;
    private final EmployeeContractLookupPort employeeContractLookupPort;
    private final ContractCatalogValidator contractCatalogValidator;
    private final ContractSubtypeRelationValidator contractSubtypeRelationValidator;
    private final ContractTimelineService contractTimelineService;

    public UpdateContractService(
            ContractRepository contractRepository,
            EmployeeContractLookupPort employeeContractLookupPort,
            ContractCatalogValidator contractCatalogValidator,
            ContractSubtypeRelationValidator contractSubtypeRelationValidator,
            ContractTimelineService contractTimelineService
    ) {
        this.contractRepository = contractRepository;
        this.employeeContractLookupPort = employeeContractLookupPort;
        this.contractCatalogValidator = contractCatalogValidator;
        this.contractSubtypeRelationValidator = contractSubtypeRelationValidator;
        this.contractTimelineService = contractTimelineService;
    }

    @Override
    @Transactional
    public Contract update(UpdateContractCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        LocalDate normalizedStartDate = normalizeStartDate(command.startDate());

        EmployeeContractContext employee = employeeContractLookupPort
                .findByBusinessKeyForUpdate(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                )
                .orElseThrow(() -> new ContractEmployeeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                ));

        Contract existing = contractRepository
                .findByEmployeeIdAndStartDate(employee.employeeId(), normalizedStartDate)
                .orElseThrow(() -> new ContractNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber,
                        normalizedStartDate
                ));

        LocalDate correctedStartDate = (command.newStartDate() != null)
                ? command.newStartDate()
                : normalizedStartDate;

        String normalizedContractCode = contractCatalogValidator
                .normalizeRequiredCode("contractCode", command.contractCode());
        String normalizedContractSubtypeCode = contractCatalogValidator
                .normalizeRequiredCode("contractSubtypeCode", command.contractSubtypeCode());

        contractCatalogValidator.validateContractCode(
                normalizedRuleSystemCode,
                normalizedContractCode,
                correctedStartDate
        );
        contractCatalogValidator.validateContractSubtypeCode(
                normalizedRuleSystemCode,
                normalizedContractSubtypeCode,
                correctedStartDate
        );
        contractSubtypeRelationValidator.validateContractSubtypeRelation(
                normalizedRuleSystemCode,
                normalizedContractCode,
                normalizedContractSubtypeCode,
                correctedStartDate
        );

        Contract corrected = new Contract(
                employee.employeeId(),
                normalizedContractCode,
                normalizedContractSubtypeCode,
                correctedStartDate,
                command.endDate()
        );

        ContractPlan plan = contractTimelineService.planCorrect(
                employee.employeeId(),
                existing,
                new DateRange(corrected.getStartDate(), corrected.getEndDate())
        );
        contractTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        contractRepository.update(corrected, normalizedStartDate);
        return corrected;
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

    private LocalDate normalizeStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }

        return startDate;
    }
}
