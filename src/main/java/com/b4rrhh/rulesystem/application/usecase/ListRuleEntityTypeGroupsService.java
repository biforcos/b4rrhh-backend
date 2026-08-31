package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.model.RuleEntityTypeGroup;
import com.b4rrhh.rulesystem.domain.port.RuleEntityTypeGroupRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListRuleEntityTypeGroupsService implements ListRuleEntityTypeGroupsUseCase {

    private final RuleEntityTypeGroupRepository ruleEntityTypeGroupRepository;

    public ListRuleEntityTypeGroupsService(RuleEntityTypeGroupRepository ruleEntityTypeGroupRepository) {
        this.ruleEntityTypeGroupRepository = ruleEntityTypeGroupRepository;
    }

    @Override
    public List<RuleEntityTypeGroup> listAll() {
        return ruleEntityTypeGroupRepository.findAll();
    }
}
