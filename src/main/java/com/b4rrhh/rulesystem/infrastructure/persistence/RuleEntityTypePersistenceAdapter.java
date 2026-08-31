package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.rulesystem.domain.model.RuleEntityType;
import com.b4rrhh.rulesystem.domain.port.RuleEntityTypeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RuleEntityTypePersistenceAdapter implements RuleEntityTypeRepository {

    private final SpringDataRuleEntityTypeRepository springDataRuleEntityTypeRepository;

    public RuleEntityTypePersistenceAdapter(SpringDataRuleEntityTypeRepository springDataRuleEntityTypeRepository) {
        this.springDataRuleEntityTypeRepository = springDataRuleEntityTypeRepository;
    }

    @Override
    public Optional<RuleEntityType> findByCode(String code) {
        return springDataRuleEntityTypeRepository.findByCode(code).map(this::toDomain);
    }

    @Override
    public List<RuleEntityType> findAll() {
        return springDataRuleEntityTypeRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public RuleEntityType save(RuleEntityType ruleEntityType) {
        RuleEntityTypeEntity entity = toEntity(ruleEntityType);
        RuleEntityTypeEntity saved = springDataRuleEntityTypeRepository.save(entity);
        return toDomain(saved);
    }

    private RuleEntityType toDomain(RuleEntityTypeEntity entity) {
        return new RuleEntityType(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getLiteralClass(),
                entity.getMaintenanceMode(),
                entity.getGroupCode(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private RuleEntityTypeEntity toEntity(RuleEntityType ruleEntityType) {
        RuleEntityTypeEntity entity = new RuleEntityTypeEntity();
        entity.setId(ruleEntityType.getId());
        entity.setCode(ruleEntityType.getCode());
        entity.setName(ruleEntityType.getName());
        entity.setLiteralClass(ruleEntityType.getLiteralClass());
        entity.setMaintenanceMode(ruleEntityType.getMaintenanceMode());
        entity.setGroupCode(ruleEntityType.getGroupCode());
        entity.setActive(ruleEntityType.isActive());
        return entity;
    }
}
