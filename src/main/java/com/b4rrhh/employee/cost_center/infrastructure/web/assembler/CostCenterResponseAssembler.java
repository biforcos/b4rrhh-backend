package com.b4rrhh.employee.cost_center.infrastructure.web.assembler;

import com.b4rrhh.employee.cost_center.application.usecase.CostCenterDistributionReadModel;
import com.b4rrhh.employee.cost_center.application.usecase.CostCenterRuleEntityTypeCodes;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterCurrentDistributionResponse;
import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterDistributionHistoryResponse;
import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterDistributionItemResponse;
import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterDistributionWindowResponse;
import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterEmployeeKeyResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convierte los códigos del reparto de centros de coste en literales, en el idioma de la
 * respuesta. La conversión ocurre aquí, en la capa web, y por eso el idioma llega al
 * resolutor sin que ningún caso de uso cambie de firma (ADR-052 §4; backend#27).
 */
@Component
public class CostCenterResponseAssembler {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;

    public CostCenterResponseAssembler(RuleEntityLabelResolver ruleEntityLabelResolver) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
    }

    public CostCenterCurrentDistributionResponse toCurrentResponse(
            CostCenterDistributionReadModel.CurrentDistribution model,
            ResponseLanguage language
    ) {
        CostCenterEmployeeKeyResponse employeeKey = new CostCenterEmployeeKeyResponse(
                model.ruleSystemCode(), model.employeeTypeCode(), model.employeeNumber()
        );

        CostCenterDistributionWindowResponse window = null;
        if (model.currentDistribution() != null) {
            window = toWindowResponse(model.ruleSystemCode(), model.currentDistribution(), language);
        }

        return new CostCenterCurrentDistributionResponse(employeeKey, window);
    }

    public CostCenterDistributionHistoryResponse toHistoryResponse(
            CostCenterDistributionReadModel.History model,
            ResponseLanguage language
    ) {
        CostCenterEmployeeKeyResponse employeeKey = new CostCenterEmployeeKeyResponse(
                model.ruleSystemCode(), model.employeeTypeCode(), model.employeeNumber()
        );

        List<CostCenterDistributionWindowResponse> windows = model.windows().stream()
                .map(window -> toWindowResponse(model.ruleSystemCode(), window, language))
                .toList();

        return new CostCenterDistributionHistoryResponse(employeeKey, windows);
    }

    /**
     * Respuesta de los endpoints de comando: la misma forma de siempre, con
     * {@code costCenterName} a null porque a ese nivel nunca se ha enriquecido.
     */
    public CostCenterDistributionWindowResponse toWindowResponse(CostCenterDistributionWindow window) {
        List<CostCenterDistributionItemResponse> items = window.getItems().stream()
                .map(item -> new CostCenterDistributionItemResponse(
                        item.getCostCenterCode(),
                        null, // no enrichment at command response level
                        item.getAllocationPercentage()
                ))
                .toList();

        return new CostCenterDistributionWindowResponse(
                window.getStartDate(),
                window.getEndDate(),
                window.getTotalAllocationPercentage(),
                items
        );
    }

    private CostCenterDistributionWindowResponse toWindowResponse(
            String ruleSystemCode,
            CostCenterDistributionReadModel.Window window,
            ResponseLanguage language
    ) {
        List<CostCenterDistributionItemResponse> items = window.items().stream()
                .map(item -> new CostCenterDistributionItemResponse(
                        item.costCenterCode(),
                        ruleEntityLabelResolver.resolveName(
                                ruleSystemCode,
                                CostCenterRuleEntityTypeCodes.COST_CENTER,
                                item.costCenterCode(),
                                language.code()
                        ).orElse(null),
                        item.allocationPercentage()
                ))
                .toList();

        return new CostCenterDistributionWindowResponse(
                window.startDate(), window.endDate(), window.totalAllocationPercentage(), items
        );
    }
}
