package com.b4rrhh.employee.workcenter.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguageArgumentResolver;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterCompanyLookupPort;
import com.b4rrhh.employee.workcenter.application.usecase.CloseWorkCenterCommand;
import com.b4rrhh.employee.workcenter.application.usecase.CloseWorkCenterUseCase;
import com.b4rrhh.employee.workcenter.application.usecase.CreateWorkCenterCommand;
import com.b4rrhh.employee.workcenter.application.usecase.CreateWorkCenterUseCase;
import com.b4rrhh.employee.workcenter.application.usecase.DeleteWorkCenterCommand;
import com.b4rrhh.employee.workcenter.application.usecase.DeleteWorkCenterUseCase;
import com.b4rrhh.employee.workcenter.application.usecase.GetWorkCenterByBusinessKeyUseCase;
import com.b4rrhh.employee.workcenter.application.usecase.ListEmployeeWorkCentersUseCase;
import com.b4rrhh.employee.workcenter.application.usecase.ReplaceWorkCenterFromDateCommand;
import com.b4rrhh.employee.workcenter.application.usecase.ReplaceWorkCenterFromDateUseCase;
import com.b4rrhh.employee.workcenter.application.usecase.UpdateWorkCenterCommand;
import com.b4rrhh.employee.workcenter.application.usecase.UpdateWorkCenterUseCase;
import com.b4rrhh.employee.workcenter.domain.exception.InvalidWorkCenterDateRangeException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterAlreadyClosedException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCatalogValueInvalidException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCompanyMismatchException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterDeleteForbiddenAtPresenceStartException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOverlapException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.infrastructure.web.assembler.WorkCenterResponseAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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

        private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CreateWorkCenterUseCase createWorkCenterUseCase;
    @Mock
    private CloseWorkCenterUseCase closeWorkCenterUseCase;
    @Mock
        private DeleteWorkCenterUseCase deleteWorkCenterUseCase;
        @Mock
    private GetWorkCenterByBusinessKeyUseCase getWorkCenterByBusinessKeyUseCase;
    @Mock
    private ListEmployeeWorkCentersUseCase listEmployeeWorkCentersUseCase;
        @Mock
        private ReplaceWorkCenterFromDateUseCase replaceWorkCenterFromDateUseCase;
        @Mock
        private UpdateWorkCenterUseCase updateWorkCenterUseCase;
        @Mock
        private RuleEntityLabelResolver ruleEntityLabelResolver;
    @Mock
    private WorkCenterCompanyLookupPort workCenterCompanyLookupPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        WorkCenterResponseAssembler workCenterResponseAssembler =
                new WorkCenterResponseAssembler(ruleEntityLabelResolver, workCenterCompanyLookupPort);

        WorkCenterController controller = new WorkCenterController(
                createWorkCenterUseCase,
                closeWorkCenterUseCase,
                deleteWorkCenterUseCase,
                getWorkCenterByBusinessKeyUseCase,
                listEmployeeWorkCentersUseCase,
                replaceWorkCenterFromDateUseCase,
                updateWorkCenterUseCase,
                workCenterResponseAssembler
        );

        lenient().when(ruleEntityLabelResolver.resolveName(anyString(), eq("WORK_CENTER"), anyString(), any()))
                .thenReturn(Optional.empty());
        lenient().when(workCenterCompanyLookupPort.findCompanyCode(anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        lenient().when(ruleEntityLabelResolver.resolveName(anyString(), eq("COMPANY"), anyString(), any()))
                .thenReturn(Optional.empty());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new WorkCenterExceptionHandler())
                .setCustomArgumentResolvers(new ResponseLanguageArgumentResolver())
                .build();
    }

    @Test
    void createMapsPathAndBodyToCommand() throws Exception {
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class)))
                .thenReturn(workCenter(1, "MADRID_HQ", LocalDate.of(2026, 1, 10), null));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workCenterCode": "MADRID_HQ",
                                  "startDate": "2026-01-10"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workCenterAssignmentNumber").value(1))
                .andExpect(jsonPath("$.workCenterCode").value("MADRID_HQ"));

        ArgumentCaptor<CreateWorkCenterCommand> captor = ArgumentCaptor.forClass(CreateWorkCenterCommand.class);
        verify(createWorkCenterUseCase).create(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals("MADRID_HQ", captor.getValue().workCenterCode());
    }

    @Test
    void replaceFromDateMapsPathAndBodyToCommand() throws Exception {
        when(replaceWorkCenterFromDateUseCase.replaceFromDate(any(ReplaceWorkCenterFromDateCommand.class)))
                .thenReturn(workCenter(2, "BARCELONA_HQ", LocalDate.of(2026, 3, 1), null));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers/replace-from-date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "effectiveDate": "2026-03-01",
                                  "workCenterCode": "BARCELONA_HQ"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workCenterAssignmentNumber").value(2))
                .andExpect(jsonPath("$.workCenterCode").value("BARCELONA_HQ"));

        ArgumentCaptor<ReplaceWorkCenterFromDateCommand> captor =
                ArgumentCaptor.forClass(ReplaceWorkCenterFromDateCommand.class);
        verify(replaceWorkCenterFromDateUseCase).replaceFromDate(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals(LocalDate.of(2026, 3, 1), captor.getValue().effectiveDate());
        assertEquals("BARCELONA_HQ", captor.getValue().workCenterCode());
    }

    @Test
    void closeMapsPathAndBodyToCommand() throws Exception {
        when(closeWorkCenterUseCase.close(any(CloseWorkCenterCommand.class)))
                .thenReturn(workCenter(1, "MADRID_HQ", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20)));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endDate": "2026-01-20"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workCenterAssignmentNumber").value(1))
                .andExpect(jsonPath("$.endDate[0]").value(2026))
                .andExpect(jsonPath("$.endDate[1]").value(1))
                .andExpect(jsonPath("$.endDate[2]").value(20));

        ArgumentCaptor<CloseWorkCenterCommand> captor = ArgumentCaptor.forClass(CloseWorkCenterCommand.class);
        verify(closeWorkCenterUseCase).close(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals(1, captor.getValue().workCenterAssignmentNumber());
    }

    @Test
    void createMapsDomainConflictToHttp409() throws Exception {
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class)))
                .thenThrow(new WorkCenterOverlapException("ESP", "INTERNAL", "EMP001"));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workCenterCode": "MADRID_HQ",
                                  "startDate": "2026-01-10"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_OVERLAP"))
                .andExpect(jsonPath("$.message", containsString("solapa")));
    }

    @Test
    void createMapsCatalogNotFoundToHttp404() throws Exception {
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class)))
                .thenThrow(new WorkCenterCatalogValueInvalidException("workCenterCode", "UNKNOWN"));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workCenterCode": "UNKNOWN",
                                  "startDate": "2026-01-10"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_CATALOG_NOT_FOUND"))
                .andExpect(jsonPath("$.details.field").value("workCenterCode"));
    }

    @Test
    void replaceFromDateMapsCatalogNotFoundToHttp404() throws Exception {
        when(replaceWorkCenterFromDateUseCase.replaceFromDate(any(ReplaceWorkCenterFromDateCommand.class)))
                .thenThrow(new WorkCenterCatalogValueInvalidException("workCenterCode", "UNKNOWN"));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers/replace-from-date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "effectiveDate": "2026-03-01",
                                  "workCenterCode": "UNKNOWN"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_CATALOG_NOT_FOUND"));
    }

    @Test
    void createMapsOutsidePresenceToHttp409() throws Exception {
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class)))
                .thenThrow(new WorkCenterOutsidePresencePeriodException(
                        "ESP",
                        "INTERNAL",
                        "EMP001",
                        LocalDate.of(2026, 1, 10),
                        null
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workCenterCode": "MADRID_HQ",
                                  "startDate": "2026-01-10"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_OUTSIDE_PRESENCE"));
    }

    @Test
    void createMapsCompanyMismatchToHttp409() throws Exception {
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class)))
                .thenThrow(new WorkCenterCompanyMismatchException("MADRID_HQ", "COMP"));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workCenterCode": "MADRID_HQ",
                                  "startDate": "2026-01-10"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_COMPANY_MISMATCH"));
    }

    @Test
    void closeMapsAlreadyClosedToHttp409() throws Exception {
        when(closeWorkCenterUseCase.close(any(CloseWorkCenterCommand.class)))
                .thenThrow(new WorkCenterAlreadyClosedException(1));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endDate": "2026-01-20"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_ALREADY_CLOSED"));
    }

    @Test
    void updateMapsInvalidPeriodToHttp409() throws Exception {
        when(updateWorkCenterUseCase.update(any(UpdateWorkCenterCommand.class)))
                .thenThrow(new InvalidWorkCenterDateRangeException("endDate must be greater than or equal to startDate"));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/work-centers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workCenterCode": "BARCELONA_HQ",
                                  "startDate": "2026-02-21",
                                  "endDate": "2026-02-20"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_INVALID_PERIOD"));
    }

    @Test
    void updateMapsPathAndBodyToCommand() throws Exception {
        when(updateWorkCenterUseCase.update(any(UpdateWorkCenterCommand.class)))
                .thenReturn(workCenter(1, "BARCELONA_HQ", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 20)));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/work-centers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workCenterCode": "BARCELONA_HQ",
                                  "startDate": "2026-02-01",
                                  "endDate": "2026-02-20"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workCenterAssignmentNumber").value(1))
                .andExpect(jsonPath("$.workCenterCode").value("BARCELONA_HQ"));

        ArgumentCaptor<UpdateWorkCenterCommand> captor = ArgumentCaptor.forClass(UpdateWorkCenterCommand.class);
        verify(updateWorkCenterUseCase).update(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals(1, captor.getValue().workCenterAssignmentNumber());
    }

    @Test
    void updateMapsNotFoundToHttp404() throws Exception {
        when(updateWorkCenterUseCase.update(any(UpdateWorkCenterCommand.class)))
                .thenThrow(new WorkCenterNotFoundException("ESP", "INTERNAL", "EMP001", 99));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/work-centers/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workCenterCode": "BARCELONA_HQ",
                                  "startDate": "2026-02-01",
                                  "endDate": "2026-02-20"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_NOT_FOUND"));
    }

    @Test
    void updateMapsOverlapToHttp409() throws Exception {
        when(updateWorkCenterUseCase.update(any(UpdateWorkCenterCommand.class)))
                .thenThrow(new WorkCenterOverlapException("ESP", "INTERNAL", "EMP001"));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/work-centers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workCenterCode": "BARCELONA_HQ",
                                  "startDate": "2026-02-01",
                                  "endDate": "2026-02-20"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_OVERLAP"));
    }

    @Test
    void deleteMapsPathToCommandAndReturnsHttp204() throws Exception {
        doNothing().when(deleteWorkCenterUseCase).delete(any(DeleteWorkCenterCommand.class));

        mockMvc.perform(delete("/employees/ESP/INTERNAL/EMP001/work-centers/1"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<DeleteWorkCenterCommand> captor = ArgumentCaptor.forClass(DeleteWorkCenterCommand.class);
        verify(deleteWorkCenterUseCase).delete(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals(1, captor.getValue().workCenterAssignmentNumber());
    }

    @Test
    void deleteMapsNotFoundToHttp404() throws Exception {
        doThrow(new WorkCenterNotFoundException("ESP", "INTERNAL", "EMP001", 99))
                .when(deleteWorkCenterUseCase)
                .delete(any(DeleteWorkCenterCommand.class));

        mockMvc.perform(delete("/employees/ESP/INTERNAL/EMP001/work-centers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_NOT_FOUND"));
    }

    @Test
    void deleteMapsPresenceStartConflictToHttp409() throws Exception {
        doThrow(new WorkCenterDeleteForbiddenAtPresenceStartException(
                "ESP",
                "INTERNAL",
                "EMP001",
                1,
                LocalDate.of(2026, 1, 10)
        )).when(deleteWorkCenterUseCase).delete(any(DeleteWorkCenterCommand.class));

        mockMvc.perform(delete("/employees/ESP/INTERNAL/EMP001/work-centers/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_DELETE_FORBIDDEN_AT_PRESENCE_START"))
                .andExpect(jsonPath("$.message").value("La asignación no puede eliminarse porque inicia una presence del empleado. Corrígela si necesitas cambiarla."));
    }

    @Test
    void getReturnsWorkCenterWithResolvedLabel() throws Exception {
        when(getWorkCenterByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001", 1))
                .thenReturn(Optional.of(workCenter(1, "MADRID_HQ", LocalDate.of(2026, 1, 10), null)));
        when(ruleEntityLabelResolver.resolveName("ESP", "WORK_CENTER", "MADRID_HQ", null))
                .thenReturn(Optional.of("Oficina central"));
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_HQ", LocalDate.of(2026, 1, 10)))
                .thenReturn(Optional.of("COMP"));
        when(ruleEntityLabelResolver.resolveName("ESP", "COMPANY", "COMP", null))
                .thenReturn(Optional.of("Compañía principal"));

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/work-centers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workCenterCode").value("MADRID_HQ"))
                .andExpect(jsonPath("$.workCenterName").value("Oficina central"))
                .andExpect(jsonPath("$.companyCode").value("COMP"))
                .andExpect(jsonPath("$.companyName").value("Compañía principal"))
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void listReturnsWorkCenterWithNullLabelWhenCatalogEntryIsMissing() throws Exception {
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(workCenter(1, "MADRID_HQ", LocalDate.of(2026, 1, 10), null)));
        when(ruleEntityLabelResolver.resolveName("ESP", "WORK_CENTER", "MADRID_HQ", null))
                .thenReturn(Optional.empty());

        MvcResult result = mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/work-centers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workCenterCode").value("MADRID_HQ"))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode first = body.path(0);
        assertTrue(!first.has("workCenterName") || first.get("workCenterName").isNull());
    }

    private WorkCenter workCenter(
            int assignmentNumber,
            String workCenterCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new WorkCenter(
                1L,
                10L,
                assignmentNumber,
                workCenterCode,
                startDate,
                endDate,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ADR-052 §4 (backend#24): el idioma entra por Accept-Language y llega al resolutor desde el ensamblador.
    @Test
    void listServesWorkCenterAndCompanyInTheLanguageOfTheAcceptLanguageHeader() throws Exception {
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(workCenter(1, "MADRID_HQ", LocalDate.of(2026, 1, 10), null)));
        when(ruleEntityLabelResolver.resolveName("ESP", "WORK_CENTER", "MADRID_HQ", "es-ES"))
                .thenReturn(Optional.of("Oficina central"));
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_HQ", LocalDate.of(2026, 1, 10)))
                .thenReturn(Optional.of("COMP"));
        when(ruleEntityLabelResolver.resolveName("ESP", "COMPANY", "COMP", "es-ES"))
                .thenReturn(Optional.of("Compañía principal"));

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/work-centers")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workCenterName").value("Oficina central"))
                .andExpect(jsonPath("$[0].companyName").value("Compañía principal"));
    }
}
