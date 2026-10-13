package com.b4rrhh.rulesystem.application.usecase;

import java.time.LocalDate;

public record CorrectLayerRuleEntityCommand(
        String layerCode,
        String ruleEntityTypeCode,
        String code,
        LocalDate startDate,
        String name,
        String description,
        LocalDate endDate
) {
}
