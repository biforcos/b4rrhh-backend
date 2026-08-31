package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.model.LiteralClass;
import com.b4rrhh.rulesystem.domain.model.MaintenanceMode;

public record CreateRuleEntityTypeCommand(
        String code,
        String name,
        LiteralClass literalClass,
        MaintenanceMode maintenanceMode,
        String groupCode
) {
}
