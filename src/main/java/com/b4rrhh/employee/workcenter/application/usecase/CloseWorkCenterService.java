package com.b4rrhh.employee.workcenter.application.usecase;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.workcenter.application.model.WorkCenterPlan;
import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterContext;
import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterLookupPort;
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
 * Closes an open work center assignment on a date. Through the component it
 * is a correction of the end date, judged like any other (ADR-057): closing
 * the assignment in force while the presence goes on leaves a gap and is
 * rejected naming it. The termination flow closes the presence first, so
 * closing on the termination date leaves none.
 *
 * @deprecated ADR-057 retires {@code close} as an operation of the API:
 *     adding the next assignment already closes the one in force, and any
 *     other end date is a correction (PUT). Kept for the termination
 *     participant and the screen until they migrate.
 */
@Deprecated
@Service
public class CloseWorkCenterService implements CloseWorkCenterUseCase {

    private final WorkCenterRepository workCenterRepository;
    private final EmployeeWorkCenterLookupPort employeeWorkCenterLookupPort;
    private final RuleSystemRepository ruleSystemRepository;
    private final WorkCenterTimelineService workCenterTimelineService;

    public CloseWorkCenterService(
            WorkCenterRepository workCenterRepository,
            EmployeeWorkCenterLookupPort employeeWorkCenterLookupPort,
            RuleSystemRepository ruleSystemRepository,
            WorkCenterTimelineService workCenterTimelineService
    ) {
        this.workCenterRepository = workCenterRepository;
        this.employeeWorkCenterLookupPort = employeeWorkCenterLookupPort;
        this.ruleSystemRepository = ruleSystemRepository;
        this.workCenterTimelineService = workCenterTimelineService;
    }

    @Override
    @Transactional
    public WorkCenter close(CloseWorkCenterCommand command) {
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

        WorkCenter closed = existing.close(command.endDate());

        WorkCenterPlan plan = workCenterTimelineService.planCorrect(
                employee.employeeId(),
                existing,
                new DateRange(closed.getStartDate(), closed.getEndDate())
        );
        workCenterTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        return workCenterRepository.save(closed);
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
