package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterEmployeeNotFoundException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionHistory;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
import com.b4rrhh.employee.cost_center.domain.service.CostCenterDistributionWindowGrouper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ListCostCenterDistributionHistoryService implements ListCostCenterDistributionHistoryUseCase {

    private final CostCenterRepository costCenterRepository;
    private final EmployeeCostCenterLookupPort employeeCostCenterLookupPort;
    private final CostCenterDistributionWindowGrouper windowGrouper;

    public ListCostCenterDistributionHistoryService(
            CostCenterRepository costCenterRepository,
            EmployeeCostCenterLookupPort employeeCostCenterLookupPort,
            CostCenterDistributionWindowGrouper windowGrouper
    ) {
        this.costCenterRepository = costCenterRepository;
        this.employeeCostCenterLookupPort = employeeCostCenterLookupPort;
        this.windowGrouper = windowGrouper;
    }

    @Override
    public CostCenterDistributionReadModel.History listHistory(ListCostCenterDistributionHistoryQuery query) {
        String ruleSystemCode = normalizeRuleSystemCode(query.ruleSystemCode());
        String employeeTypeCode = normalizeEmployeeTypeCode(query.employeeTypeCode());
        String employeeNumber = normalizeEmployeeNumber(query.employeeNumber());

        EmployeeCostCenterContext employee = employeeCostCenterLookupPort
                .findByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .orElseThrow(() -> new CostCenterEmployeeNotFoundException(
                        ruleSystemCode, employeeTypeCode, employeeNumber
                ));

        List<CostCenterAllocation> allAllocations = costCenterRepository.findByEmployeeIdOrderByStartDate(
                employee.employeeId()
        );

        CostCenterDistributionHistory history = windowGrouper.group(allAllocations);

        List<CostCenterDistributionReadModel.Window> windows = history.getWindows().stream()
                .map(this::toReadModelWindow)
                .toList();

        return new CostCenterDistributionReadModel.History(
                ruleSystemCode, employeeTypeCode, employeeNumber, windows
        );
    }

    private CostCenterDistributionReadModel.Window toReadModelWindow(CostCenterDistributionWindow window) {
        List<CostCenterDistributionReadModel.Item> items = window.getItems().stream()
                .map(line -> new CostCenterDistributionReadModel.Item(
                        line.getCostCenterCode(), line.getAllocationPercentage()
                ))
                .toList();

        BigDecimal total = window.getTotalAllocationPercentage();

        return new CostCenterDistributionReadModel.Window(
                window.getStartDate(), window.getEndDate(), total, items
        );
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
