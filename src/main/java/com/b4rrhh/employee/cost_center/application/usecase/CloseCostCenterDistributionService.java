package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.model.CostCenterDistributionPlan;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.application.service.CostCenterTimelineService;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionNotFoundException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterEmployeeNotFoundException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
import com.b4rrhh.employee.cost_center.domain.service.CostCenterDistributionWindowGrouper;
import com.b4rrhh.employee.temporal.support.DateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Closes a distribution window on a date, every line of it. Through the
 * component it is a correction of the end date, judged like any other
 * (ADR-057): closing the window in force while the presence goes on leaves a
 * gap, which this series allows (optional coverage, decision 1); closing it
 * after the presence ends is outside the presence and is rejected. The
 * termination flow closes the presence first, so closing on the termination
 * date leaves no gap at all.
 *
 * @deprecated ADR-057 retires {@code close} as an operation of the API:
 *     adding the next window already closes the one in force, and any other
 *     end date is a correction (PUT). Kept for the screen until it migrates.
 */
@Deprecated
@Service
public class CloseCostCenterDistributionService implements CloseCostCenterDistributionUseCase {

    private final CostCenterRepository costCenterRepository;
    private final EmployeeCostCenterLookupPort employeeCostCenterLookupPort;
    private final CostCenterTimelineService costCenterTimelineService;
    private final CostCenterDistributionWindowGrouper windowGrouper;

    public CloseCostCenterDistributionService(
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
    @Transactional
    public CostCenterDistributionWindow close(CloseCostCenterDistributionCommand command) {
        String ruleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String employeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String employeeNumber = normalizeEmployeeNumber(command.employeeNumber());

        if (command.windowStartDate() == null) {
            throw new CostCenterDistributionInvalidException("windowStartDate is required");
        }
        if (command.endDate() == null) {
            throw new CostCenterDistributionInvalidException("endDate is required");
        }
        if (command.endDate().isBefore(command.windowStartDate())) {
            throw new CostCenterDistributionInvalidException(
                    "endDate must not be before windowStartDate"
            );
        }

        EmployeeCostCenterContext employee = employeeCostCenterLookupPort
                .findByBusinessKeyForUpdate(ruleSystemCode, employeeTypeCode, employeeNumber)
                .orElseThrow(() -> new CostCenterEmployeeNotFoundException(
                        ruleSystemCode, employeeTypeCode, employeeNumber
                ));

        CostCenterDistributionWindow existing = windowGrouper
                .group(costCenterRepository.findByEmployeeIdAndStartDate(employee.employeeId(), command.windowStartDate()))
                .findByStartDate(command.windowStartDate())
                .orElseThrow(() -> new CostCenterDistributionNotFoundException(
                        ruleSystemCode, employeeTypeCode, employeeNumber, command.windowStartDate()
                ));

        CostCenterDistributionPlan plan = costCenterTimelineService.planCorrect(
                employee.employeeId(),
                existing,
                new DateRange(existing.getStartDate(), command.endDate())
        );
        costCenterTimelineService.requireAccepted(plan, ruleSystemCode, employeeTypeCode, employeeNumber);

        costCenterRepository.adjustWindowEndDate(employee.employeeId(), existing.getStartDate(), command.endDate());

        List<CostCenterAllocation> closedItems = existing.getItems().stream()
                .map(item -> new CostCenterAllocation(
                        item.getEmployeeId(),
                        item.getCostCenterCode(),
                        item.getAllocationPercentage(),
                        item.getStartDate(),
                        command.endDate()
                ))
                .toList();

        return new CostCenterDistributionWindow(existing.getStartDate(), command.endDate(), closedItems);
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
