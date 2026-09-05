package com.b4rrhh.employee.labor_classification.application.usecase;

import com.b4rrhh.employee.labor_classification.application.command.PlanLaborClassificationChangeCommand;
import com.b4rrhh.employee.labor_classification.application.model.LaborClassificationPlan;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationContext;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationLookupPort;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationTimelineService;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationEmployeeNotFoundException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationNotFoundException;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.domain.port.LaborClassificationRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers what an add, a removal or a correction would do to the series
 * without applying it (ADR-057, decision 6). Rejected plans come back as
 * plans, not as errors: the screen shows the gap, the overlap or the
 * occurrence an add would correct, and the user decides.
 */
@Service
public class PlanLaborClassificationChangeService implements PlanLaborClassificationChangeUseCase {

    private final LaborClassificationRepository laborClassificationRepository;
    private final EmployeeLaborClassificationLookupPort employeeLaborClassificationLookupPort;
    private final LaborClassificationTimelineService laborClassificationTimelineService;

    public PlanLaborClassificationChangeService(
            LaborClassificationRepository laborClassificationRepository,
            EmployeeLaborClassificationLookupPort employeeLaborClassificationLookupPort,
            LaborClassificationTimelineService laborClassificationTimelineService
    ) {
        this.laborClassificationRepository = laborClassificationRepository;
        this.employeeLaborClassificationLookupPort = employeeLaborClassificationLookupPort;
        this.laborClassificationTimelineService = laborClassificationTimelineService;
    }

    @Override
    @Transactional(readOnly = true)
    public LaborClassificationPlan plan(PlanLaborClassificationChangeCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        if (command.operation() == null) {
            throw new IllegalArgumentException("operation is required");
        }

        EmployeeLaborClassificationContext employee = employeeLaborClassificationLookupPort
                .findByBusinessKey(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                )
                .orElseThrow(() -> new LaborClassificationEmployeeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                ));

        return switch (command.operation()) {
            case ADD -> laborClassificationTimelineService.planAdd(
                    employee.employeeId(),
                    requireDates(command)
            );
            case REMOVE -> laborClassificationTimelineService.planRemove(
                    employee.employeeId(),
                    requireOccurrence(command, employee)
            );
            case CORRECT -> laborClassificationTimelineService.planCorrect(
                    employee.employeeId(),
                    requireOccurrence(command, employee),
                    requireDates(command)
            );
        };
    }

    private LaborClassification requireOccurrence(
            PlanLaborClassificationChangeCommand command,
            EmployeeLaborClassificationContext employee
    ) {
        if (command.laborClassificationStartDate() == null) {
            throw new IllegalArgumentException("laborClassificationStartDate is required");
        }

        return laborClassificationRepository
                .findByEmployeeIdAndStartDate(employee.employeeId(), command.laborClassificationStartDate())
                .orElseThrow(() -> new LaborClassificationNotFoundException(
                        employee.ruleSystemCode(),
                        employee.employeeTypeCode(),
                        employee.employeeNumber(),
                        command.laborClassificationStartDate()
                ));
    }

    private static DateRange requireDates(PlanLaborClassificationChangeCommand command) {
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
