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

/**
 * Adds a work center assignment to the series. The add is planned against
 * the invariants of the series (ADR-057): inside the presence, no overlap,
 * no gap. The one automatic consequence is closing the assignment in force
 * on the new start date the day before it. An add that starts on the start
 * date of an existing assignment is not an add and is rejected as its
 * correction (backend#52).
 */
@Service("employeeCreateWorkCenterService")
public class CreateWorkCenterService implements CreateWorkCenterUseCase {

    private final WorkCenterRepository workCenterRepository;
    private final EmployeeWorkCenterLookupPort employeeWorkCenterLookupPort;
    private final RuleSystemRepository ruleSystemRepository;
    private final WorkCenterCatalogValidator workCenterCatalogValidator;
    private final WorkCenterTimelineService workCenterTimelineService;
    private final WorkCenterEmployeeCompanyDomainService workCenterEmployeeCompanyDomainService;

    public CreateWorkCenterService(
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
    public WorkCenter create(CreateWorkCenterCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());

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

        String workCenterCode = workCenterCatalogValidator.normalizeRequiredCode("workCenterCode", command.workCenterCode());
        workCenterCatalogValidator.validateWorkCenterCode(normalizedRuleSystemCode, workCenterCode, command.startDate());

        int nextAssignmentNumber = workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(employee.employeeId())
                .map(value -> value + 1)
                .orElse(1);

        WorkCenter newWorkCenter = new WorkCenter(
                null,
                employee.employeeId(),
                nextAssignmentNumber,
                workCenterCode,
                command.startDate(),
                command.endDate(),
                null,
                null
        );

        workCenterEmployeeCompanyDomainService.validateWorkCenterBelongsToEmployeeCompany(
                employee.employeeId(),
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber,
                workCenterCode,
                newWorkCenter.getStartDate()
        );

        WorkCenterPlan plan = workCenterTimelineService.planAdd(
                employee.employeeId(),
                new DateRange(newWorkCenter.getStartDate(), newWorkCenter.getEndDate())
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

        return workCenterRepository.save(newWorkCenter);
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