package com.b4rrhh.rulesystem.catalogoption.infrastructure.persistence;

import com.b4rrhh.rulesystem.catalogoption.domain.model.DirectCatalogOption;
import com.b4rrhh.rulesystem.catalogoption.domain.port.DirectCatalogOptionRepository;
import com.b4rrhh.rulesystem.infrastructure.persistence.RuleEntityEntity;
import com.b4rrhh.rulesystem.infrastructure.persistence.SpringDataRuleEntityRepository;
import com.b4rrhh.rulesystem.translation.domain.model.LanguageCode;
import com.b4rrhh.rulesystem.translation.infrastructure.persistence.RuleEntityTranslationEntity;
import com.b4rrhh.rulesystem.translation.infrastructure.persistence.SpringDataRuleEntityTranslationRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RuleEntityDirectCatalogOptionReadAdapter implements DirectCatalogOptionRepository {

    private final SpringDataRuleEntityRepository springDataRuleEntityRepository;
    private final SpringDataRuleEntityTranslationRepository springDataRuleEntityTranslationRepository;

    public RuleEntityDirectCatalogOptionReadAdapter(
            SpringDataRuleEntityRepository springDataRuleEntityRepository,
            SpringDataRuleEntityTranslationRepository springDataRuleEntityTranslationRepository
    ) {
        this.springDataRuleEntityRepository = springDataRuleEntityRepository;
        this.springDataRuleEntityTranslationRepository = springDataRuleEntityTranslationRepository;
    }

    @Override
    public List<DirectCatalogOption> findDirectOptions(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            LocalDate referenceDate,
            String qLike,
            String languageCode
    ) {
        List<RuleEntityEntity> entities = springDataRuleEntityRepository
                .findDirectCatalogOptions(
                        ruleSystemCode,
                        ruleEntityTypeCode,
                        referenceDate,
                        qLike,
                        SpringDataRuleEntityRepository.MAX_DATE
                );
        Map<Long, String> translatedNames = translatedNames(entities, languageCode);

        return entities.stream()
                .map(entity -> toDomain(entity, translatedNames.get(entity.getId())))
                .toList();
    }

    // Una consulta por lote, no una por opcion: es lo que alimenta todos los desplegables.
    private Map<Long, String> translatedNames(List<RuleEntityEntity> entities, String languageCode) {
        Optional<String> language = LanguageCode.canonical(languageCode);
        if (language.isEmpty() || entities.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = entities.stream().map(RuleEntityEntity::getId).toList();
        return springDataRuleEntityTranslationRepository
                .findByRuleEntityIdInAndLanguageCode(ids, language.get())
                .stream()
                .filter(translation -> translation.getName() != null && !translation.getName().isBlank())
                .collect(Collectors.toMap(RuleEntityTranslationEntity::getRuleEntityId, RuleEntityTranslationEntity::getName));
    }

    private DirectCatalogOption toDomain(RuleEntityEntity entity, String translatedName) {
        return new DirectCatalogOption(
                entity.getCode(),
                translatedName != null ? translatedName : entity.getName(),
                entity.isActive(),
                entity.getStartDate(),
                entity.getEndDate()
        );
    }
}
