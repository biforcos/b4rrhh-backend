package com.b4rrhh.employee.workcenter.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguageArgumentResolver;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterCompanyLookupPort;
import com.b4rrhh.employee.workcenter.application.usecase.CreateWorkCenterCommand;
import com.b4rrhh.employee.workcenter.application.usecase.CreateWorkCenterUseCase;
import com.b4rrhh.employee.workcenter.application.usecase.DeleteWorkCenterCommand;
import com.b4rrhh.employee.workcenter.application.usecase.DeleteWorkCenterUseCase;
import com.b4rrhh.employee.workcenter.application.usecase.GetWorkCenterByBusinessKeyUseCase;
import com.b4rrhh.employee.workcenter.application.usecase.ListEmployeeWorkCentersUseCase;
import com.b4rrhh.employee.workcenter.application.usecase.PlanWorkCenterChangeCommand;
import com.b4rrhh.employee.workcenter.application.usecase.PlanWorkCenterChangeUseCase;
import com.b4rrhh.employee.workcenter.application.model.WorkCenterPlan;
import com.b4rrhh.employee.workcenter.application.model.WorkCenterPlanAdjustment;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import com.b4rrhh.employee.workcenter.application.usecase.UpdateWorkCenterCommand;
import com.b4rrhh.employee.workcenter.application.usecase.UpdateWorkCenterUseCase;
import com.b4rrhh.employee.workcenter.domain.exception.InvalidWorkCenterDateRangeException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterAlreadyClosedException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCatalogValueInvalidException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCompanyMismatchException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterIsACorrectionException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOverlapException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterPresenceCoverageGapException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterOccurrence;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterPeriod;
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
        private DeleteWorkCenterUseCase deleteWorkCenterUseCase;
        @Mock
    private GetWorkCenterByBusinessKeyUseCase getWorkCenterByBusinessKeyUseCase;
    @Mock
    private ListEmployeeWorkCentersUseCase listEmployeeWorkCentersUseCase;
        @Mock
        private UpdateWorkCenterUseCase updateWorkCenterUseCase;
    @Mock
    private PlanWorkCenterChangeUseCase planWorkCenterChangeUseCase;
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
                deleteWorkCenterUseCase,
                getWorkCenterByBusinessKeyUseCase,
                listEmployeeWorkCentersUseCase,
                updateWorkCenterUseCase,
                planWorkCenterChangeUseCase,
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

    // ADR-057: the rejection names what it ran into, so the screen can show it.
    @Test
    void createMapsAnOverlapToHttp409NamingTheSharedDates() throws Exception {
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class)))
                .thenThrow(new WorkCenterOverlapException(
                        "ESP", "INTERNAL", "EMP001",
                        LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 10),
                        List.of(new WorkCenterPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10)))
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workCenterCode": "MADRID_HQ",
                                  "startDate": "2026-01-15",
                                  "endDate": "2026-02-10"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_OVERLAP"))
                .andExpect(jsonPath("$.details.overlaps[0].startDate[0]").value(2026))
                .andExpect(jsonPath("$.details.overlaps[0].startDate[1]").value(2))
                .andExpect(jsonPath("$.details.overlaps[0].startDate[2]").value(1))
                .andExpect(jsonPath("$.details.overlaps[0].endDate[2]").value(10));
    }

    @Test
    void createMapsACoverageGapToHttp409SayingWhichGapAndWhatToStretch() throws Exception {
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class)))
                .thenThrow(new WorkCenterPresenceCoverageGapException(
                        "ESP", "INTERNAL", "EMP001",
                        List.of(new WorkCenterPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))),
                        List.of(new WorkCenterOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workCenterCode": "MADRID_HQ",
                                  "startDate": "2026-03-01"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_COVERAGE_GAP"))
                .andExpect(jsonPath("$.details.gaps[0].startDate[1]").value(2))
                .andExpect(jsonPath("$.details.gaps[0].endDate[2]").value(28))
                .andExpect(jsonPath("$.details.stretchCandidates[0].workCenterAssignmentNumber").value(1));
    }

    @Test
    void createMapsACorrectionAskedForAsAnAddToHttp409NamingTheAssignmentToCorrect() throws Exception {
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class)))
                .thenThrow(new WorkCenterIsACorrectionException(
                        "ESP", "INTERNAL", "EMP001",
                        new WorkCenterOccurrence(1, LocalDate.of(2026, 1, 10), null),
                        new WorkCenterPeriod(LocalDate.of(2026, 1, 10), null)
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workCenterCode": "BARCELONA_HQ",
                                  "startDate": "2026-01-10"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_IS_A_CORRECTION"))
                .andExpect(jsonPath("$.message", containsString("corrige")))
                .andExpect(jsonPath("$.details.correctedOccurrence.workCenterAssignmentNumber").value(1))
                .andExpect(jsonPath("$.details.correctedOccurrence.startDate[2]").value(10));
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

    // ADR-057 §3: the delete is bounded by the gap invariant. The assignment that starts the
    // presence was the old special case; now it is one more gap, named with its neighbours.
    @Test
    void deleteMapsACoverageGapToHttp409NamingTheNeighboursToStretch() throws Exception {
        doThrow(new WorkCenterPresenceCoverageGapException(
                "ESP", "INTERNAL", "EMP001",
                List.of(new WorkCenterPeriod(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 31))),
                List.of(new WorkCenterOccurrence(2, LocalDate.of(2026, 2, 1), null))
        )).when(deleteWorkCenterUseCase).delete(any(DeleteWorkCenterCommand.class));

        mockMvc.perform(delete("/employees/ESP/INTERNAL/EMP001/work-centers/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_COVERAGE_GAP"))
                .andExpect(jsonPath("$.details.gaps[0].startDate[2]").value(10))
                .andExpect(jsonPath("$.details.stretchCandidates[0].workCenterAssignmentNumber").value(2));
    }

    // ADR-057 §6: the plan is asked for and shown; nothing is applied.
    @Test
    void planMapsTheRequestToTheCommandAndReturnsThePlanWithoutApplyingIt() throws Exception {
        when(planWorkCenterChangeUseCase.plan(any(PlanWorkCenterChangeCommand.class)))
                .thenReturn(new WorkCenterPlan(
                        TimelineOperation.ADD,
                        null,
                        new WorkCenterOccurrence(null, LocalDate.of(2026, 2, 1), null),
                        null,
                        new WorkCenterPlanAdjustment(
                                1,
                                new WorkCenterPeriod(LocalDate.of(2026, 1, 1), null),
                                new WorkCenterPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
                        ),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                new WorkCenterOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                                new WorkCenterOccurrence(null, LocalDate.of(2026, 2, 1), null)
                        )
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "ADD",
                                  "startDate": "2026-02-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("ADD"))
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.rejection").doesNotExist())
                .andExpect(jsonPath("$.adjustedOccurrence.workCenterAssignmentNumber").value(1))
                .andExpect(jsonPath("$.adjustedOccurrence.after.endDate[2]").value(31))
                .andExpect(jsonPath("$.projected.length()").value(2))
                .andExpect(jsonPath("$.projected[1].workCenterAssignmentNumber").doesNotExist());

        ArgumentCaptor<PlanWorkCenterChangeCommand> captor = ArgumentCaptor.forClass(PlanWorkCenterChangeCommand.class);
        verify(planWorkCenterChangeUseCase).plan(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals(TimelineOperation.ADD, captor.getValue().operation());
        assertEquals(LocalDate.of(2026, 2, 1), captor.getValue().startDate());
        verify(createWorkCenterUseCase, org.mockito.Mockito.never()).create(any());
    }

    @Test
    void planTellsTheScreenAnAddOnAnExistingStartDateIsACorrectionOfThatAssignment() throws Exception {
        when(planWorkCenterChangeUseCase.plan(any(PlanWorkCenterChangeCommand.class)))
                .thenReturn(new WorkCenterPlan(
                        TimelineOperation.CORRECT,
                        TimelineRejection.IS_A_CORRECTION,
                        new WorkCenterOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)),
                        new WorkCenterOccurrence(1, LocalDate.of(2026, 1, 1), null),
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new WorkCenterOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)))
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "ADD",
                                  "startDate": "2026-01-01",
                                  "endDate": "2026-01-15"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("CORRECT"))
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.rejection").value("IS_A_CORRECTION"))
                .andExpect(jsonPath("$.correctedOccurrence.workCenterAssignmentNumber").value(1))
                .andExpect(jsonPath("$.correctedOccurrence.endDate").doesNotExist());
    }

    @Test
    void planIdentifiesTheAssignmentToRemoveByItsNumber() throws Exception {
        when(planWorkCenterChangeUseCase.plan(any(PlanWorkCenterChangeCommand.class)))
                .thenReturn(new WorkCenterPlan(
                        TimelineOperation.REMOVE,
                        null,
                        new WorkCenterOccurrence(2, LocalDate.of(2026, 2, 1), null),
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new WorkCenterOccurrence(1, LocalDate.of(2026, 1, 1), null))
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "REMOVE",
                                  "workCenterAssignmentNumber": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("REMOVE"))
                .andExpect(jsonPath("$.occurrence.workCenterAssignmentNumber").value(2));

        ArgumentCaptor<PlanWorkCenterChangeCommand> captor = ArgumentCaptor.forClass(PlanWorkCenterChangeCommand.class);
        verify(planWorkCenterChangeUseCase).plan(captor.capture());
        assertEquals(TimelineOperation.REMOVE, captor.getValue().operation());
        assertEquals(2, captor.getValue().workCenterAssignmentNumber());
    }

    @Test
    void planRejectsAnUnknownOperation() throws Exception {
        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/work-centers/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "REPLACE"
                                }
                                """))
                .andExpect(status().isBadRequest());
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
