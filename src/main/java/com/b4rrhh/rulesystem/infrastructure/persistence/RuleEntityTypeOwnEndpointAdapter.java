package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.rulesystem.application.port.RuleEntityTypeOwnEndpointPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RuleEntityTypeOwnEndpointAdapter implements RuleEntityTypeOwnEndpointPort {

    private final SpringDataRuleEntityTypeRepository springDataRuleEntityTypeRepository;

    public RuleEntityTypeOwnEndpointAdapter(
            SpringDataRuleEntityTypeRepository springDataRuleEntityTypeRepository
    ) {
        this.springDataRuleEntityTypeRepository = springDataRuleEntityTypeRepository;
    }

    @Override
    public Optional<String> findApiCollectionPath(String ruleEntityTypeCode) {
        return springDataRuleEntityTypeRepository.findByCode(ruleEntityTypeCode)
                .map(RuleEntityTypeEntity::getApiCollectionPath);
    }
}
