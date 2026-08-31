package com.b4rrhh.rulesystem.infrastructure.web.dto;

import java.util.List;

public record RuleEntityTypeResponse(
        String code,
        String name,
        boolean active,
        String literalClass,
        String maintenanceMode,
        RuleEntityTypeGroupResponse group,
        List<RuleEntityTypeExtensionResponse> extensions
) {
}
