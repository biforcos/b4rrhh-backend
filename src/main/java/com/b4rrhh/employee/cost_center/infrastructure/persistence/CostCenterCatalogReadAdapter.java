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
    // Se resuelve dentro de un caso de uso, no en la capa web, asi que no hay idioma que
    // pasar: literal base hasta que backend#27 decida donde vive esta resolucion.
    public Optional<String> findCostCenterName(String ruleSystemCode, String costCenterCode) {
        return ruleEntityLabelResolver.resolveName(
                ruleSystemCode,
                CostCenterRuleEntityTypeCodes.COST_CENTER,
                costCenterCode,
                null
        );
    }
}
