package com.b4rrhh.employee.identifier.infrastructure.web;

import com.b4rrhh.employee.identifier.application.usecase.CreateIdentifierCommand;
import com.b4rrhh.employee.identifier.application.usecase.CreateIdentifierUseCase;
import com.b4rrhh.employee.identifier.application.usecase.DeleteIdentifierUseCase;
import com.b4rrhh.employee.identifier.application.usecase.GetIdentifierByBusinessKeyUseCase;
import com.b4rrhh.employee.identifier.application.usecase.ListEmployeeIdentifiersUseCase;
import com.b4rrhh.employee.identifier.application.usecase.UpdateIdentifierCommand;
import com.b4rrhh.employee.identifier.application.usecase.UpdateIdentifierUseCase;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguageArgumentResolver;
import com.b4rrhh.employee.identifier.domain.exception.IdentifierAlreadyExistsException;
import com.b4rrhh.employee.identifier.domain.exception.IdentifierNotFoundException;
import com.b4rrhh.employee.identifier.domain.model.Identifier;
import com.b4rrhh.employee.identifier.infrastructure.web.assembler.IdentifierResponseAssembler;
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
class IdentifierControllerHttpTest {

    @Mock
    private CreateIdentifierUseCase createIdentifierUseCase;
    @Mock
    private UpdateIdentifierUseCase updateIdentifierUseCase;
    @Mock
    private GetIdentifierByBusinessKeyUseCase getIdentifierByBusinessKeyUseCase;
    @Mock
    private ListEmployeeIdentifiersUseCase listEmployeeIdentifiersUseCase;
    @Mock
    private DeleteIdentifierUseCase deleteIdentifierUseCase;
        @Mock
        private RuleEntityLabelResolver ruleEntityLabelResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        IdentifierController controller = new IdentifierController(
                createIdentifierUseCase,
                updateIdentifierUseCase,
                getIdentifierByBusinessKeyUseCase,
                listEmployeeIdentifiersUseCase,
                deleteIdentifierUseCase,
                new IdentifierResponseAssembler(ruleEntityLabelResolver)
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IdentifierExceptionHandler())
                .setCustomArgumentResolvers(new ResponseLanguageArgumentResolver())
                .build();
    }

    @Test
    void createMapsPathAndBodyToCommand() throws Exception {
        when(ruleEntityLabelResolver.resolveName("ESP", "EMPLOYEE_IDENTIFIER_TYPE", "NATIONAL_ID", null))
                .thenReturn(Optional.of("Documento nacional"));
        when(createIdentifierUseCase.create(any(CreateIdentifierCommand.class)))
                .thenReturn(identifier(10L, "NATIONAL_ID", "12345678A", true));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/identifiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifierTypeCode": "NATIONAL_ID",
                                  "identifierValue": "12345678A",
                                  "issuingCountryCode": "ESP",
                                  "isPrimary": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identifierTypeCode").value("NATIONAL_ID"))
                .andExpect(jsonPath("$.identifierTypeName").value("Documento nacional"))
                .andExpect(jsonPath("$.identifierValue").value("12345678A"))
                .andExpect(jsonPath("$.isPrimary").value(true));

        ArgumentCaptor<CreateIdentifierCommand> captor = ArgumentCaptor.forClass(CreateIdentifierCommand.class);
        verify(createIdentifierUseCase).create(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals("NATIONAL_ID", captor.getValue().identifierTypeCode());
    }

    @Test
    void updateMapsPathAndBodyToCommand() throws Exception {
        when(ruleEntityLabelResolver.resolveName("ESP", "EMPLOYEE_IDENTIFIER_TYPE", "NATIONAL_ID", null))
                .thenReturn(Optional.empty());
        when(updateIdentifierUseCase.update(any(UpdateIdentifierCommand.class)))
                .thenReturn(identifier(10L, "NATIONAL_ID", "87654321Z", true));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/identifiers/NATIONAL_ID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifierValue": "87654321Z",
                                  "issuingCountryCode": "ESP",
                                  "isPrimary": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifierTypeCode").value("NATIONAL_ID"))
                .andExpect(jsonPath("$.identifierTypeName").isEmpty())
                .andExpect(jsonPath("$.identifierValue").value("87654321Z"))
                .andExpect(jsonPath("$.isPrimary").value(true));

        ArgumentCaptor<UpdateIdentifierCommand> captor = ArgumentCaptor.forClass(UpdateIdentifierCommand.class);
        verify(updateIdentifierUseCase).update(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals("NATIONAL_ID", captor.getValue().identifierTypeCode());
    }

    @Test
    void updateRejectsIdentifierTypeCodeInBody() throws Exception {
        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/identifiers/NATIONAL_ID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifierTypeCode": "PASSPORT",
                                  "identifierValue": "87654321Z"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(updateIdentifierUseCase);
    }

    @Test
    void createMapsDomainConflictToHttp409() throws Exception {
        when(createIdentifierUseCase.create(any(CreateIdentifierCommand.class)))
                .thenThrow(new IdentifierAlreadyExistsException("ESP", "INTERNAL", "EMP001", "NATIONAL_ID"));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/identifiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifierTypeCode": "NATIONAL_ID",
                                  "identifierValue": "12345678A"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Identifier already exists")));
    }

    @Test
    void deleteMapsDomainNotFoundToHttp404() throws Exception {
        org.mockito.Mockito.doThrow(new IdentifierNotFoundException("ESP", "INTERNAL", "EMP001", "NATIONAL_ID"))
                .when(deleteIdentifierUseCase)
                .delete(any());

        mockMvc.perform(delete("/employees/ESP/INTERNAL/EMP001/identifiers/NATIONAL_ID"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Identifier not found")));
    }

    private Identifier identifier(Long employeeId, String identifierTypeCode, String identifierValue, boolean isPrimary) {
        return new Identifier(
                1L,
                employeeId,
                identifierTypeCode,
                identifierValue,
                "ESP",
                null,
                isPrimary,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ADR-052 §4 (backend#24): el idioma entra por Accept-Language y llega al resolutor desde el ensamblador.
    @Test
    void listServesTheIdentifierTypeInTheLanguageOfTheAcceptLanguageHeader() throws Exception {
        when(listEmployeeIdentifiersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(identifier(10L, "NATIONAL_ID", "12345678A", true)));
        when(ruleEntityLabelResolver.resolveName("ESP", "EMPLOYEE_IDENTIFIER_TYPE", "NATIONAL_ID", "es-ES"))
                .thenReturn(Optional.of("DNI"));

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/identifiers")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].identifierTypeName").value("DNI"));
    }
}
