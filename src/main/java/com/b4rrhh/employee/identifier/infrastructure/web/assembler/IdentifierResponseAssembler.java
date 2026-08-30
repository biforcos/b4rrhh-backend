package com.b4rrhh.employee.identifier.infrastructure.web.assembler;

import com.b4rrhh.employee.identifier.application.usecase.IdentifierRuleEntityTypeCodes;
import com.b4rrhh.employee.identifier.domain.model.Identifier;
import com.b4rrhh.employee.identifier.infrastructure.web.dto.IdentifierResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IdentifierResponseAssembler {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;

    public IdentifierResponseAssembler(RuleEntityLabelResolver ruleEntityLabelResolver) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
    }

    public IdentifierResponse toResponse(String ruleSystemCode, Identifier identifier, ResponseLanguage language) {
        String identifierTypeName = ruleEntityLabelResolver
                .resolveName(ruleSystemCode, IdentifierRuleEntityTypeCodes.EMPLOYEE_IDENTIFIER_TYPE,
                        identifier.getIdentifierTypeCode(), language.code())
                .orElse(null);

        return new IdentifierResponse(
                identifier.getIdentifierTypeCode(),
                identifierTypeName,
                identifier.getIdentifierValue(),
                identifier.getIssuingCountryCode(),
                identifier.getExpirationDate(),
                identifier.isPrimary()
        );
    }

    public List<IdentifierResponse> toResponseList(String ruleSystemCode, List<Identifier> identifiers, ResponseLanguage language) {
        return identifiers.stream()
                .map(identifier -> toResponse(ruleSystemCode, identifier, language))
                .toList();
    }
}
