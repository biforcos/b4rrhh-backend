package com.b4rrhh.employee.identifier.infrastructure.persistence;

import com.b4rrhh.employee.identifier.application.port.IdentifierCatalogReadPort;
import com.b4rrhh.employee.identifier.application.usecase.IdentifierRuleEntityTypeCodes;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class IdentifierCatalogReadAdapter implements IdentifierCatalogReadPort {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;

    public IdentifierCatalogReadAdapter(RuleEntityLabelResolver ruleEntityLabelResolver) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
    }

    @Override
    public Optional<String> findIdentifierTypeName(String ruleSystemCode, String identifierTypeCode, String languageCode) {
        return ruleEntityLabelResolver.resolveName(
                ruleSystemCode,
                IdentifierRuleEntityTypeCodes.EMPLOYEE_IDENTIFIER_TYPE,
                identifierTypeCode,
                languageCode
        );
    }
}
