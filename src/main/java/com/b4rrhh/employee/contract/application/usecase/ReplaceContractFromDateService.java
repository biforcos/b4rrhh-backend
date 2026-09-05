package com.b4rrhh.employee.contract.application.usecase;

import com.b4rrhh.employee.contract.application.command.ReplaceContractFromDateCommand;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Adapter kept for the screen and the loader that still call
 * {@code replace-from-date}. It is an add with the end date the old
 * {@code ReplaceMode} used to derive: the tail of the contract in force on
 * the effective date, or open when none is. The component then does what
 * the old planner did: closes the covering contract the day before
 * ({@code SPLIT}), or inserts when nothing covers ({@code NO_COVERING}). What
 * changes is {@code EXACT_START}: starting on the very start date of an
 * existing contract no longer replaces it silently, it is rejected as its
 * correction (backend#52) and the correction is asked for as such (PUT).
 *
 * @deprecated ADR-057 retires {@code Replace…FromDate} as a model. Adding a
 *     contract already closes the one in force. To be removed once the
 *     contract screen has migrated to add-with-dates.
 */
@Deprecated
@Service
public class ReplaceContractFromDateService implements ReplaceContractFromDateUseCase {

    private final ContractRepository contractRepository;
    private final EmployeeContractLookupPort employeeContractLookupPort;
    private final ContractCatalogValidator contractCatalogValidator;
    private final ContractSubtypeRelationValidator contractSubtypeRelationValidator;
    private final ContractTimelineService contractTimelineService;

    public ReplaceContractFromDateService(
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
    public Contract replaceFromDate(ReplaceContractFromDateCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        LocalDate normalizedEffectiveDate = normalizeEffectiveDate(command.effectiveDate());

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
                normalizedEffectiveDate
        );
        contractCatalogValidator.validateContractSubtypeCode(
                normalizedRuleSystemCode,
                normalizedContractSubtypeCode,
                normalizedEffectiveDate
        );
        contractSubtypeRelationValidator.validateContractSubtypeRelation(
                normalizedRuleSystemCode,
                normalizedContractCode,
                normalizedContractSubtypeCode,
                normalizedEffectiveDate
        );

        // The old planner gave the replacement the tail of the contract in force on
        // the effective date (SPLIT) or left it open when nothing covered it
        // (NO_COVERING). The end date is all that is derived here; the rest is the
        // component's judgement.
        Contract replacement = new Contract(
                employee.employeeId(),
                normalizedContractCode,
                normalizedContractSubtypeCode,
                normalizedEffectiveDate,
                endDateOfContractInForceOn(employee.employeeId(), normalizedEffectiveDate)
        );

        ContractPlan plan = contractTimelineService.planAdd(
                employee.employeeId(),
                new DateRange(replacement.getStartDate(), replacement.getEndDate())
        );
        contractTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        if (plan.adjustsAnOccurrence()) {
            LocalDate coveringStartDate = plan.adjustedOccurrence().before().startDate();
            Contract covering = contractRepository
                    .findByEmployeeIdAndStartDate(employee.employeeId(), coveringStartDate)
                    .orElseThrow(() -> new IllegalStateException(
                            "Planned contract vanished: startDate=" + coveringStartDate
                    ));
            Contract closed = covering.adjustEndDate(plan.adjustedOccurrence().after().endDate());
            contractRepository.update(closed, closed.getStartDate());
        }

        contractRepository.save(replacement);
        return replacement;
    }

    private LocalDate endDateOfContractInForceOn(Long employeeId, LocalDate date) {
        List<Contract> history = contractRepository.findByEmployeeIdOrderByStartDate(employeeId);
        for (Contract contract : history) {
            boolean startsOnOrBefore = !contract.getStartDate().isAfter(date);
            boolean reaches = contract.getEndDate() == null || !contract.getEndDate().isBefore(date);
            if (startsOnOrBefore && reaches) {
                return contract.getEndDate();
            }
        }

        return null;
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

    private LocalDate normalizeEffectiveDate(LocalDate effectiveDate) {
        if (effectiveDate == null) {
            throw new IllegalArgumentException("effectiveDate is required");
        }

        return effectiveDate;
    }
}
