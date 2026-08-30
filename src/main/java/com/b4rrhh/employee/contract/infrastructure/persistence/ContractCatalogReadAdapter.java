package com.b4rrhh.employee.contract.infrastructure.persistence;

import com.b4rrhh.employee.contract.application.port.ContractCatalogReadPort;
import com.b4rrhh.employee.contract.application.usecase.ContractRuleEntityTypeCodes;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ContractCatalogReadAdapter implements ContractCatalogReadPort {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;

    public ContractCatalogReadAdapter(RuleEntityLabelResolver ruleEntityLabelResolver) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
    }

    @Override
    public Optional<String> findContractTypeName(String ruleSystemCode, String contractCode, String languageCode) {
        return ruleEntityLabelResolver.resolveName(
                ruleSystemCode,
                ContractRuleEntityTypeCodes.CONTRACT,
                contractCode,
                languageCode
        );
    }

    @Override
    public Optional<String> findContractSubtypeName(String ruleSystemCode, String contractSubtypeCode, String languageCode) {
        return ruleEntityLabelResolver.resolveName(
                ruleSystemCode,
                ContractRuleEntityTypeCodes.CONTRACT_SUBTYPE,
                contractSubtypeCode,
                languageCode
        );
    }
}
