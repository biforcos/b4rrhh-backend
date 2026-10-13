package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;

import java.util.List;

public interface ListLayerRuleEntitiesUseCase {
    List<RuleEntity> list(ListLayerRuleEntitiesQuery query);
}
