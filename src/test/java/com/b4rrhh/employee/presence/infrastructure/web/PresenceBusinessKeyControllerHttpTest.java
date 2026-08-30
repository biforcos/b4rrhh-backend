package com.b4rrhh.employee.presence.infrastructure.web;

import com.b4rrhh.employee.presence.application.usecase.ClosePresenceUseCase;
import com.b4rrhh.employee.presence.application.usecase.CreatePresenceCommand;
import com.b4rrhh.employee.presence.application.usecase.CreatePresenceUseCase;
import com.b4rrhh.employee.presence.application.usecase.GetPresenceByBusinessKeyUseCase;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.presence.infrastructure.web.assembler.PresenceResponseAssembler;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguageArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PresenceBusinessKeyControllerHttpTest {

    @Mock
    private CreatePresenceUseCase createPresenceUseCase;
    @Mock
    private ClosePresenceUseCase closePresenceUseCase;
    @Mock
    private GetPresenceByBusinessKeyUseCase getPresenceByBusinessKeyUseCase;
    @Mock
    private ListEmployeePresencesUseCase listEmployeePresencesUseCase;
    @Mock
    private RuleEntityLabelResolver ruleEntityLabelResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PresenceBusinessKeyController controller = new PresenceBusinessKeyController(
                createPresenceUseCase,
                closePresenceUseCase,
                getPresenceByBusinessKeyUseCase,
                listEmployeePresencesUseCase,
                new PresenceResponseAssembler(ruleEntityLabelResolver)
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PresenceExceptionHandler())
                .setCustomArgumentResolvers(new ResponseLanguageArgumentResolver())
                .build();
    }

    @Test
    void createReturnsCompanyNameWhenCatalogLabelExists() throws Exception {
        when(ruleEntityLabelResolver.resolveName("ESP", "COMPANY", "AC01", null))
                .thenReturn(Optional.of("Empresa Activa"));
        when(ruleEntityLabelResolver.resolveName("ESP", "EMPLOYEE_PRESENCE_ENTRY_REASON", "ENT01", null))
                .thenReturn(Optional.of("Alta inicial"));
        when(createPresenceUseCase.create(any(CreatePresenceCommand.class)))
                .thenReturn(activePresence());

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyCode": "AC01",
                                  "entryReasonCode": "ENT01",
                                  "startDate": "2026-01-10"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.presenceNumber").value(1))
                .andExpect(jsonPath("$.companyCode").value("AC01"))
                .andExpect(jsonPath("$.companyName").value("Empresa Activa"))
                .andExpect(jsonPath("$.entryReasonCode").value("ENT01"))
                .andExpect(jsonPath("$.entryReasonName").value("Alta inicial"))
                .andExpect(jsonPath("$.exitReasonName").isEmpty())
                .andExpect(jsonPath("$.employeeId").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void createReturnsNullCompanyNameWhenCatalogLabelIsMissing() throws Exception {
        when(ruleEntityLabelResolver.resolveName("ESP", "COMPANY", "AC01", null))
                .thenReturn(Optional.empty());
        when(ruleEntityLabelResolver.resolveName("ESP", "EMPLOYEE_PRESENCE_ENTRY_REASON", "ENT01", null))
                .thenReturn(Optional.empty());
        when(createPresenceUseCase.create(any(CreatePresenceCommand.class)))
                .thenReturn(activePresence());

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyCode": "AC01",
                                  "entryReasonCode": "ENT01",
                                  "startDate": "2026-01-10"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyCode").value("AC01"))
                .andExpect(jsonPath("$.companyName").isEmpty())
                .andExpect(jsonPath("$.entryReasonName").isEmpty())
                .andExpect(jsonPath("$.exitReasonName").isEmpty());
    }

    // ADR-052 §4: el idioma entra por Accept-Language y llega al resolutor desde la capa web.
    @Test
    void listResolvesLabelsInTheLanguageOfTheAcceptLanguageHeader() throws Exception {
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(activePresence()));
        lenient().when(ruleEntityLabelResolver.resolveName("ESP", "EMPLOYEE_PRESENCE_ENTRY_REASON", "ENT01", "es-ES"))
                .thenReturn(Optional.of("Contratación"));

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/presences")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES,es;q=0.9,en;q=0.8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entryReasonName").value("Contratación"));
    }

    @Test
    void listAsksForTheBaseLiteralWhenThereIsNoAcceptLanguageHeader() throws Exception {
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(activePresence()));
        lenient().when(ruleEntityLabelResolver.resolveName("ESP", "EMPLOYEE_PRESENCE_ENTRY_REASON", "ENT01", null))
                .thenReturn(Optional.of("Hiring"));

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/presences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entryReasonName").value("Hiring"));
    }

    private Presence activePresence() {
        return new Presence(
                20L,
                10L,
                1,
                "AC01",
                "ENT01",
                null,
                LocalDate.of(2026, 1, 10),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
