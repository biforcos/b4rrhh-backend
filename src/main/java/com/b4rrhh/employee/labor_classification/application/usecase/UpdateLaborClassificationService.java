package com.b4rrhh.employee.labor_classification.application.usecase;

import com.b4rrhh.employee.labor_classification.application.command.UpdateLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.model.LaborClassificationPlan;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationContext;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationLookupPort;
import com.b4rrhh.employee.labor_classification.application.service.AgreementCategoryRelationValidator;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationCatalogValidator;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationTimelineService;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationEmployeeNotFoundException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationNotFoundException;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.domain.port.LaborClassificationRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Corrects a labor classification: its codes, its dates, or both. Nothing
 * else moves (ADR-057, decision 3): if the corrected dates leave a gap or an
 * overlap, the plan rejects them and names what the user would have to
 * stretch instead. A closed occurrence can be corrected too: what identifies
 * it is the day it starts, and a wrong category on a past one is still wrong.
 */
@Service
public class UpdateLaborClassificationService implements UpdateLaborClassificationUseCase {

    private final LaborClassificationRepository laborClassificationRepository;
    private final EmployeeLaborClassificationLookupPort employeeLaborClassificationLookupPort;
    private final LaborClassificationCatalogValidator laborClassificationCatalogValidator;
    private final AgreementCategoryRelationValidator agreementCategoryRelationValidator;
    private final LaborClassificationTimelineService laborClassificationTimelineService;

    public UpdateLaborClassificationService(
            LaborClassificationRepository laborClassificationRepository,
            EmployeeLaborClassificationLookupPort employeeLaborClassificationLookupPort,
            LaborClassificationCatalogValidator laborClassificationCatalogValidator,
            AgreementCategoryRelationValidator agreementCategoryRelationValidator,
            LaborClassificationTimelineService laborClassificationTimelineService
    ) {
        this.laborClassificationRepository = laborClassificationRepository;
        this.employeeLaborClassificationLookupPort = employeeLaborClassificationLookupPort;
        this.laborClassificationCatalogValidator = laborClassificationCatalogValidator;
        this.agreementCategoryRelationValidator = agreementCategoryRelationValidator;
        this.laborClassificationTimelineService = laborClassificationTimelineService;
    }

    @Override
    @Transactional
    public LaborClassification update(UpdateLaborClassificationCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        LocalDate normalizedStartDate = normalizeStartDate(command.startDate());

        EmployeeLaborClassificationContext employee = employeeLaborClassificationLookupPort
                .findByBusinessKeyForUpdate(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                )
                .orElseThrow(() -> new LaborClassificationEmployeeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                ));

        LaborClassification existing = laborClassificationRepository
                .findByEmployeeIdAndStartDate(employee.employeeId(), normalizedStartDate)
                .orElseThrow(() -> new LaborClassificationNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber,
                        normalizedStartDate
                ));

        LocalDate correctedStartDate = (command.newStartDate() != null)
                ? command.newStartDate()
                : normalizedStartDate;

        String normalizedAgreementCode = laborClassificationCatalogValidator
                .normalizeRequiredCode("agreementCode", command.agreementCode());
        String normalizedAgreementCategoryCode = laborClassificationCatalogValidator
                .normalizeRequiredCode("agreementCategoryCode", command.agreementCategoryCode());

        laborClassificationCatalogValidator.validateAgreementCode(
                normalizedRuleSystemCode,
                normalizedAgreementCode,
                correctedStartDate
        );
        laborClassificationCatalogValidator.validateAgreementCategoryCode(
                normalizedRuleSystemCode,
                normalizedAgreementCategoryCode,
                correctedStartDate
        );
        agreementCategoryRelationValidator.validateAgreementCategoryRelation(
                normalizedRuleSystemCode,
                normalizedAgreementCode,
                normalizedAgreementCategoryCode,
                correctedStartDate
        );

        LaborClassification corrected = new LaborClassification(
                employee.employeeId(),
                normalizedAgreementCode,
                normalizedAgreementCategoryCode,
                correctedStartDate,
                command.endDate()
        );

        LaborClassificationPlan plan = laborClassificationTimelineService.planCorrect(
                employee.employeeId(),
                existing,
                new DateRange(corrected.getStartDate(), corrected.getEndDate())
        );
        laborClassificationTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        laborClassificationRepository.update(corrected, normalizedStartDate);
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
