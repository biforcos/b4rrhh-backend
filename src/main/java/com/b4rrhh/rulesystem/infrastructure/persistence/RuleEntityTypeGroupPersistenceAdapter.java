package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.rulesystem.domain.model.RuleEntityTypeGroup;
import com.b4rrhh.rulesystem.domain.port.RuleEntityTypeGroupRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleEntityTypeGroupPersistenceAdapter implements RuleEntityTypeGroupRepository {

    private final SpringDataRuleEntityTypeGroupRepository springDataRuleEntityTypeGroupRepository;

    public RuleEntityTypeGroupPersistenceAdapter(
            SpringDataRuleEntityTypeGroupRepository springDataRuleEntityTypeGroupRepository
    ) {
        this.springDataRuleEntityTypeGroupRepository = springDataRuleEntityTypeGroupRepository;
    }

    @Override
    public List<RuleEntityTypeGroup> findAll() {
        return springDataRuleEntityTypeGroupRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(entity -> new RuleEntityTypeGroup(
                        entity.getCode(),
                        entity.getName(),
                        entity.getDisplayOrder()))
                .toList();
    }
}
