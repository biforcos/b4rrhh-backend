package com.b4rrhh.rulesystem.application.usecase;

import java.time.LocalDate;

public record CloseLayerRuleEntityCommand(
        String layerCode,
        String ruleEntityTypeCode,
        String code,
        LocalDate startDate,
        LocalDate endDate
) {
}
