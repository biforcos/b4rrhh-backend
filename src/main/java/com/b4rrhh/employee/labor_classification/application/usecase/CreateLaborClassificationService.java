package com.b4rrhh.employee.labor_classification.application.usecase;

import com.b4rrhh.employee.labor_classification.application.command.CreateLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.model.LaborClassificationPlan;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationContext;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationLookupPort;
import com.b4rrhh.employee.labor_classification.application.service.AgreementCategoryRelationValidator;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationCatalogValidator;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationTimelineService;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationEmployeeNotFoundException;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.domain.port.LaborClassificationRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds a labor classification to the series. The add is planned against the
 * invariants of the series (ADR-057): inside the presence, no overlap, no
 * gap. The one automatic consequence is closing the occurrence in force on
 * the new start date the day before it. An add that starts on the start date
 * of an existing occurrence is not an add and is rejected as its correction
 * (backend#52).
 */
@Service
public class CreateLaborClassificationService implements CreateLaborClassificationUseCase {

    private final LaborClassificationRepository laborClassificationRepository;
    private final EmployeeLaborClassificationLookupPort employeeLaborClassificationLookupPort;
    private final LaborClassificationCatalogValidator laborClassificationCatalogValidator;
    private final AgreementCategoryRelationValidator agreementCategoryRelationValidator;
    private final LaborClassificationTimelineService laborClassificationTimelineService;

    public CreateLaborClassificationService(
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
    public LaborClassification create(CreateLaborClassificationCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());

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

        String normalizedAgreementCode = laborClassificationCatalogValidator
                .normalizeRequiredCode("agreementCode", command.agreementCode());
        String normalizedAgreementCategoryCode = laborClassificationCatalogValidator
                .normalizeRequiredCode("agreementCategoryCode", command.agreementCategoryCode());

        laborClassificationCatalogValidator.validateAgreementCode(
                normalizedRuleSystemCode,
                normalizedAgreementCode,
                command.startDate()
        );
        laborClassificationCatalogValidator.validateAgreementCategoryCode(
                normalizedRuleSystemCode,
                normalizedAgreementCategoryCode,
                command.startDate()
        );
        agreementCategoryRelationValidator.validateAgreementCategoryRelation(
                normalizedRuleSystemCode,
                normalizedAgreementCode,
                normalizedAgreementCategoryCode,
                command.startDate()
        );

        LaborClassification newLaborClassification = new LaborClassification(
                employee.employeeId(),
                normalizedAgreementCode,
                normalizedAgreementCategoryCode,
                command.startDate(),
                command.endDate()
        );

        LaborClassificationPlan plan = laborClassificationTimelineService.planAdd(
                employee.employeeId(),
                new DateRange(newLaborClassification.getStartDate(), newLaborClassification.getEndDate())
        );
        laborClassificationTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        if (plan.adjustsAnOccurrence()) {
            LaborClassification covering = laborClassificationRepository
                    .findByEmployeeIdAndStartDate(employee.employeeId(), plan.adjustedOccurrence().before().startDate())
                    .orElseThrow(() -> new IllegalStateException(
                            "Planned labor classification vanished: startDate="
                                    + plan.adjustedOccurrence().before().startDate()
                    ));
            LaborClassification closed = covering.adjustEndDate(plan.adjustedOccurrence().after().endDate());
            laborClassificationRepository.update(closed, closed.getStartDate());
        }

        laborClassificationRepository.save(newLaborClassification);
        return newLaborClassification;
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
