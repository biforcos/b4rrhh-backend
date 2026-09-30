package com.b4rrhh.rulesystem.translation.infrastructure.persistence;

import com.b4rrhh.rulesystem.translation.domain.model.RuleEntityTypeTranslation;
import com.b4rrhh.rulesystem.translation.domain.port.RuleEntityTypeTranslationRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleEntityTypeTranslationPersistenceAdapter implements RuleEntityTypeTranslationRepository {

    private final SpringDataRuleEntityTypeTranslationRepository springDataRepository;

    public RuleEntityTypeTranslationPersistenceAdapter(
            SpringDataRuleEntityTypeTranslationRepository springDataRepository
    ) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public List<RuleEntityTypeTranslation> findByLanguageCode(String languageCode) {
        return springDataRepository.findByLanguageCode(languageCode).stream()
                .map(entity -> new RuleEntityTypeTranslation(
                        entity.getRuleEntityTypeCode(),
                        entity.getLanguageCode(),
                        entity.getName()
                ))
                .toList();
    }
}
