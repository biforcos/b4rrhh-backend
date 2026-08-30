package com.b4rrhh.employee.contact.infrastructure.web;

import com.b4rrhh.employee.contact.application.usecase.CreateContactCommand;
import com.b4rrhh.employee.contact.application.usecase.CreateContactUseCase;
import com.b4rrhh.employee.contact.application.usecase.DeleteContactUseCase;
import com.b4rrhh.employee.contact.application.usecase.GetContactByBusinessKeyUseCase;
import com.b4rrhh.employee.contact.application.usecase.ListEmployeeContactsUseCase;
import com.b4rrhh.employee.contact.application.usecase.UpdateContactCommand;
import com.b4rrhh.employee.contact.application.usecase.UpdateContactUseCase;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguageArgumentResolver;
import com.b4rrhh.employee.contact.domain.exception.ContactAlreadyExistsException;
import com.b4rrhh.employee.contact.domain.exception.ContactNotFoundException;
import com.b4rrhh.employee.contact.domain.model.Contact;
import com.b4rrhh.employee.contact.infrastructure.web.assembler.ContactResponseAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContactControllerHttpTest {

    @Mock
    private CreateContactUseCase createContactUseCase;
    @Mock
    private UpdateContactUseCase updateContactUseCase;
    @Mock
    private GetContactByBusinessKeyUseCase getContactByBusinessKeyUseCase;
    @Mock
    private ListEmployeeContactsUseCase listEmployeeContactsUseCase;
    @Mock
    private DeleteContactUseCase deleteContactUseCase;
        @Mock
        private RuleEntityLabelResolver ruleEntityLabelResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ContactController controller = new ContactController(
                createContactUseCase,
                updateContactUseCase,
                getContactByBusinessKeyUseCase,
                listEmployeeContactsUseCase,
                deleteContactUseCase,
                new ContactResponseAssembler(ruleEntityLabelResolver)
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ContactExceptionHandler())
                .setCustomArgumentResolvers(new ResponseLanguageArgumentResolver())
                .build();
    }

    @Test
    void createMapsPathAndBodyToCommand() throws Exception {
        when(ruleEntityLabelResolver.resolveName("ESP", "CONTACT_TYPE", "EMAIL", null))
                .thenReturn(Optional.of("Correo electronico"));
        when(createContactUseCase.create(any(CreateContactCommand.class)))
                .thenReturn(contact(10L, "EMAIL", "john.doe@example.com"));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contactTypeCode": "EMAIL",
                                  "contactValue": "john.doe@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contactTypeCode").value("EMAIL"))
                .andExpect(jsonPath("$.contactTypeName").value("Correo electronico"))
                .andExpect(jsonPath("$.contactValue").value("john.doe@example.com"));

        ArgumentCaptor<CreateContactCommand> captor = ArgumentCaptor.forClass(CreateContactCommand.class);
        verify(createContactUseCase).create(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals("EMAIL", captor.getValue().contactTypeCode());
        assertEquals("john.doe@example.com", captor.getValue().contactValue());
    }

    @Test
    void updateMapsPathAndBodyToCommand() throws Exception {
        when(ruleEntityLabelResolver.resolveName("ESP", "CONTACT_TYPE", "EMAIL", null))
                .thenReturn(Optional.empty());
        when(updateContactUseCase.update(any(UpdateContactCommand.class)))
                .thenReturn(contact(10L, "EMAIL", "updated@example.com"));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/contacts/EMAIL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contactValue": "updated@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactTypeCode").value("EMAIL"))
                .andExpect(jsonPath("$.contactTypeName").isEmpty())
                .andExpect(jsonPath("$.contactValue").value("updated@example.com"));

        ArgumentCaptor<UpdateContactCommand> captor = ArgumentCaptor.forClass(UpdateContactCommand.class);
        verify(updateContactUseCase).update(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals("EMAIL", captor.getValue().contactTypeCode());
        assertEquals("updated@example.com", captor.getValue().contactValue());
    }

    @Test
    void updateRejectsContactTypeCodeInBody() throws Exception {
        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/contacts/EMAIL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contactTypeCode": "MOBILE",
                                  "contactValue": "updated@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(updateContactUseCase);
    }

    @Test
    void createMapsDomainConflictToHttp409() throws Exception {
        when(createContactUseCase.create(any(CreateContactCommand.class)))
                .thenThrow(new ContactAlreadyExistsException("ESP", "INTERNAL", "EMP001", "EMAIL"));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contactTypeCode": "EMAIL",
                                  "contactValue": "john.doe@example.com"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Contact already exists")));
    }

    @Test
    void deleteMapsDomainNotFoundToHttp404() throws Exception {
        org.mockito.Mockito.doThrow(new ContactNotFoundException("ESP", "INTERNAL", "EMP001", "EMAIL"))
                .when(deleteContactUseCase)
                .delete(any());

        mockMvc.perform(delete("/employees/ESP/INTERNAL/EMP001/contacts/EMAIL"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Contact not found")));
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

    // ADR-052 §4 (backend#24): el idioma entra por Accept-Language y llega al resolutor desde el ensamblador.
    @Test
    void listServesTheContactTypeInTheLanguageOfTheAcceptLanguageHeader() throws Exception {
        when(listEmployeeContactsUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(contact(10L, "EMAIL", "john.doe@example.com")));
        when(ruleEntityLabelResolver.resolveName("ESP", "CONTACT_TYPE", "EMAIL", "es-ES"))
                .thenReturn(Optional.of("Correo electrónico"));

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/contacts")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contactTypeName").value("Correo electrónico"));
    }
}
