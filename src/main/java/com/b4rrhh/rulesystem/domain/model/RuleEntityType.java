package com.b4rrhh.rulesystem.domain.model;

import java.time.LocalDateTime;

public class RuleEntityType {

    /**
     * El nivel en el que nace un tipo creado por la API (ADR-077). Cambiarlo es mover sus
     * entidades de capa, y eso es una migración, no una llamada.
     */
    public static final int NATIONAL_LEVEL = 3;

    private final Long id;
    private final String code;
    private final String name;
    private final LiteralClass literalClass;
    private final MaintenanceMode maintenanceMode;
    private final String groupCode;
    private final int level;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public RuleEntityType(
            Long id,
            String code,
            String name,
            LiteralClass literalClass,
            MaintenanceMode maintenanceMode,
            String groupCode,
            int level,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.literalClass = literalClass;
        this.maintenanceMode = maintenanceMode;
        this.groupCode = groupCode;
        this.level = level;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public LiteralClass getLiteralClass() { return literalClass; }
    public MaintenanceMode getMaintenanceMode() { return maintenanceMode; }
    public String getGroupCode() { return groupCode; }
    public int getLevel() { return level; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
