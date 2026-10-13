package com.b4rrhh.rulesystem.infrastructure.web.dto;

import java.util.List;

public record LayerResponse(
        String code,
        String name,
        int level,
        boolean active,
        List<String> mountedBy
) {
}
