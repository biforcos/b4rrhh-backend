package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.model.RuleEntityExtension;
import com.b4rrhh.rulesystem.domain.port.RuleEntityExtensionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListRuleEntityExtensionsService implements ListRuleEntityExtensionsUseCase {

    private final RuleEntityExtensionRepository ruleEntityExtensionRepository;

    public ListRuleEntityExtensionsService(RuleEntityExtensionRepository ruleEntityExtensionRepository) {
        this.ruleEntityExtensionRepository = ruleEntityExtensionRepository;
    }

    @Override
    public List<RuleEntityExtension> listAll() {
        return ruleEntityExtensionRepository.findAll();
    }
}
