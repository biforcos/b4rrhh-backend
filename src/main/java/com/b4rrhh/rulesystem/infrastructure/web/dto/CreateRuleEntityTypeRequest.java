package com.b4rrhh.rulesystem.infrastructure.web.dto;

public record CreateRuleEntityTypeRequest(
        String code,
        String name,
        String literalClass,
        String maintenanceMode,
        String groupCode
) {
}
