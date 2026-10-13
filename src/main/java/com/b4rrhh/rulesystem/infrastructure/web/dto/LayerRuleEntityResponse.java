package com.b4rrhh.rulesystem.infrastructure.web.dto;

import java.time.LocalDate;

/** Una entidad vista desde su capa: sin {@code ruleSystemCode}, porque no se pidió desde ninguna (frontend#131). */
public record LayerRuleEntityResponse(
        String layerCode,
        Integer level,
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
