package com.b4rrhh.employee.workcenter.application.usecase;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.workcenter.application.model.WorkCenterPlan;
import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterContext;
import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterLookupPort;
import com.b4rrhh.employee.workcenter.application.service.WorkCenterCatalogValidator;
import com.b4rrhh.employee.workcenter.application.service.WorkCenterTimelineService;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterEmployeeNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterRuleSystemNotFoundException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterRepository;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corrects a work center assignment: its code, its dates, or both. Nothing
 * else moves (ADR-057, decision 3): if the corrected dates leave a gap or an
 * overlap, the plan rejects them and names what the user would have to
 * stretch instead.
 */
@Service("employeeUpdateWorkCenterService")
public class UpdateWorkCenterService implements UpdateWorkCenterUseCase {

    private final WorkCenterRepository workCenterRepository;
    private final EmployeeWorkCenterLookupPort employeeWorkCenterLookupPort;
    private final RuleSystemRepository ruleSystemRepository;
    private final WorkCenterCatalogValidator workCenterCatalogValidator;
    private final WorkCenterTimelineService workCenterTimelineService;

    public UpdateWorkCenterService(
            WorkCenterRepository workCenterRepository,
            EmployeeWorkCenterLookupPort employeeWorkCenterLookupPort,
            RuleSystemRepository ruleSystemRepository,
            WorkCenterCatalogValidator workCenterCatalogValidator,
            WorkCenterTimelineService workCenterTimelineService
    ) {
        this.workCenterRepository = workCenterRepository;
        this.employeeWorkCenterLookupPort = employeeWorkCenterLookupPort;
        this.ruleSystemRepository = ruleSystemRepository;
        this.workCenterCatalogValidator = workCenterCatalogValidator;
        this.workCenterTimelineService = workCenterTimelineService;
    }

    @Override
    @Transactional
    public WorkCenter update(UpdateWorkCenterCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        Integer normalizedAssignmentNumber = normalizeAssignmentNumber(command.workCenterAssignmentNumber());

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

        WorkCenter existing = workCenterRepository
                .findByEmployeeIdAndWorkCenterAssignmentNumber(employee.employeeId(), normalizedAssignmentNumber)
                .orElseThrow(() -> new WorkCenterNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber,
                        normalizedAssignmentNumber
                ));

        String workCenterCode = workCenterCatalogValidator.normalizeRequiredCode("workCenterCode", command.workCenterCode());
        workCenterCatalogValidator.validateWorkCenterCode(normalizedRuleSystemCode, workCenterCode, command.startDate());

        WorkCenter corrected = new WorkCenter(
                existing.getId(),
                existing.getEmployeeId(),
                existing.getWorkCenterAssignmentNumber(),
                workCenterCode,
                command.startDate(),
                command.endDate(),
                existing.getCreatedAt(),
                existing.getUpdatedAt()
        );

        WorkCenterPlan plan = workCenterTimelineService.planCorrect(
                employee.employeeId(),
                existing,
                new DateRange(corrected.getStartDate(), corrected.getEndDate())
        );
        workCenterTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        return workCenterRepository.save(corrected);
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

    private Integer normalizeAssignmentNumber(Integer workCenterAssignmentNumber) {
        if (workCenterAssignmentNumber == null || workCenterAssignmentNumber <= 0) {
            throw new IllegalArgumentException("workCenterAssignmentNumber must be a positive integer");
        }

        return workCenterAssignmentNumber;
    }
}
