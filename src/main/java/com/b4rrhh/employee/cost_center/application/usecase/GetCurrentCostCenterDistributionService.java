package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterEmployeeNotFoundException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class GetCurrentCostCenterDistributionService implements GetCurrentCostCenterDistributionUseCase {

    private final CostCenterRepository costCenterRepository;
    private final EmployeeCostCenterLookupPort employeeCostCenterLookupPort;

    public GetCurrentCostCenterDistributionService(
            CostCenterRepository costCenterRepository,
            EmployeeCostCenterLookupPort employeeCostCenterLookupPort
    ) {
        this.costCenterRepository = costCenterRepository;
        this.employeeCostCenterLookupPort = employeeCostCenterLookupPort;
    }

    @Override
    public CostCenterDistributionReadModel.CurrentDistribution getCurrent(GetCurrentCostCenterDistributionQuery query) {
        String ruleSystemCode = normalizeRuleSystemCode(query.ruleSystemCode());
        String employeeTypeCode = normalizeEmployeeTypeCode(query.employeeTypeCode());
        String employeeNumber = normalizeEmployeeNumber(query.employeeNumber());

        EmployeeCostCenterContext employee = employeeCostCenterLookupPort
                .findByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .orElseThrow(() -> new CostCenterEmployeeNotFoundException(
                        ruleSystemCode, employeeTypeCode, employeeNumber
                ));

        List<CostCenterAllocation> activeLines = costCenterRepository.findActiveAtDate(
                employee.employeeId(), LocalDate.now()
        );

        CostCenterDistributionReadModel.Window currentWindow = null;
        if (!activeLines.isEmpty()) {
            currentWindow = buildWindow(activeLines);
        }

        return new CostCenterDistributionReadModel.CurrentDistribution(
                ruleSystemCode, employeeTypeCode, employeeNumber, currentWindow
        );
    }

    private CostCenterDistributionReadModel.Window buildWindow(List<CostCenterAllocation> lines) {
        List<CostCenterDistributionReadModel.Item> items = lines.stream()
                .map(line -> new CostCenterDistributionReadModel.Item(
                        line.getCostCenterCode(), line.getAllocationPercentage()
                ))
                .toList();

        BigDecimal total = lines.stream()
                .map(CostCenterAllocation::getAllocationPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate startDate = lines.get(0).getStartDate();
        LocalDate endDate = lines.get(0).getEndDate();

        return new CostCenterDistributionReadModel.Window(startDate, endDate, total, items);
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
