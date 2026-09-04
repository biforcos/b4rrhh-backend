package com.b4rrhh.employee.working_time.infrastructure.web;

import com.b4rrhh.employee.working_time.application.usecase.CloseWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CloseWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.GetWorkingTimeByBusinessKeyCommand;
import com.b4rrhh.employee.working_time.application.usecase.GetWorkingTimeByBusinessKeyUseCase;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesCommand;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesUseCase;
import com.b4rrhh.employee.working_time.application.usecase.UpdateWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.UpdateWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.service.StandardWorkingTimeDerivationPolicy;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeCoverageGapException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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