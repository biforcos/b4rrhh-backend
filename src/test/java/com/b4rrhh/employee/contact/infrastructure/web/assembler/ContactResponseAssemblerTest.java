package com.b4rrhh.employee.contact.infrastructure.web.assembler;

import com.b4rrhh.employee.contact.domain.model.Contact;
import com.b4rrhh.employee.contact.infrastructure.web.dto.ContactResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactResponseAssemblerTest {

    @Mock
    private RuleEntityLabelResolver ruleEntityLabelResolver;

    @Test
    void toResponseEnrichesLabelInTheLanguageOfTheResponse() {
        ContactResponseAssembler assembler = new ContactResponseAssembler(ruleEntityLabelResolver);
        Contact contact = contact(10L, "EMAIL", "john.doe@example.com");
        when(ruleEntityLabelResolver.resolveName("ESP", "CONTACT_TYPE", "EMAIL", "es-ES"))
                .thenReturn(Optional.of("Correo electronico"));

        ContactResponse response = assembler.toResponse("ESP", contact, new ResponseLanguage("es-ES"));

        assertEquals("EMAIL", response.contactTypeCode());
        assertEquals("Correo electronico", response.contactTypeName());
        assertEquals("john.doe@example.com", response.contactValue());
    }

    @Test
    void toResponseKeepsCodeAndUsesNullWhenLabelMissing() {
        ContactResponseAssembler assembler = new ContactResponseAssembler(ruleEntityLabelResolver);
        Contact contact = contact(10L, "EMAIL", "john.doe@example.com");
        when(ruleEntityLabelResolver.resolveName("ESP", "CONTACT_TYPE", "EMAIL", null))
                .thenReturn(Optional.empty());

        ContactResponse response = assembler.toResponse("ESP", contact, ResponseLanguage.base());

        assertEquals("EMAIL", response.contactTypeCode());
        assertNull(response.contactTypeName());
        assertEquals("john.doe@example.com", response.contactValue());
    }

    private Contact contact(Long employeeId, String contactTypeCode, String contactValue) {
        return new Contact(
                1L,
                employeeId,
                contactTypeCode,
                contactValue,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
