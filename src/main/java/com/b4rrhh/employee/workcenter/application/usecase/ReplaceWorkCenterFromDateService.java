package com.b4rrhh.employee.workcenter.application.usecase;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.workcenter.application.model.WorkCenterPlan;
import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterContext;
import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterLookupPort;
import com.b4rrhh.employee.workcenter.application.service.WorkCenterCatalogValidator;
import com.b4rrhh.employee.workcenter.application.service.WorkCenterTimelineService;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterEmployeeNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterRuleSystemNotFoundException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterRepository;
import com.b4rrhh.employee.workcenter.domain.service.WorkCenterEmployeeCompanyDomainService;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Adapter kept for the workforce loader, which still calls
 * {@code replace-from-date} (the screen never did: it calls the add). It is
 * an add with the end date the old planner used to derive: the tail of the
 * assignment in force on the effective date, or open when none is. The
 * component then does what the old planner did: closes the covering
 * assignment the day before ({@code SPLIT}), or inserts when nothing covers
 * ({@code NO_COVERING}). What changes is {@code EXACT_START}: starting on the
 * very start date of an existing assignment no longer replaces it silently,
 * it is rejected as its correction (backend#52) and the correction is asked
 * for as such (PUT).
 *
 * @deprecated ADR-057 retires {@code Replace…FromDate} as a model. Adding a
 *     work center assignment already closes the one in force. To be removed
 *     once the loader has migrated to add-with-dates.
 */
@Deprecated
@Service
public class ReplaceWorkCenterFromDateService implements ReplaceWorkCenterFromDateUseCase {

    private final WorkCenterRepository workCenterRepository;
    private final EmployeeWorkCenterLookupPort employeeWorkCenterLookupPort;
    private final RuleSystemRepository ruleSystemRepository;
    private final WorkCenterCatalogValidator workCenterCatalogValidator;
    private final WorkCenterTimelineService workCenterTimelineService;
    private final WorkCenterEmployeeCompanyDomainService workCenterEmployeeCompanyDomainService;

    public ReplaceWorkCenterFromDateService(
            WorkCenterRepository workCenterRepository,
            EmployeeWorkCenterLookupPort employeeWorkCenterLookupPort,
            RuleSystemRepository ruleSystemRepository,
            WorkCenterCatalogValidator workCenterCatalogValidator,
            WorkCenterTimelineService workCenterTimelineService,
            WorkCenterEmployeeCompanyDomainService workCenterEmployeeCompanyDomainService
    ) {
        this.workCenterRepository = workCenterRepository;
        this.employeeWorkCenterLookupPort = employeeWorkCenterLookupPort;
        this.ruleSystemRepository = ruleSystemRepository;
        this.workCenterCatalogValidator = workCenterCatalogValidator;
        this.workCenterTimelineService = workCenterTimelineService;
        this.workCenterEmployeeCompanyDomainService = workCenterEmployeeCompanyDomainService;
    }

    @Override
    @Transactional
    public WorkCenter replaceFromDate(ReplaceWorkCenterFromDateCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        LocalDate normalizedEffectiveDate = normalizeEffectiveDate(command.effectiveDate());

        ruleSystemRepository.findByCode(normalizedRuleSystemCode)
                .orElseThrow(() -> new WorkCenterRuleSystemNotFoundException(normalizedRuleSystemCode));

        EmployeeWorkCenterContext employee = employeeWorkCenterLookupPort
                .findByBusinessKeyForUpdate(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                )
                .orElseThrow(() -> new WorkCenterEmployeeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                ));

        String normalizedWorkCenterCode = workCenterCatalogValidator
                .normalizeRequiredCode("workCenterCode", command.workCenterCode());
        workCenterCatalogValidator.validateWorkCenterCode(
                normalizedRuleSystemCode,
                normalizedWorkCenterCode,
                normalizedEffectiveDate
        );

        workCenterEmployeeCompanyDomainService.validateWorkCenterBelongsToEmployeeCompany(
                employee.employeeId(),
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber,
                normalizedWorkCenterCode,
                normalizedEffectiveDate
        );

        // The old planner gave the replacement the tail of the assignment in force on the
        // effective date (SPLIT) or left it open when nothing covered it (NO_COVERING). The
        // end date is all that is derived here; the rest is the component's judgement.
        WorkCenter replacement = new WorkCenter(
                null,
                employee.employeeId(),
                nextAssignmentNumber(employee.employeeId()),
                normalizedWorkCenterCode,
                normalizedEffectiveDate,
                endDateOfAssignmentInForceOn(employee.employeeId(), normalizedEffectiveDate),
                null,
                null
        );

        WorkCenterPlan plan = workCenterTimelineService.planAdd(
                employee.employeeId(),
                new DateRange(replacement.getStartDate(), replacement.getEndDate())
        );
        workCenterTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        if (plan.adjustsAnOccurrence()) {
            Integer coveringNumber = plan.adjustedOccurrence().workCenterAssignmentNumber();
            WorkCenter covering = workCenterRepository
                    .findByEmployeeIdAndWorkCenterAssignmentNumber(employee.employeeId(), coveringNumber)
                    .orElseThrow(() -> new IllegalStateException(
                            "Planned work center assignment vanished: workCenterAssignmentNumber=" + coveringNumber
                    ));
            workCenterRepository.save(covering.adjustEndDate(plan.adjustedOccurrence().after().endDate()));
        }

        return workCenterRepository.save(replacement);
    }

    private int nextAssignmentNumber(Long employeeId) {
        return workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(employeeId)
                .map(value -> value + 1)
                .orElse(1);
    }

    private LocalDate endDateOfAssignmentInForceOn(Long employeeId, LocalDate date) {
        List<WorkCenter> history = workCenterRepository.findByEmployeeIdOrderByStartDate(employeeId);
        for (WorkCenter assignment : history) {
            boolean startsOnOrBefore = !assignment.getStartDate().isAfter(date);
            boolean reaches = assignment.getEndDate() == null || !assignment.getEndDate().isBefore(date);
            if (startsOnOrBefore && reaches) {
                return assignment.getEndDate();
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
