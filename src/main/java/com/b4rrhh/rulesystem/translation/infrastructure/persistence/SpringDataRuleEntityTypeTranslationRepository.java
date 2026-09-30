package com.b4rrhh.rulesystem.translation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataRuleEntityTypeTranslationRepository
        extends JpaRepository<RuleEntityTypeTranslationEntity, RuleEntityTypeTranslationId> {

    List<RuleEntityTypeTranslationEntity> findByLanguageCode(String languageCode);
}
