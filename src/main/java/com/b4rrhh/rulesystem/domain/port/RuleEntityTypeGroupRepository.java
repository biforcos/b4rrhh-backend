package com.b4rrhh.rulesystem.domain.port;

import com.b4rrhh.rulesystem.domain.model.RuleEntityTypeGroup;

import java.util.List;

public interface RuleEntityTypeGroupRepository {

    /** Todos los grupos, en el orden del menú ({@code display_order}). */
    List<RuleEntityTypeGroup> findAll();
}
