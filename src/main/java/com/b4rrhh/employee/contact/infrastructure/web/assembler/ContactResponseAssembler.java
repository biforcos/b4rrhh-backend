package com.b4rrhh.employee.contact.infrastructure.web.assembler;

import com.b4rrhh.employee.contact.application.usecase.ContactRuleEntityTypeCodes;
import com.b4rrhh.employee.contact.domain.model.Contact;
import com.b4rrhh.employee.contact.infrastructure.web.dto.ContactResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContactResponseAssembler {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;

    public ContactResponseAssembler(RuleEntityLabelResolver ruleEntityLabelResolver) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
    }

    public ContactResponse toResponse(String ruleSystemCode, Contact contact, ResponseLanguage language) {
        String contactTypeName = ruleEntityLabelResolver
                .resolveName(ruleSystemCode, ContactRuleEntityTypeCodes.CONTACT_TYPE,
                        contact.getContactTypeCode(), language.code())
                .orElse(null);

        return new ContactResponse(
                contact.getContactTypeCode(),
                contactTypeName,
                contact.getContactValue()
        );
    }

    public List<ContactResponse> toResponseList(String ruleSystemCode, List<Contact> contacts, ResponseLanguage language) {
        return contacts.stream()
                .map(contact -> toResponse(ruleSystemCode, contact, language))
                .toList();
    }
}
