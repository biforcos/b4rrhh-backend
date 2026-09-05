package com.b4rrhh.employee.contract.application.usecase;

import com.b4rrhh.employee.contract.application.command.CreateContractCommand;
import com.b4rrhh.employee.contract.application.model.ContractPlan;
import com.b4rrhh.employee.contract.application.port.EmployeeContractContext;
import com.b4rrhh.employee.contract.application.port.EmployeeContractLookupPort;
import com.b4rrhh.employee.contract.application.service.ContractSubtypeRelationValidator;
import com.b4rrhh.employee.contract.application.service.ContractCatalogValidator;
import com.b4rrhh.employee.contract.application.service.ContractTimelineService;
import com.b4rrhh.employee.contract.domain.exception.ContractEmployeeNotFoundException;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.contract.domain.port.ContractRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds a contract to the series. The add is planned against the invariants
 * of the series (ADR-057): inside the presence, no overlap, no gap. The one
 * automatic consequence is closing the contract in force on the new start
 * date the day before it. An add that starts on the start date of an
 * existing contract is not an add and is rejected as its correction
 * (backend#52).
 */
@Service
public class CreateContractService implements CreateContractUseCase {

    private final ContractRepository contractRepository;
    private final EmployeeContractLookupPort employeeContractLookupPort;
    private final ContractCatalogValidator contractCatalogValidator;
    private final ContractSubtypeRelationValidator contractSubtypeRelationValidator;
    private final ContractTimelineService contractTimelineService;

    public CreateContractService(
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
    public Contract create(CreateContractCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());

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

        String normalizedContractCode = contractCatalogValidator
                .normalizeRequiredCode("contractCode", command.contractCode());
        String normalizedContractSubtypeCode = contractCatalogValidator
                .normalizeRequiredCode("contractSubtypeCode", command.contractSubtypeCode());

        contractCatalogValidator.validateContractCode(
                normalizedRuleSystemCode,
                normalizedContractCode,
                command.startDate()
        );
        contractCatalogValidator.validateContractSubtypeCode(
                normalizedRuleSystemCode,
                normalizedContractSubtypeCode,
                command.startDate()
        );
        contractSubtypeRelationValidator.validateContractSubtypeRelation(
                normalizedRuleSystemCode,
                normalizedContractCode,
                normalizedContractSubtypeCode,
                command.startDate()
        );

        Contract newContract = new Contract(
                employee.employeeId(),
                normalizedContractCode,
                normalizedContractSubtypeCode,
                command.startDate(),
                command.endDate()
        );

        ContractPlan plan = contractTimelineService.planAdd(
                employee.employeeId(),
                new DateRange(newContract.getStartDate(), newContract.getEndDate())
        );
        contractTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        if (plan.adjustsAnOccurrence()) {
            Contract covering = contractRepository
                    .findByEmployeeIdAndStartDate(employee.employeeId(), plan.adjustedOccurrence().before().startDate())
                    .orElseThrow(() -> new IllegalStateException(
                            "Planned contract vanished: startDate=" + plan.adjustedOccurrence().before().startDate()
                    ));
            Contract closed = covering.adjustEndDate(plan.adjustedOccurrence().after().endDate());
            contractRepository.update(closed, closed.getStartDate());
        }

        contractRepository.save(newContract);
        return newContract;
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
