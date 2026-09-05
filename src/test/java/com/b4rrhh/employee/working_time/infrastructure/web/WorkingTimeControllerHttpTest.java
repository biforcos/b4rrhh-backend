package com.b4rrhh.employee.working_time.infrastructure.web;

import com.b4rrhh.employee.working_time.application.usecase.CloseWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CloseWorkingTimeUseCase;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import com.b4rrhh.employee.working_time.application.model.WorkingTimePlan;
import com.b4rrhh.employee.working_time.application.model.WorkingTimePlanAdjustment;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.DeleteWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.DeleteWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.GetWorkingTimeByBusinessKeyCommand;
import com.b4rrhh.employee.working_time.application.usecase.GetWorkingTimeByBusinessKeyUseCase;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesCommand;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesUseCase;
import com.b4rrhh.employee.working_time.application.usecase.PlanWorkingTimeChangeCommand;
import com.b4rrhh.employee.working_time.application.usecase.PlanWorkingTimeChangeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.UpdateWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.UpdateWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.service.StandardWorkingTimeDerivationPolicy;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeCoverageGapException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeIsACorrectionException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOverlapException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeDerivedHours;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeOccurrence;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimePeriod;
import com.b4rrhh.employee.working_time.infrastructure.web.assembler.WorkingTimeResponseAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WorkingTimeControllerHttpTest {

    @Mock
    private CreateWorkingTimeUseCase createWorkingTimeUseCase;
    @Mock
    private ListEmployeeWorkingTimesUseCase listEmployeeWorkingTimesUseCase;
    @Mock
    private GetWorkingTimeByBusinessKeyUseCase getWorkingTimeByBusinessKeyUseCase;
    @Mock
    private CloseWorkingTimeUseCase closeWorkingTimeUseCase;
    @Mock
    private UpdateWorkingTimeUseCase updateWorkingTimeUseCase;
    @Mock
    private DeleteWorkingTimeUseCase deleteWorkingTimeUseCase;
    @Mock
    private PlanWorkingTimeChangeUseCase planWorkingTimeChangeUseCase;

    private final StandardWorkingTimeDerivationPolicy derivationPolicy = new StandardWorkingTimeDerivationPolicy();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        WorkingTimeController controller = new WorkingTimeController(
                createWorkingTimeUseCase,
                listEmployeeWorkingTimesUseCase,
                getWorkingTimeByBusinessKeyUseCase,
                closeWorkingTimeUseCase,
                updateWorkingTimeUseCase,
                deleteWorkingTimeUseCase,
                planWorkingTimeChangeUseCase,
                new WorkingTimeResponseAssembler()
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new WorkingTimeExceptionHandler())
                .build();
    }

    @Test
    void createMapsPathAndBodyToCommand() throws Exception {
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class)))
                .thenReturn(workingTime(1, LocalDate.of(2026, 1, 10), null, new BigDecimal("50")));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/working-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-01-10",
                                  "workingTimePercentage": 50
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workingTimeNumber").value(1))
                .andExpect(jsonPath("$.workingTimePercentage").value(50))
                        .andExpect(jsonPath("$.weeklyHours").value(16.69))
                .andExpect(jsonPath("$.id").doesNotExist());

        ArgumentCaptor<CreateWorkingTimeCommand> captor = ArgumentCaptor.forClass(CreateWorkingTimeCommand.class);
        verify(createWorkingTimeUseCase).create(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals(LocalDate.of(2026, 1, 10), captor.getValue().startDate());
        assertNull(captor.getValue().endDate());
        assertEquals(new BigDecimal("50"), captor.getValue().workingTimePercentage());
    }

    @Test
    void createCarriesTheEndDateWhenGiven() throws Exception {
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class)))
                .thenReturn(workingTime(1, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 3, 31), new BigDecimal("50")));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/working-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-01-10",
                                  "endDate": "2026-03-31",
                                  "workingTimePercentage": 50
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.endDate[0]").value(2026))
                .andExpect(jsonPath("$.endDate[1]").value(3))
                .andExpect(jsonPath("$.endDate[2]").value(31));

        ArgumentCaptor<CreateWorkingTimeCommand> captor = ArgumentCaptor.forClass(CreateWorkingTimeCommand.class);
        verify(createWorkingTimeUseCase).create(captor.capture());
        assertEquals(LocalDate.of(2026, 3, 31), captor.getValue().endDate());
    }

    @Test
    void updateCarriesBothDatesAndThePercentage() throws Exception {
        when(updateWorkingTimeUseCase.update(any(UpdateWorkingTimeCommand.class)))
                .thenReturn(workingTime(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), new BigDecimal("60")));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/working-times/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-02-01",
                                  "endDate": "2026-02-28",
                                  "workingTimePercentage": 60
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workingTimeNumber").value(2));

        ArgumentCaptor<UpdateWorkingTimeCommand> captor = ArgumentCaptor.forClass(UpdateWorkingTimeCommand.class);
        verify(updateWorkingTimeUseCase).update(captor.capture());
        assertEquals(2, captor.getValue().workingTimeNumber());
        assertEquals(LocalDate.of(2026, 2, 1), captor.getValue().startDate());
        assertEquals(LocalDate.of(2026, 2, 28), captor.getValue().endDate());
        assertEquals(new BigDecimal("60"), captor.getValue().workingTimePercentage());
    }

    @Test
    void createMapsACoverageGapToHttp409SayingWhichGapAndWhatToStretch() throws Exception {
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class)))
                .thenThrow(new WorkingTimeCoverageGapException(
                        "ESP", "INTERNAL", "EMP001",
                        List.of(new WorkingTimePeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))),
                        List.of(
                                new WorkingTimeOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                                new WorkingTimeOccurrence(null, LocalDate.of(2026, 3, 1), null)
                        )
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/working-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-03-01",
                                  "workingTimePercentage": 50
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKING_TIME_COVERAGE_GAP"))
                .andExpect(jsonPath("$.message", containsString("sin cubrir")))
                .andExpect(jsonPath("$.details.gaps[0].startDate[1]").value(2))
                .andExpect(jsonPath("$.details.gaps[0].startDate[2]").value(1))
                .andExpect(jsonPath("$.details.gaps[0].endDate[2]").value(28))
                .andExpect(jsonPath("$.details.stretchCandidates[0].workingTimeNumber").value(1))
                .andExpect(jsonPath("$.details.stretchCandidates[1].workingTimeNumber").doesNotExist());
    }

    @Test
    void createRejectsDerivedHourFieldsInRequest() throws Exception {
        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/working-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-01-10",
                                  "workingTimePercentage": 50,
                                  "weeklyHours": 20
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listReturnsEmployeeWorkingTimesWithoutTechnicalIds() throws Exception {
        when(listEmployeeWorkingTimesUseCase.listByEmployeeBusinessKey(any(ListEmployeeWorkingTimesCommand.class)))
                .thenReturn(List.of(workingTime(1, LocalDate.of(2026, 1, 10), null, new BigDecimal("75"))));

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/working-times"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workingTimeNumber").value(1))
                        .andExpect(jsonPath("$[0].dailyHours").value(5.01))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    void getByBusinessKeyUsesFunctionalIdentity() throws Exception {
        when(getWorkingTimeByBusinessKeyUseCase.getByBusinessKey(any(GetWorkingTimeByBusinessKeyCommand.class)))
                .thenReturn(workingTime(3, LocalDate.of(2026, 2, 1), null, new BigDecimal("100")));

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/working-times/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workingTimeNumber").value(3))
                        .andExpect(jsonPath("$.monthlyHours").value(144.67));
    }

    @Test
    void closeMapsPathAndBodyToCommand() throws Exception {
        when(closeWorkingTimeUseCase.close(any(CloseWorkingTimeCommand.class)))
                .thenReturn(workingTime(1, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20), new BigDecimal("50")));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/working-times/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endDate": "2026-01-20"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workingTimeNumber").value(1))
                .andExpect(jsonPath("$.endDate[0]").value(2026))
                .andExpect(jsonPath("$.endDate[1]").value(1))
                .andExpect(jsonPath("$.endDate[2]").value(20));

        ArgumentCaptor<CloseWorkingTimeCommand> captor = ArgumentCaptor.forClass(CloseWorkingTimeCommand.class);
        verify(closeWorkingTimeUseCase).close(captor.capture());
        assertEquals(1, captor.getValue().workingTimeNumber());
        assertEquals(LocalDate.of(2026, 1, 20), captor.getValue().endDate());
    }

    @Test
    void createMapsOverlapToHttp409() throws Exception {
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class)))
                .thenThrow(new WorkingTimeOverlapException("ESP", "INTERNAL", "EMP001", LocalDate.of(2026, 1, 10), null));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/working-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-01-10",
                                  "workingTimePercentage": 50
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKING_TIME_OVERLAP"))
                .andExpect(jsonPath("$.message", containsString("solapa")));
    }

    @Test
    void deleteMapsPathToCommandAndAnswersNoContent() throws Exception {
        mockMvc.perform(delete("/employees/ESP/INTERNAL/EMP001/working-times/2"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<DeleteWorkingTimeCommand> captor = ArgumentCaptor.forClass(DeleteWorkingTimeCommand.class);
        verify(deleteWorkingTimeUseCase).delete(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals(2, captor.getValue().workingTimeNumber());
    }

    @Test
    void deleteMapsACoverageGapToHttp409NamingTheNeighboursToStretch() throws Exception {
        doThrow(new WorkingTimeCoverageGapException(
                "ESP", "INTERNAL", "EMP001",
                List.of(new WorkingTimePeriod(LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31))),
                List.of(
                        new WorkingTimeOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)),
                        new WorkingTimeOccurrence(3, LocalDate.of(2026, 2, 1), null)
                )
        )).when(deleteWorkingTimeUseCase).delete(any(DeleteWorkingTimeCommand.class));

        mockMvc.perform(delete("/employees/ESP/INTERNAL/EMP001/working-times/2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKING_TIME_COVERAGE_GAP"))
                .andExpect(jsonPath("$.details.stretchCandidates[0].workingTimeNumber").value(1))
                .andExpect(jsonPath("$.details.stretchCandidates[1].workingTimeNumber").value(3));
    }

    @Test
    void planMapsTheRequestToTheCommandAndReturnsThePlanWithoutApplyingIt() throws Exception {
        when(planWorkingTimeChangeUseCase.plan(any(PlanWorkingTimeChangeCommand.class)))
                .thenReturn(new WorkingTimePlan(
                        TimelineOperation.ADD,
                        null,
                        new WorkingTimeOccurrence(null, LocalDate.of(2026, 1, 16), null),
                        null,
                        new WorkingTimePlanAdjustment(
                                1,
                                new WorkingTimePeriod(LocalDate.of(2026, 1, 1), null),
                                new WorkingTimePeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15))
                        ),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                new WorkingTimeOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)),
                                new WorkingTimeOccurrence(null, LocalDate.of(2026, 1, 16), null)
                        )
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/working-times/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "ADD",
                                  "startDate": "2026-01-16"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("ADD"))
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.rejection").doesNotExist())
                .andExpect(jsonPath("$.occurrence.workingTimeNumber").doesNotExist())
                .andExpect(jsonPath("$.adjustedOccurrence.workingTimeNumber").value(1))
                .andExpect(jsonPath("$.adjustedOccurrence.after.endDate[2]").value(15))
                .andExpect(jsonPath("$.projected[0].workingTimeNumber").value(1))
                .andExpect(jsonPath("$.projected[1].workingTimeNumber").doesNotExist());

        ArgumentCaptor<PlanWorkingTimeChangeCommand> captor = ArgumentCaptor.forClass(PlanWorkingTimeChangeCommand.class);
        verify(planWorkingTimeChangeUseCase).plan(captor.capture());
        assertEquals(TimelineOperation.ADD, captor.getValue().operation());
        assertEquals(LocalDate.of(2026, 1, 16), captor.getValue().startDate());
        assertNull(captor.getValue().endDate());
        assertNull(captor.getValue().workingTimeNumber());
        verify(createWorkingTimeUseCase, org.mockito.Mockito.never()).create(any());
    }

    @Test
    void aRejectedPlanIsStillHttp200WithTheGapNamed() throws Exception {
        when(planWorkingTimeChangeUseCase.plan(any(PlanWorkingTimeChangeCommand.class)))
                .thenReturn(new WorkingTimePlan(
                        TimelineOperation.REMOVE,
                        TimelineRejection.GAP_NOT_ALLOWED,
                        new WorkingTimeOccurrence(2, LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31)),
                        null,
                        null,
                        List.of(),
                        List.of(new WorkingTimePeriod(LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31))),
                        List.of(new WorkingTimeOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15))),
                        List.of(new WorkingTimeOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)))
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/working-times/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "REMOVE",
                                  "workingTimeNumber": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.rejection").value("GAP_NOT_ALLOWED"))
                .andExpect(jsonPath("$.gaps[0].startDate[2]").value(16))
                .andExpect(jsonPath("$.stretchCandidates[0].workingTimeNumber").value(1));

        ArgumentCaptor<PlanWorkingTimeChangeCommand> captor = ArgumentCaptor.forClass(PlanWorkingTimeChangeCommand.class);
        verify(planWorkingTimeChangeUseCase).plan(captor.capture());
        assertEquals(TimelineOperation.REMOVE, captor.getValue().operation());
        assertEquals(2, captor.getValue().workingTimeNumber());
    }

    @Test
    void createMapsACorrectionAskedForAsAnAddToHttp409NamingTheWorkingTimeToCorrect() throws Exception {
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class)))
                .thenThrow(new WorkingTimeIsACorrectionException(
                        "ESP", "INTERNAL", "EMP001",
                        new WorkingTimeOccurrence(1, LocalDate.of(2026, 1, 1), null),
                        new WorkingTimePeriod(LocalDate.of(2026, 1, 1), null)
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/working-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-01-01",
                                  "workingTimePercentage": 50
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKING_TIME_IS_A_CORRECTION"))
                .andExpect(jsonPath("$.message", containsString("corrige")))
                .andExpect(jsonPath("$.details.correctedOccurrence.workingTimeNumber").value(1))
                .andExpect(jsonPath("$.details.correctedOccurrence.startDate[0]").value(2026));
    }

    @Test
    void planTellsTheScreenAnAddOnAnExistingStartDateIsACorrectionOfThatWorkingTime() throws Exception {
        when(planWorkingTimeChangeUseCase.plan(any(PlanWorkingTimeChangeCommand.class)))
                .thenReturn(new WorkingTimePlan(
                        TimelineOperation.CORRECT,
                        TimelineRejection.IS_A_CORRECTION,
                        new WorkingTimeOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)),
                        new WorkingTimeOccurrence(1, LocalDate.of(2026, 1, 1), null),
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new WorkingTimeOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)))
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/working-times/plan")
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
                .andExpect(jsonPath("$.correctedOccurrence.workingTimeNumber").value(1))
                .andExpect(jsonPath("$.correctedOccurrence.endDate").doesNotExist())
                .andExpect(jsonPath("$.occurrence.workingTimeNumber").value(1))
                .andExpect(jsonPath("$.occurrence.endDate[2]").value(15));
    }

    @Test
    void planRejectsAnUnknownOperation() throws Exception {
        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/working-times/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "REPLACE",
                                  "startDate": "2026-01-16"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private WorkingTime workingTime(
            int workingTimeNumber,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal percentage
    ) {
        WorkingTimeDerivedHours derivedHours = derivationPolicy.derive(percentage, new java.math.BigDecimal("1736"));

        return WorkingTime.rehydrate(
                (long) workingTimeNumber,
                10L,
                workingTimeNumber,
                startDate,
                endDate,
                percentage,
                derivedHours,
                LocalDateTime.now(),
                                LocalDateTime.now()
        );
    }
}