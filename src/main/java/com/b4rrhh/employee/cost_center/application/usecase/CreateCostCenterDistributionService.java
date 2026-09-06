package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.model.CostCenterDistributionPlan;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.application.service.CostCenterCatalogValidator;
import com.b4rrhh.employee.cost_center.application.service.CostCenterTimelineService;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterEmployeeNotFoundException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
import com.b4rrhh.employee.cost_center.domain.service.CostCenterDistributionTimelineValidator;
import com.b4rrhh.employee.temporal.support.DateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds a distribution window to the series. The add is planned against the
 * invariants of the series (ADR-057): inside the presence and no overlap. A
 * gap it leaves is legal here, because the series declares optional coverage
 * (decision 1). The one automatic consequence is closing the window in force on the
 * new start date the day before it, every line of it at once. An add that
 * starts on the start date of an existing window is not an add and is
 * rejected as its correction (backend#52). What the lines add up to is the
 * vertical's own rule and is checked before asking the plan.
 */
@Service
public class CreateCostCenterDistributionService implements CreateCostCenterDistributionUseCase {

    private final CostCenterRepository costCenterRepository;
    private final EmployeeCostCenterLookupPort employeeCostCenterLookupPort;
    private final CostCenterCatalogValidator costCenterCatalogValidator;
    private final CostCenterTimelineService costCenterTimelineService;
    private final CostCenterDistributionTimelineValidator timelineValidator;

    public CreateCostCenterDistributionService(
            CostCenterRepository costCenterRepository,
            EmployeeCostCenterLookupPort employeeCostCenterLookupPort,
            CostCenterCatalogValidator costCenterCatalogValidator,
            CostCenterTimelineService costCenterTimelineService,
            CostCenterDistributionTimelineValidator timelineValidator
    ) {
        this.costCenterRepository = costCenterRepository;
        this.employeeCostCenterLookupPort = employeeCostCenterLookupPort;
        this.costCenterCatalogValidator = costCenterCatalogValidator;
        this.costCenterTimelineService = costCenterTimelineService;
        this.timelineValidator = timelineValidator;
    }

    @Override
    @Transactional
    public CostCenterDistributionWindow create(CreateCostCenterDistributionCommand command) {
        String ruleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String employeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String employeeNumber = normalizeEmployeeNumber(command.employeeNumber());

        if (command.startDate() == null) {
            throw new CostCenterDistributionInvalidException("startDate is required");
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new CostCenterDistributionInvalidException("at least one distribution item is required");
        }

        EmployeeCostCenterContext employee = employeeCostCenterLookupPort
                .findByBusinessKeyForUpdate(ruleSystemCode, employeeTypeCode, employeeNumber)
                .orElseThrow(() -> new CostCenterEmployeeNotFoundException(
                        ruleSystemCode, employeeTypeCode, employeeNumber
                ));

        // Build allocations (catalog validation + row-level validation happens in constructor)
        List<CostCenterAllocation> allocations = new ArrayList<>();
        for (CostCenterDistributionItem item : command.items()) {
            String costCenterCode = costCenterCatalogValidator.normalizeRequiredCode("costCenterCode", item.costCenterCode());
            costCenterCatalogValidator.validateCostCenterCode(ruleSystemCode, costCenterCode, command.startDate());
            allocations.add(new CostCenterAllocation(
                    employee.employeeId(),
                    costCenterCode,
                    item.allocationPercentage(),
                    command.startDate(),
                    command.endDate()
            ));
        }

        // Validate sum <= 100
        timelineValidator.validateWindow(allocations);

        CostCenterDistributionPlan plan = costCenterTimelineService.planAdd(
                employee.employeeId(),
                new DateRange(command.startDate(), command.endDate())
        );
        costCenterTimelineService.requireAccepted(plan, ruleSystemCode, employeeTypeCode, employeeNumber);

        if (plan.adjustsAnOccurrence()) {
            costCenterRepository.adjustWindowEndDate(
                    employee.employeeId(),
                    plan.adjustedOccurrence().before().startDate(),
                    plan.adjustedOccurrence().after().endDate()
            );
        }

        costCenterRepository.saveAll(allocations);

        return new CostCenterDistributionWindow(command.startDate(), command.endDate(), allocations);
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
