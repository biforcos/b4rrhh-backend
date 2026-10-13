package com.b4rrhh.rulesystem.application.usecase;

import java.time.LocalDate;

public record ListLayerRuleEntitiesQuery(
        String layerCode,
        String ruleEntityTypeCode,
        String code,
        Boolean active,
        LocalDate referenceDate
) {
}
