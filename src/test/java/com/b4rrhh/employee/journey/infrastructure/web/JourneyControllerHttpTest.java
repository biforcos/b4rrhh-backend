package com.b4rrhh.employee.journey.infrastructure.web;

import com.b4rrhh.employee.journey.application.usecase.EmployeeJourneyView;
import com.b4rrhh.employee.journey.application.usecase.GetEmployeeJourneyCommand;
import com.b4rrhh.employee.journey.application.usecase.GetEmployeeJourneyUseCase;
import com.b4rrhh.employee.journey.application.usecase.JourneyEmployeeHeaderView;
import com.b4rrhh.employee.journey.application.usecase.JourneyEmployeeNotFoundException;
import com.b4rrhh.employee.journey.application.usecase.JourneyItemView;
import com.b4rrhh.employee.journey.application.usecase.JourneyTrackView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JourneyControllerHttpTest {

    @Mock
    private GetEmployeeJourneyUseCase getEmployeeJourneyUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JourneyController controller = new JourneyController(getEmployeeJourneyUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new JourneyExceptionHandler())
                .build();
    }

    @Test
    void mapsPathToCommandAndReturnsExpectedJourneyShapeWithoutTechnicalIds() throws Exception {
        when(getEmployeeJourneyUseCase.get(any(GetEmployeeJourneyCommand.class)))
                .thenReturn(sampleJourney());

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/journey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.ruleSystemCode").value("ESP"))
                .andExpect(jsonPath("$.employee.employeeTypeCode").value("INTERNAL"))
                .andExpect(jsonPath("$.employee.employeeNumber").value("EMP001"))
                .andExpect(jsonPath("$.employee.displayName").value("Lidia Morales"))
                .andExpect(jsonPath("$.employee.id").doesNotExist())
                .andExpect(jsonPath("$.tracks[0].trackCode").value("PRESENCE"))
                .andExpect(jsonPath("$.tracks[0].trackLabel").value("Presence"))
                .andExpect(jsonPath("$.tracks[0].items[0].startDate[0]").value(2026))
                .andExpect(jsonPath("$.tracks[0].items[0].startDate[1]").value(1))
                .andExpect(jsonPath("$.tracks[0].items[0].startDate[2]").value(1))
                .andExpect(jsonPath("$.tracks[0].items[0].label").value("COMP01"))
                .andExpect(jsonPath("$.tracks[0].items[0].details.companyCode").value("COMP01"))
                .andExpect(jsonPath("$.tracks[0].items[0].employeeId").doesNotExist());

        ArgumentCaptor<GetEmployeeJourneyCommand> captor = ArgumentCaptor.forClass(GetEmployeeJourneyCommand.class);
        verify(getEmployeeJourneyUseCase).get(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
    }

    @Test
    void returns404WhenEmployeeJourneyIsNotFound() throws Exception {
        when(getEmployeeJourneyUseCase.get(any(GetEmployeeJourneyCommand.class)))
                .thenThrow(new JourneyEmployeeNotFoundException("ESP", "INTERNAL", "EMP001"));

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/journey"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("No existe el empleado")));
    }

    @Test
    void requiresExactJourneyPath() throws Exception {
        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/journeys"))
                .andExpect(status().isNotFound());
    }

    private EmployeeJourneyView sampleJourney() {
        return new EmployeeJourneyView(
                new JourneyEmployeeHeaderView("ESP", "INTERNAL", "EMP001", "Lidia Morales"),
                List.of(
                        new JourneyTrackView(
                                "PRESENCE",
                                "Presence",
                                List.of(
                                        new JourneyItemView(
                                                LocalDate.of(2026, 1, 1),
                                                null,
                                                "COMP01",
                                                Map.of(
                                                        "companyCode", "COMP01",
                                                        "entryReasonCode", "ENTRY_A",
                                                        "exitReasonCode", ""
                                                )
                                        )
                                )
                        )
                )
        );
    }
}
