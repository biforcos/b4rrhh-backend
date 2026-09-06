package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.model.CostCenterDistributionPlan;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.application.service.CostCenterTimelineService;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionNotFoundException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterEmployeeNotFoundException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
import com.b4rrhh.employee.cost_center.domain.service.CostCenterDistributionWindowGrouper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Removes a distribution window, every line of it (ADR-057, decision 3).
 * Removing the last one reopens the previous one: it is the "oops" and it is
 * safe. Removing one in the middle leaves a gap between its neighbours, and
 * here that is accepted: the series declares optional coverage (decision 1),
 * so a stretch of the presence without a distribution is a legal state and
 * nothing else moves. It is the only vertical where that removal goes
 * through (backend#54).
 */
@Service
public class DeleteCostCenterDistributionService implements DeleteCostCenterDistributionUseCase {

    private final CostCenterRepository costCenterRepository;
    private final EmployeeCostCenterLookupPort employeeCostCenterLookupPort;
    private final CostCenterTimelineService costCenterTimelineService;
    private final CostCenterDistributionWindowGrouper windowGrouper;

    public DeleteCostCenterDistributionService(
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
    public void delete(DeleteCostCenterDistributionCommand command) {
        String ruleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String employeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String employeeNumber = normalizeEmployeeNumber(command.employeeNumber());

        if (command.windowStartDate() == null) {
            throw new CostCenterDistributionInvalidException("windowStartDate is required");
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

        CostCenterDistributionPlan plan = costCenterTimelineService.planRemove(employee.employeeId(), existing);
        costCenterTimelineService.requireAccepted(plan, ruleSystemCode, employeeTypeCode, employeeNumber);

        if (plan.adjustsAnOccurrence()) {
            costCenterRepository.adjustWindowEndDate(
                    employee.employeeId(),
                    plan.adjustedOccurrence().before().startDate(),
                    plan.adjustedOccurrence().after().endDate()
            );
        }

        costCenterRepository.deleteAllForWindow(employee.employeeId(), existing.getStartDate());
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
