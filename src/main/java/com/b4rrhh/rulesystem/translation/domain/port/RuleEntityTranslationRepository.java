package com.b4rrhh.rulesystem.translation.domain.port;

import com.b4rrhh.rulesystem.translation.domain.model.RuleEntityTranslation;

import java.util.Optional;

public interface RuleEntityTranslationRepository {

    Optional<RuleEntityTranslation> findByRuleEntityIdAndLanguageCode(Long ruleEntityId, String languageCode);
}
