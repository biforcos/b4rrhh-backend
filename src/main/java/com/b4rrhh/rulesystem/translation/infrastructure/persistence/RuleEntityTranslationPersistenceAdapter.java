package com.b4rrhh.rulesystem.translation.infrastructure.persistence;

import com.b4rrhh.rulesystem.translation.domain.model.RuleEntityTranslation;
import com.b4rrhh.rulesystem.translation.domain.port.RuleEntityTranslationRepository;
import org.springframework.stereotype.Component;

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
                .map(entity -> new RuleEntityTranslation(
                        entity.getRuleEntityId(),
                        entity.getLanguageCode(),
                        entity.getName(),
                        entity.getDescription()
                ));
    }
}
