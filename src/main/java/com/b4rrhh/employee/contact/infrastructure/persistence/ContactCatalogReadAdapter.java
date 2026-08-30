package com.b4rrhh.employee.contact.infrastructure.persistence;

import com.b4rrhh.employee.contact.application.port.ContactCatalogReadPort;
import com.b4rrhh.employee.contact.application.usecase.ContactRuleEntityTypeCodes;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ContactCatalogReadAdapter implements ContactCatalogReadPort {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;

    public ContactCatalogReadAdapter(RuleEntityLabelResolver ruleEntityLabelResolver) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
    }

    @Override
    public Optional<String> findContactTypeName(String ruleSystemCode, String contactTypeCode, String languageCode) {
        return ruleEntityLabelResolver.resolveName(
                ruleSystemCode,
                ContactRuleEntityTypeCodes.CONTACT_TYPE,
                contactTypeCode,
                languageCode
        );
    }
}
