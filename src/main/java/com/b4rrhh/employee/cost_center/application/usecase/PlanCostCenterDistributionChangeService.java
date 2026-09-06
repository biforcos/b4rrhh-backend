package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.model.CostCenterDistributionPlan;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.application.service.CostCenterTimelineService;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionNotFoundException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterEmployeeNotFoundException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
import com.b4rrhh.employee.cost_center.domain.service.CostCenterDistributionWindowGrouper;
import com.b4rrhh.employee.temporal.support.DateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers what an add, a removal or a correction would do to the series
 * without applying it (ADR-057, decision 6). Rejected plans come back as
 * plans, not as errors: the screen shows the gap, the overlap or the window
 * an add would correct, and the user decides.
 */
@Service
public class PlanCostCenterDistributionChangeService implements PlanCostCenterDistributionChangeUseCase {

    private final CostCenterRepository costCenterRepository;
    private final EmployeeCostCenterLookupPort employeeCostCenterLookupPort;
    private final CostCenterTimelineService costCenterTimelineService;
    private final CostCenterDistributionWindowGrouper windowGrouper;

    public PlanCostCenterDistributionChangeService(
            CostCenterRepository costCenterRepository,
            EmployeeCostCenterLookupPort employeeCostCenterLookupPort,
            CostCenterTimelineService costCenterTimelineService,
            CostCenterDistributionWindowGrouper windowGrouper
    ) {
        this.costCenterRepository = costCenterRepository;
        this.employeeCostCenterLookupPort = employeeCostCenterLookupPort;
        this.costCenterTimelineService = costCenterTimelineService;
        this.windowGrouper = windowGrouper;
    }

    @Override
    @Transactional(readOnly = true)
    public CostCenterDistributionPlan plan(PlanCostCenterDistributionChangeCommand command) {
        String ruleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String employeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String employeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        if (command.operation() == null) {
            throw new IllegalArgumentException("operation is required");
        }

        EmployeeCostCenterContext employee = employeeCostCenterLookupPort
                .findByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .orElseThrow(() -> new CostCenterEmployeeNotFoundException(
                        ruleSystemCode, employeeTypeCode, employeeNumber
                ));

        return switch (command.operation()) {
            case ADD -> costCenterTimelineService.planAdd(
                    employee.employeeId(),
                    requireDates(command)
            );
            case REMOVE -> costCenterTimelineService.planRemove(
                    employee.employeeId(),
                    requireWindow(command, employee)
            );
            case CORRECT -> costCenterTimelineService.planCorrect(
                    employee.employeeId(),
                    requireWindow(command, employee),
                    requireDates(command)
            );
        };
    }

    private CostCenterDistributionWindow requireWindow(
            PlanCostCenterDistributionChangeCommand command,
            EmployeeCostCenterContext employee
    ) {
        if (command.windowStartDate() == null) {
            throw new IllegalArgumentException("windowStartDate is required");
        }

        return windowGrouper
                .group(costCenterRepository.findByEmployeeIdAndStartDate(employee.employeeId(), command.windowStartDate()))
                .findByStartDate(command.windowStartDate())
                .orElseThrow(() -> new CostCenterDistributionNotFoundException(
                        employee.ruleSystemCode(),
                        employee.employeeTypeCode(),
                        employee.employeeNumber(),
                        command.windowStartDate()
                ));
    }

    private static DateRange requireDates(PlanCostCenterDistributionChangeCommand command) {
        if (command.startDate() == null) {
            throw new IllegalArgumentException("startDate is required");
        }

        return new DateRange(command.startDate(), command.endDate());
    }

    private String normalizeRuleSystemCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ruleSystemCode is required");
        }
        return value.trim().toUpperCase();
    }

    private String normalizeEmployeeTypeCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("employeeTypeCode is required");
        }
        return value.trim().toUpperCase();
    }

    private String normalizeEmployeeNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("employeeNumber is required");
        }
        return value.trim();
    }
}
