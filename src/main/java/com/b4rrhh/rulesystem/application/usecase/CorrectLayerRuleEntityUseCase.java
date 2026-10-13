package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;

public interface CorrectLayerRuleEntityUseCase {
    RuleEntity correct(CorrectLayerRuleEntityCommand command);
}
