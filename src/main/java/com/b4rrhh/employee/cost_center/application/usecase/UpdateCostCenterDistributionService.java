package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.model.CostCenterDistributionPlan;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.application.service.CostCenterCatalogValidator;
import com.b4rrhh.employee.cost_center.application.service.CostCenterTimelineService;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionNotFoundException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterEmployeeNotFoundException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
import com.b4rrhh.employee.cost_center.domain.service.CostCenterDistributionTimelineValidator;
import com.b4rrhh.employee.cost_center.domain.service.CostCenterDistributionWindowGrouper;
import com.b4rrhh.employee.temporal.support.DateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Corrects a distribution window: its lines, its dates, or both. Correcting a
 * line is correcting the set (ADR-057, decision 0): the window is replaced
 * whole, under the same start date unless the correction moves it. Nothing
 * else moves (decision 3): if the corrected dates leave a gap or an overlap,
 * the plan rejects them and names what the user would have to stretch
 * instead.
 */
@Service
public class UpdateCostCenterDistributionService implements UpdateCostCenterDistributionUseCase {

    private final CostCenterRepository costCenterRepository;
    private final EmployeeCostCenterLookupPort employeeCostCenterLookupPort;
    private final CostCenterCatalogValidator costCenterCatalogValidator;
    private final CostCenterTimelineService costCenterTimelineService;
    private final CostCenterDistributionTimelineValidator timelineValidator;
    private final CostCenterDistributionWindowGrouper windowGrouper;

    public UpdateCostCenterDistributionService(
            CostCenterRepository costCenterRepository,
            EmployeeCostCenterLookupPort employeeCostCenterLookupPort,
            CostCenterCatalogValidator costCenterCatalogValidator,
            CostCenterTimelineService costCenterTimelineService,
            CostCenterDistributionTimelineValidator timelineValidator,
            CostCenterDistributionWindowGrouper windowGrouper
    ) {
        this.costCenterRepository = costCenterRepository;
        this.employeeCostCenterLookupPort = employeeCostCenterLookupPort;
        this.costCenterCatalogValidator = costCenterCatalogValidator;
        this.costCenterTimelineService = costCenterTimelineService;
        this.timelineValidator = timelineValidator;
        this.windowGrouper = windowGrouper;
    }

    @Override
    @Transactional
    public CostCenterDistributionWindow update(UpdateCostCenterDistributionCommand command) {
        String ruleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String employeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String employeeNumber = normalizeEmployeeNumber(command.employeeNumber());

        if (command.windowStartDate() == null) {
            throw new CostCenterDistributionInvalidException("windowStartDate is required");
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new CostCenterDistributionInvalidException("at least one distribution item is required");
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

        boolean keepsItsDates = command.startDate() == null;
        LocalDate startDate = keepsItsDates ? existing.getStartDate() : command.startDate();
        LocalDate endDate = keepsItsDates ? existing.getEndDate() : command.endDate();

        List<CostCenterAllocation> corrected = new ArrayList<>();
        for (CostCenterDistributionItem item : command.items()) {
            String costCenterCode = costCenterCatalogValidator.normalizeRequiredCode("costCenterCode", item.costCenterCode());
            costCenterCatalogValidator.validateCostCenterCode(ruleSystemCode, costCenterCode, startDate);
            corrected.add(new CostCenterAllocation(
                    employee.employeeId(),
                    costCenterCode,
                    item.allocationPercentage(),
                    startDate,
                    endDate
            ));
        }

        timelineValidator.validateWindow(corrected);

        CostCenterDistributionPlan plan = costCenterTimelineService.planCorrect(
                employee.employeeId(),
                existing,
                new DateRange(startDate, endDate)
        );
        costCenterTimelineService.requireAccepted(plan, ruleSystemCode, employeeTypeCode, employeeNumber);

        costCenterRepository.deleteAllForWindow(employee.employeeId(), existing.getStartDate());
        costCenterRepository.saveAll(corrected);

        return new CostCenterDistributionWindow(startDate, endDate, corrected);
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
