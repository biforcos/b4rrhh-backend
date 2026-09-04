package com.b4rrhh.employee.working_time.application.usecase;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.working_time.application.model.WorkingTimePlan;
import com.b4rrhh.employee.working_time.application.port.EmployeeWorkingTimeContext;
import com.b4rrhh.employee.working_time.application.port.EmployeeWorkingTimeLookupPort;
import com.b4rrhh.employee.working_time.application.service.WorkingTimeTimelineService;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeEmployeeNotFoundException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNotFoundException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.port.WorkingTimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers what an add, a removal or a correction would do to the series
 * without applying it (ADR-057, decision 6). Rejected plans come back as
 * plans, not as errors: the screen shows the gap or the overlap and the
 * user decides.
 */
@Service
public class PlanWorkingTimeChangeService implements PlanWorkingTimeChangeUseCase {

    private final WorkingTimeRepository workingTimeRepository;
    private final EmployeeWorkingTimeLookupPort employeeWorkingTimeLookupPort;
    private final WorkingTimeTimelineService workingTimeTimelineService;

    public PlanWorkingTimeChangeService(
            WorkingTimeRepository workingTimeRepository,
            EmployeeWorkingTimeLookupPort employeeWorkingTimeLookupPort,
            WorkingTimeTimelineService workingTimeTimelineService
    ) {
        this.workingTimeRepository = workingTimeRepository;
        this.employeeWorkingTimeLookupPort = employeeWorkingTimeLookupPort;
        this.workingTimeTimelineService = workingTimeTimelineService;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkingTimePlan plan(PlanWorkingTimeChangeCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        if (command.operation() == null) {
            throw new IllegalArgumentException("operation is required");
        }

        EmployeeWorkingTimeContext employee = employeeWorkingTimeLookupPort
                .findByBusinessKey(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                )
                .orElseThrow(() -> new WorkingTimeEmployeeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                ));

        return switch (command.operation()) {
            case ADD -> workingTimeTimelineService.planAdd(
                    employee.employeeId(),
                    requireDates(command)
            );
            case REMOVE -> workingTimeTimelineService.planRemove(
                    employee.employeeId(),
                    requireOccurrence(command, employee)
            );
            case CORRECT -> workingTimeTimelineService.planCorrect(
                    employee.employeeId(),
                    requireOccurrence(command, employee),
                    requireDates(command)
            );
        };
    }

    private WorkingTime requireOccurrence(PlanWorkingTimeChangeCommand command, EmployeeWorkingTimeContext employee) {
        if (command.workingTimeNumber() == null || command.workingTimeNumber() <= 0) {
            throw new IllegalArgumentException("workingTimeNumber must be a positive integer");
        }

        return workingTimeRepository
                .findByEmployeeIdAndWorkingTimeNumber(employee.employeeId(), command.workingTimeNumber())
                .orElseThrow(() -> new WorkingTimeNotFoundException(
                        employee.ruleSystemCode(),
                        employee.employeeTypeCode(),
                        employee.employeeNumber(),
                        command.workingTimeNumber()
                ));
    }

    private static DateRange requireDates(PlanWorkingTimeChangeCommand command) {
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
