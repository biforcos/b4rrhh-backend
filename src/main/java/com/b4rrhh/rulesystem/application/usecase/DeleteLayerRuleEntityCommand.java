package com.b4rrhh.rulesystem.application.usecase;

import java.time.LocalDate;

public record DeleteLayerRuleEntityCommand(
        String layerCode,
        String ruleEntityTypeCode,
        String code,
        LocalDate startDate
) {
}
