package com.b4rrhh.employee.cost_center.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read model for a distribution window, used as output from use cases: codes and figures
 * only. The cost center name is presentation and gets resolved in the web layer
 * (ADR-052 §4; backend#27).
 */
public class CostCenterDistributionReadModel {

    public record Item(
            String costCenterCode,
            BigDecimal allocationPercentage
    ) {
    }

    public record Window(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal totalAllocationPercentage,
            List<Item> items
    ) {
    }

    public record CurrentDistribution(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            Window currentDistribution
    ) {
    }

    public record History(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            List<Window> windows
    ) {
    }
}
