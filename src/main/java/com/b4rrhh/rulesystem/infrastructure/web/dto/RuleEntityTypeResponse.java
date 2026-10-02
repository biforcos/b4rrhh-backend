package com.b4rrhh.rulesystem.infrastructure.web.dto;

import java.util.List;

public record RuleEntityTypeResponse(
        String code,
        String name,
        String label,
        int level,
        boolean active,
        String literalClass,
        String maintenanceMode,
        RuleEntityTypeGroupResponse group,
        List<RuleEntityTypeExtensionResponse> extensions
) {
}
