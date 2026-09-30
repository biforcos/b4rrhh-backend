package com.b4rrhh.rulesystem.translation.infrastructure.persistence;

import com.b4rrhh.rulesystem.translation.domain.model.RuleEntityTranslation;
import com.b4rrhh.rulesystem.translation.domain.port.RuleEntityTranslationRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class RuleEntityTranslationPersistenceAdapter implements RuleEntityTranslationRepository {

    private final SpringDataRuleEntityTranslationRepository springDataRuleEntityTranslationRepository;

    public RuleEntityTranslationPersistenceAdapter(
            SpringDataRuleEntityTranslationRepository springDataRuleEntityTranslationRepository
    ) {
        this.springDataRuleEntityTranslationRepository = springDataRuleEntityTranslationRepository;
    }

    @Override
    public Optional<RuleEntityTranslation> findByRuleEntityIdAndLanguageCode(Long ruleEntityId, String languageCode) {
        return springDataRuleEntityTranslationRepository
                .findByRuleEntityIdAndLanguageCode(ruleEntityId, languageCode)
                .map(RuleEntityTranslationPersistenceAdapter::toDomain);
    }

    @Override
    public List<RuleEntityTranslation> findByRuleEntityIdsAndLanguageCode(
            Collection<Long> ruleEntityIds,
            String languageCode
    ) {
        if (ruleEntityIds.isEmpty()) {
            return List.of();
        }
        return springDataRuleEntityTranslationRepository
                .findByRuleEntityIdInAndLanguageCode(ruleEntityIds, languageCode).stream()
                .map(RuleEntityTranslationPersistenceAdapter::toDomain)
                .toList();
    }

    private static RuleEntityTranslation toDomain(RuleEntityTranslationEntity entity) {
        return new RuleEntityTranslation(
                entity.getRuleEntityId(),
                entity.getLanguageCode(),
                entity.getName(),
                entity.getDescription()
        );
    }
}
