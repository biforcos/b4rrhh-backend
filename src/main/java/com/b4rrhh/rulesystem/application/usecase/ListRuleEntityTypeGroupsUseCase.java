package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.model.RuleEntityTypeGroup;

import java.util.List;

public interface ListRuleEntityTypeGroupsUseCase {

    /** Todos los grupos del menú, en su orden. */
    List<RuleEntityTypeGroup> listAll();
}
