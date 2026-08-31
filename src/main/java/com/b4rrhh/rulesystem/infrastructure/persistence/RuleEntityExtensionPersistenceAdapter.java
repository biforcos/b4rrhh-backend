package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.rulesystem.domain.model.RuleEntityExtension;
import com.b4rrhh.rulesystem.domain.port.RuleEntityExtensionRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleEntityExtensionPersistenceAdapter implements RuleEntityExtensionRepository {

    private final SpringDataRuleEntityExtensionRepository springDataRuleEntityExtensionRepository;

    public RuleEntityExtensionPersistenceAdapter(
            SpringDataRuleEntityExtensionRepository springDataRuleEntityExtensionRepository
    ) {
        this.springDataRuleEntityExtensionRepository = springDataRuleEntityExtensionRepository;
    }

    @Override
    public List<RuleEntityExtension> findAll() {
        return springDataRuleEntityExtensionRepository.findAll().stream().map(this::toDomain).toList();
    }

    private RuleEntityExtension toDomain(RuleEntityExtensionEntity entity) {
        return new RuleEntityExtension(
                entity.getRuleEntityTypeCode(),
                entity.getExtensionCode(),
                entity.getTableName(),
                entity.getCardinality(),
                entity.isRequired()
        );
    }
}
