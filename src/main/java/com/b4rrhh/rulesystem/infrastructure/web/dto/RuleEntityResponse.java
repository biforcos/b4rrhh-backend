package com.b4rrhh.rulesystem.infrastructure.web.dto;

import java.time.LocalDate;

public record RuleEntityResponse(
        String ruleSystemCode,
        String ruleEntityTypeCode,
        String code,
        String name,
        String label,
        String description,
        boolean active,
        LocalDate startDate,
        LocalDate endDate
) {
}
