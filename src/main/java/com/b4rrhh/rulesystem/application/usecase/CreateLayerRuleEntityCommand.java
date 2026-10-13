package com.b4rrhh.rulesystem.application.usecase;

import java.time.LocalDate;

public record CreateLayerRuleEntityCommand(
        String layerCode,
        String ruleEntityTypeCode,
        String code,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate
) {
}
