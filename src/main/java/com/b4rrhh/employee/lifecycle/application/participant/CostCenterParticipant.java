package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.cost_center.application.usecase.CostCenterDistributionItem;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionCommand;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionUseCase;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterCatalogValueInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionInvalidException;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.application.port.HireParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CostCenterParticipant implements HireParticipant {

    private final CreateCostCenterDistributionUseCase createCostCenterDistributionUseCase;

    public CostCenterParticipant(CreateCostCenterDistributionUseCase createCostCenterDistributionUseCase) {
        this.createCostCenterDistributionUseCase = createCostCenterDistributionUseCase;
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public void participate(HireContext ctx) {
        HireEmployeeCommand.HireEmployeeCostCenterDistributionCommand costCenterCmd =
                ctx.costCenterDistribution();
        if (costCenterCmd == null) {
            return;
        }
        List<CostCenterDistributionItem> items = costCenterCmd.items().stream()
                .map(item -> new CostCenterDistributionItem(
                        item.costCenterCode(),
                        BigDecimal.valueOf(item.allocationPercentage())))
                .toList();
        try {
            ctx.setCostCenter(createCostCenterDistributionUseCase.create(
                    new CreateCostCenterDistributionCommand(
                            ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                            ctx.hireDate(), null, items)));
        } catch (CostCenterCatalogValueInvalidException | CostCenterDistributionInvalidException ex) {
            throw new HireEmployeeCatalogValueInvalidException(ex.getMessage(), ex);
        }
    }
}
