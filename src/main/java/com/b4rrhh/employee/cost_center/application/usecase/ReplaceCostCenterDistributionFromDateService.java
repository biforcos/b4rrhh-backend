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
import com.b4rrhh.employee.cost_center.domain.service.CostCenterDistributionWindowGrouper;
import com.b4rrhh.employee.temporal.support.DateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter kept for the screen and the workforce loader, which still call
 * {@code replace-from-date}. It is an add whose end date is the tail of the
 * window in force on the effective date, or open when none is. The component
 * then does what this use case did by hand: closes the covering window the
 * day before, every line of it. What changes is what it used to refuse and
 * what it used to do silently: nothing in force before the effective date is
 * no longer a "not found" but whatever the invariants say of the resulting
 * series, and starting on the very start date of an existing window is
 * rejected as its correction (backend#52) and asked for as such (PUT).
 *
 * @deprecated ADR-057 retires {@code Replace…FromDate} as a model. Adding a
 *     window already closes the one in force. To be removed once the screen
 *     and the loader have migrated to add-with-dates.
 */
@Deprecated
@Service
public class ReplaceCostCenterDistributionFromDateService implements ReplaceCostCenterDistributionFromDateUseCase {

    private final CostCenterRepository costCenterRepository;
    private final EmployeeCostCenterLookupPort employeeCostCenterLookupPort;
    private final CostCenterCatalogValidator costCenterCatalogValidator;
    private final CostCenterTimelineService costCenterTimelineService;
    private final CostCenterDistributionTimelineValidator timelineValidator;
    private final CostCenterDistributionWindowGrouper windowGrouper;

    public ReplaceCostCenterDistributionFromDateService(
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
    public CostCenterDistributionWindow replaceFromDate(ReplaceCostCenterDistributionFromDateCommand command) {
        String ruleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String employeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String employeeNumber = normalizeEmployeeNumber(command.employeeNumber());

        if (command.effectiveDate() == null) {
            throw new CostCenterDistributionInvalidException("effectiveDate is required");
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new CostCenterDistributionInvalidException("at least one distribution item is required");
        }

        EmployeeCostCenterContext employee = employeeCostCenterLookupPort
                .findByBusinessKeyForUpdate(ruleSystemCode, employeeTypeCode, employeeNumber)
                .orElseThrow(() -> new CostCenterEmployeeNotFoundException(
                        ruleSystemCode, employeeTypeCode, employeeNumber
                ));

        // The replacement takes the tail of the window in force on the effective date, or stays
        // open when nothing covers it. The end date is all that is derived here; the rest is the
        // component's judgement.
        LocalDate endDate = windowGrouper
                .group(costCenterRepository.findByEmployeeIdOrderByStartDate(employee.employeeId()))
                .findActiveAt(command.effectiveDate())
                .map(CostCenterDistributionWindow::getEndDate)
                .orElse(null);

        List<CostCenterAllocation> newAllocations = new ArrayList<>();
        for (CostCenterDistributionItem item : command.items()) {
            String costCenterCode = costCenterCatalogValidator.normalizeRequiredCode("costCenterCode", item.costCenterCode());
            costCenterCatalogValidator.validateCostCenterCode(ruleSystemCode, costCenterCode, command.effectiveDate());
            newAllocations.add(new CostCenterAllocation(
                    employee.employeeId(),
                    costCenterCode,
                    item.allocationPercentage(),
                    command.effectiveDate(),
                    endDate
            ));
        }

        // Validate sum <= 100 for new window
        timelineValidator.validateWindow(newAllocations);

        CostCenterDistributionPlan plan = costCenterTimelineService.planAdd(
                employee.employeeId(),
                new DateRange(command.effectiveDate(), endDate)
        );
        costCenterTimelineService.requireAccepted(plan, ruleSystemCode, employeeTypeCode, employeeNumber);

        if (plan.adjustsAnOccurrence()) {
            costCenterRepository.adjustWindowEndDate(
                    employee.employeeId(),
                    plan.adjustedOccurrence().before().startDate(),
                    plan.adjustedOccurrence().after().endDate()
            );
        }

        costCenterRepository.saveAll(newAllocations);

        return new CostCenterDistributionWindow(command.effectiveDate(), endDate, newAllocations);
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
