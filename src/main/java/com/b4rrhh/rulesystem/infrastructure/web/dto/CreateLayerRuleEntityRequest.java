package com.b4rrhh.rulesystem.infrastructure.web.dto;

import java.time.LocalDate;

public record CreateLayerRuleEntityRequest(
        String ruleEntityTypeCode,
        String code,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate
) {
}
