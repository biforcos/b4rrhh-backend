package com.b4rrhh.rulesystem.translation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataRuleEntityTranslationRepository
        extends JpaRepository<RuleEntityTranslationEntity, RuleEntityTranslationId> {

    Optional<RuleEntityTranslationEntity> findByRuleEntityIdAndLanguageCode(Long ruleEntityId, String languageCode);
}
