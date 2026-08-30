package com.b4rrhh.employee.cost_center.infrastructure.persistence;

import com.b4rrhh.employee.cost_center.application.port.CostCenterCatalogReadPort;
import com.b4rrhh.employee.cost_center.application.usecase.CostCenterRuleEntityTypeCodes;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CostCenterCatalogReadAdapter implements CostCenterCatalogReadPort {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;

    public CostCenterCatalogReadAdapter(RuleEntityLabelResolver ruleEntityLabelResolver) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
    }

    @Override
    public Optional<String> findCostCenterName(String ruleSystemCode, String costCenterCode, String languageCode) {
        return ruleEntityLabelResolver.resolveName(
                ruleSystemCode,
                CostCenterRuleEntityTypeCodes.COST_CENTER,
                costCenterCode,
                languageCode
        );
    }
}
