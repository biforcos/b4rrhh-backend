package com.b4rrhh.rulesystem.workcenter.infrastructure.web;

import com.b4rrhh.rulesystem.workcenter.application.usecase.CreateWorkCenterCommand;
import com.b4rrhh.rulesystem.workcenter.application.usecase.CreateWorkCenterContactCommand;
import com.b4rrhh.rulesystem.workcenter.application.usecase.CreateWorkCenterContactUseCase;
import com.b4rrhh.rulesystem.workcenter.application.usecase.CreateWorkCenterUseCase;
import com.b4rrhh.rulesystem.workcenter.application.usecase.DeleteWorkCenterContactCommand;
import com.b4rrhh.rulesystem.workcenter.application.usecase.DeleteWorkCenterContactUseCase;
import com.b4rrhh.rulesystem.workcenter.application.usecase.GetWorkCenterContactQuery;
import com.b4rrhh.rulesystem.workcenter.application.usecase.GetWorkCenterContactUseCase;
import com.b4rrhh.rulesystem.workcenter.application.usecase.GetWorkCenterQuery;
import com.b4rrhh.rulesystem.workcenter.application.usecase.GetWorkCenterUseCase;
import com.b4rrhh.rulesystem.workcenter.application.usecase.ListWorkCenterContactsUseCase;
import com.b4rrhh.rulesystem.workcenter.application.usecase.ListWorkCentersQuery;
import com.b4rrhh.rulesystem.workcenter.application.usecase.ListWorkCentersUseCase;
import com.b4rrhh.rulesystem.workcenter.application.usecase.UpdateWorkCenterCommand;
import com.b4rrhh.rulesystem.workcenter.application.usecase.UpdateWorkCenterContactCommand;
import com.b4rrhh.rulesystem.workcenter.application.usecase.UpdateWorkCenterContactUseCase;
import com.b4rrhh.rulesystem.workcenter.application.usecase.UpdateWorkCenterUseCase;
import com.b4rrhh.rulesystem.workcenter.application.view.WorkCenterDetails;
import com.b4rrhh.rulesystem.workcenter.domain.exception.WorkCenterNotFoundException;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenter;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterAddress;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterContact;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterProfile;
import com.b4rrhh.rulesystem.workcenter.infrastructure.web.assembler.WorkCenterResponseAssembler;
import com.b4rrhh.rulesystem.workcenter.infrastructure.web.mapper.WorkCenterCommandMapper;
import com.b4rrhh.rulesystem.workcenter.infrastructure.web.mapper.WorkCenterContactCommandMapper;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguageArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WorkCenterControllerHttpTest {

    @Mock
    private CreateWorkCenterUseCase createWorkCenterUseCase;
    @Mock
    private ListWorkCentersUseCase listWorkCentersUseCase;
    @Mock
    private GetWorkCenterUseCase getWorkCenterUseCase;
    @Mock
    private UpdateWorkCenterUseCase updateWorkCenterUseCase;
    @Mock
    private CreateWorkCenterContactUseCase createWorkCenterContactUseCase;
    @Mock
    private UpdateWorkCenterContactUseCase updateWorkCenterContactUseCase;
    @Mock
    private DeleteWorkCenterContactUseCase deleteWorkCenterContactUseCase;
    @Mock
    private GetWorkCenterContactUseCase getWorkCenterContactUseCase;
    @Mock
    private ListWorkCenterContactsUseCase listWorkCenterContactsUseCase;
    @Mock
    private RuleEntityLabelResolver ruleEntityLabelResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        WorkCenterResponseAssembler assembler = new WorkCenterResponseAssembler(ruleEntityLabelResolver);

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new WorkCenterController(
                                createWorkCenterUseCase,
                                listWorkCentersUseCase,
                                getWorkCenterUseCase,
                                updateWorkCenterUseCase,
                                new WorkCenterCommandMapper(),
                                assembler
                        ),
                        new WorkCenterContactController(
                                createWorkCenterContactUseCase,
                                updateWorkCenterContactUseCase,
                                deleteWorkCenterContactUseCase,
                                getWorkCenterContactUseCase,
                                listWorkCenterContactsUseCase,
                                new WorkCenterContactCommandMapper(),
                                assembler
                        )
                )
                .setControllerAdvice(new WorkCenterExceptionHandler())
                .setCustomArgumentResolvers(new ResponseLanguageArgumentResolver())
                .build();
    }

    @Test
    void postCreatesAggregatedWorkCenter() throws Exception {
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class))).thenReturn(workCenter());

        mockMvc.perform(post("/work-centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "workCenterCode": "MADRID-HQ",
                                  "name": "Madrid HQ",
                                  "description": "Headquarters",
                                  "startDate": "2026-01-01",
                                  "companyCode": "ACME",
                                  "address": {
                                    "street": "Gran Via 1",
                                    "city": "Madrid",
                                    "postalCode": "28013",
                                    "regionCode": "MD",
                                    "countryCode": "ESP"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workCenterCode").value("MADRID-HQ"))
                .andExpect(jsonPath("$.companyCode").value("ACME"));

        ArgumentCaptor<CreateWorkCenterCommand> captor = ArgumentCaptor.forClass(CreateWorkCenterCommand.class);
        verify(createWorkCenterUseCase).create(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("MADRID-HQ", captor.getValue().workCenterCode());
    }

    @Test
    void getListReturnsAggregatedWorkCenters() throws Exception {
        when(listWorkCentersUseCase.list(any(ListWorkCentersQuery.class))).thenReturn(List.of(workCenter()));

        mockMvc.perform(get("/work-centers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workCenterCode").value("MADRID-HQ"))
                .andExpect(jsonPath("$[0].countryCode").value("ESP"));
    }

    @Test
    void getByBusinessKeyReturnsAggregatedWorkCenter() throws Exception {
        when(getWorkCenterUseCase.get(any(GetWorkCenterQuery.class))).thenReturn(workCenter());

        mockMvc.perform(get("/work-centers/ESP/MADRID-HQ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Madrid HQ"))
                .andExpect(jsonPath("$.companyCode").value("ACME"));
    }

    @Test
    void putUpdatesAggregatedWorkCenterWithoutTouchingContacts() throws Exception {
        when(updateWorkCenterUseCase.update(any(UpdateWorkCenterCommand.class))).thenReturn(workCenter());

        mockMvc.perform(put("/work-centers/ESP/MADRID-HQ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Madrid HQ",
                                  "description": "Headquarters",
                                  "companyCode": "ACME",
                                  "address": {
                                    "street": "Gran Via 1",
                                    "city": "Madrid",
                                    "postalCode": "28013",
                                    "regionCode": "MD",
                                    "countryCode": "ESP"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address.city").value("Madrid"));
    }

    @Test
    void contactEndpointsUseContactNumberAsBusinessKey() throws Exception {
        WorkCenterContact contact = new WorkCenterContact(1, "EMAIL", "hq@example.com");
        when(ruleEntityLabelResolver.resolveName("ESP", "CONTACT_TYPE", "EMAIL", null))
                .thenReturn(Optional.of("Email"));
        when(createWorkCenterContactUseCase.create(any(CreateWorkCenterContactCommand.class))).thenReturn(contact);
        when(listWorkCenterContactsUseCase.list("ESP", "MADRID-HQ")).thenReturn(List.of(contact));
        when(getWorkCenterContactUseCase.get(any(GetWorkCenterContactQuery.class))).thenReturn(contact);
        when(updateWorkCenterContactUseCase.update(any(UpdateWorkCenterContactCommand.class))).thenReturn(contact);

        mockMvc.perform(post("/work-centers/ESP/MADRID-HQ/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contactTypeCode": "EMAIL",
                                  "contactValue": "hq@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contactNumber").value(1));

        mockMvc.perform(get("/work-centers/ESP/MADRID-HQ/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contactTypeCode").value("EMAIL"));

        mockMvc.perform(get("/work-centers/ESP/MADRID-HQ/contacts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactValue").value("hq@example.com"));

        mockMvc.perform(put("/work-centers/ESP/MADRID-HQ/contacts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contactTypeCode": "EMAIL",
                                  "contactValue": "hq@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactTypeName").value("Email"));

        mockMvc.perform(delete("/work-centers/ESP/MADRID-HQ/contacts/1"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<DeleteWorkCenterContactCommand> deleteCaptor = ArgumentCaptor.forClass(DeleteWorkCenterContactCommand.class);
        verify(deleteWorkCenterContactUseCase).delete(deleteCaptor.capture());
        assertEquals(1, deleteCaptor.getValue().contactNumber());
    }

    @Test
    void getReturns404WhenWorkCenterNotFound() throws Exception {
        when(getWorkCenterUseCase.get(any(GetWorkCenterQuery.class)))
                .thenThrow(new WorkCenterNotFoundException("ESP", "MADRID-HQ"));

        mockMvc.perform(get("/work-centers/ESP/MADRID-HQ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Work center not found")));
    }

    private WorkCenterDetails workCenter() {
        return new WorkCenterDetails(
                new WorkCenter(
                        "ESP",
                        "MADRID-HQ",
                        "Madrid HQ",
                        "Headquarters",
                        LocalDate.of(2026, 1, 1),
                        null,
                        true
                ),
                new WorkCenterProfile(
                        "ACME",
                        new WorkCenterAddress("Gran Via 1", "Madrid", "28013", "MD", "ESP")
                )
        );
    }
}