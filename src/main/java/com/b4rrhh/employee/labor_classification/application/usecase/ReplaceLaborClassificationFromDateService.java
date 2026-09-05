package com.b4rrhh.employee.labor_classification.application.usecase;

import com.b4rrhh.employee.labor_classification.application.command.ReplaceLaborClassificationFromDateCommand;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Adapter kept for the screen and the loader that still call
 * {@code replace-from-date}. It is an add with the end date the old
 * {@code ReplaceMode} used to derive: the tail of the occurrence in force on
 * the effective date, or open when none is. The component then does what
 * the old planner did: closes the covering occurrence the day before
 * ({@code SPLIT}), or inserts when nothing covers ({@code NO_COVERING}). What
 * changes is {@code EXACT_START}: starting on the very start date of an
 * existing occurrence no longer replaces it silently, it is rejected as its
 * correction (backend#52) and the correction is asked for as such (PUT).
 *
 * @deprecated ADR-057 retires {@code Replace…FromDate} as a model. Adding a
 *     labor classification already closes the one in force. To be removed
 *     once the screen has migrated to add-with-dates.
 */
@Deprecated
@Service
public class ReplaceLaborClassificationFromDateService implements ReplaceLaborClassificationFromDateUseCase {

    private final LaborClassificationRepository laborClassificationRepository;
    private final EmployeeLaborClassificationLookupPort employeeLaborClassificationLookupPort;
    private final LaborClassificationCatalogValidator laborClassificationCatalogValidator;
    private final AgreementCategoryRelationValidator agreementCategoryRelationValidator;
    private final LaborClassificationTimelineService laborClassificationTimelineService;

    public ReplaceLaborClassificationFromDateService(
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
    public LaborClassification replaceFromDate(ReplaceLaborClassificationFromDateCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        LocalDate normalizedEffectiveDate = normalizeEffectiveDate(command.effectiveDate());

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
                normalizedEffectiveDate
        );
        laborClassificationCatalogValidator.validateAgreementCategoryCode(
                normalizedRuleSystemCode,
                normalizedAgreementCategoryCode,
                normalizedEffectiveDate
        );
        agreementCategoryRelationValidator.validateAgreementCategoryRelation(
                normalizedRuleSystemCode,
                normalizedAgreementCode,
                normalizedAgreementCategoryCode,
                normalizedEffectiveDate
        );

        // The old planner gave the replacement the tail of the occurrence in force on
        // the effective date (SPLIT) or left it open when nothing covered it
        // (NO_COVERING). The end date is all that is derived here; the rest is the
        // component's judgement.
        LaborClassification replacement = new LaborClassification(
                employee.employeeId(),
                normalizedAgreementCode,
                normalizedAgreementCategoryCode,
                normalizedEffectiveDate,
                endDateOfOccurrenceInForceOn(employee.employeeId(), normalizedEffectiveDate)
        );

        LaborClassificationPlan plan = laborClassificationTimelineService.planAdd(
                employee.employeeId(),
                new DateRange(replacement.getStartDate(), replacement.getEndDate())
        );
        laborClassificationTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        if (plan.adjustsAnOccurrence()) {
            LocalDate coveringStartDate = plan.adjustedOccurrence().before().startDate();
            LaborClassification covering = laborClassificationRepository
                    .findByEmployeeIdAndStartDate(employee.employeeId(), coveringStartDate)
                    .orElseThrow(() -> new IllegalStateException(
                            "Planned labor classification vanished: startDate=" + coveringStartDate
                    ));
            LaborClassification closed = covering.adjustEndDate(plan.adjustedOccurrence().after().endDate());
            laborClassificationRepository.update(closed, closed.getStartDate());
        }

        laborClassificationRepository.save(replacement);
        return replacement;
    }

    private LocalDate endDateOfOccurrenceInForceOn(Long employeeId, LocalDate date) {
        List<LaborClassification> history = laborClassificationRepository.findByEmployeeIdOrderByStartDate(employeeId);
        for (LaborClassification occurrence : history) {
            boolean startsOnOrBefore = !occurrence.getStartDate().isAfter(date);
            boolean reaches = occurrence.getEndDate() == null || !occurrence.getEndDate().isBefore(date);
            if (startsOnOrBefore && reaches) {
                return occurrence.getEndDate();
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
