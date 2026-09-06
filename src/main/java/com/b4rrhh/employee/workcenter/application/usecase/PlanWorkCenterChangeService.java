package com.b4rrhh.employee.workcenter.application.usecase;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.workcenter.application.model.WorkCenterPlan;
import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterContext;
import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterLookupPort;
import com.b4rrhh.employee.workcenter.application.service.WorkCenterTimelineService;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterEmployeeNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterNotFoundException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers what an add, a removal or a correction would do to the series
 * without applying it (ADR-057, decision 6). Rejected plans come back as
 * plans, not as errors: the screen shows the gap, the overlap or the
 * assignment an add would correct, and the user decides.
 */
@Service
public class PlanWorkCenterChangeService implements PlanWorkCenterChangeUseCase {

    private final WorkCenterRepository workCenterRepository;
    private final EmployeeWorkCenterLookupPort employeeWorkCenterLookupPort;
    private final WorkCenterTimelineService workCenterTimelineService;

    public PlanWorkCenterChangeService(
            WorkCenterRepository workCenterRepository,
            EmployeeWorkCenterLookupPort employeeWorkCenterLookupPort,
            WorkCenterTimelineService workCenterTimelineService
    ) {
        this.workCenterRepository = workCenterRepository;
        this.employeeWorkCenterLookupPort = employeeWorkCenterLookupPort;
        this.workCenterTimelineService = workCenterTimelineService;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkCenterPlan plan(PlanWorkCenterChangeCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        if (command.operation() == null) {
            throw new IllegalArgumentException("operation is required");
        }

        EmployeeWorkCenterContext employee = employeeWorkCenterLookupPort
                .findByBusinessKey(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                )
                .orElseThrow(() -> new WorkCenterEmployeeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                ));

        return switch (command.operation()) {
            case ADD -> workCenterTimelineService.planAdd(
                    employee.employeeId(),
                    requireDates(command)
            );
            case REMOVE -> workCenterTimelineService.planRemove(
                    employee.employeeId(),
                    requireOccurrence(command, employee)
            );
            case CORRECT -> workCenterTimelineService.planCorrect(
                    employee.employeeId(),
                    requireOccurrence(command, employee),
                    requireDates(command)
            );
        };
    }

    private WorkCenter requireOccurrence(PlanWorkCenterChangeCommand command, EmployeeWorkCenterContext employee) {
        if (command.workCenterAssignmentNumber() == null || command.workCenterAssignmentNumber() <= 0) {
            throw new IllegalArgumentException("workCenterAssignmentNumber must be a positive integer");
        }

        return workCenterRepository
                .findByEmployeeIdAndWorkCenterAssignmentNumber(employee.employeeId(), command.workCenterAssignmentNumber())
                .orElseThrow(() -> new WorkCenterNotFoundException(
                        employee.ruleSystemCode(),
                        employee.employeeTypeCode(),
                        employee.employeeNumber(),
                        command.workCenterAssignmentNumber()
                ));
    }

    private static DateRange requireDates(PlanWorkCenterChangeCommand command) {
        if (command.startDate() == null) {
            throw new IllegalArgumentException("startDate is required");
        }

        return new DateRange(command.startDate(), command.endDate());
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
