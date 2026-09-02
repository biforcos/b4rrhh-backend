package com.b4rrhh.employee.journey.infrastructure.web;

import com.b4rrhh.employee.journey.application.usecase.EmployeeJourneyTimelineView;
import com.b4rrhh.employee.journey.application.usecase.GetEmployeeJourneyV2Command;
import com.b4rrhh.employee.journey.application.usecase.GetEmployeeJourneyV2UseCase;
import com.b4rrhh.employee.journey.application.usecase.JourneyEmployeeHeaderView;
import com.b4rrhh.employee.journey.application.usecase.JourneyEmployeeNotFoundException;
import com.b4rrhh.employee.journey.application.usecase.JourneyEventStatus;
import com.b4rrhh.employee.journey.application.usecase.JourneyEventType;
import com.b4rrhh.employee.journey.application.usecase.JourneyEventView;
import com.b4rrhh.employee.journey.application.usecase.JourneyTrackCode;
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
class JourneyV2ControllerHttpTest {

    @Mock
    private GetEmployeeJourneyV2UseCase getEmployeeJourneyV2UseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JourneyV2Controller controller = new JourneyV2Controller(getEmployeeJourneyV2UseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new JourneyV2ExceptionHandler())
                .build();
    }

    @Test
    void mapsPathToCommandAndReturnsEventBasedJourneyShape() throws Exception {
        when(getEmployeeJourneyV2UseCase.get(any(GetEmployeeJourneyV2Command.class)))
                .thenReturn(sampleJourney());

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/journey-v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.ruleSystemCode").value("ESP"))
                .andExpect(jsonPath("$.employee.employeeTypeCode").value("INTERNAL"))
                .andExpect(jsonPath("$.employee.employeeNumber").value("EMP001"))
                .andExpect(jsonPath("$.employee.displayName").value("Lidia Morales"))
                .andExpect(jsonPath("$.events[0].eventDate[0]").value(2023))
                .andExpect(jsonPath("$.events[0].eventDate[1]").value(1))
                .andExpect(jsonPath("$.events[0].eventDate[2]").value(10))
                .andExpect(jsonPath("$.events[0].eventType").value("HIRE"))
                .andExpect(jsonPath("$.events[0].trackCode").value("PRESENCE"))
                // Sin frases: el cliente rotula el tipo de evento (ADR-052 §4, backend#40).
                .andExpect(jsonPath("$.events[0].title").doesNotExist())
                .andExpect(jsonPath("$.events[0].subtitle").doesNotExist())
                .andExpect(jsonPath("$.events[0].status").value("completed"))
                .andExpect(jsonPath("$.events[0].isCurrent").value(false))
                .andExpect(jsonPath("$.events[0].details.presenceNumber").value(1))
                .andExpect(jsonPath("$.events[0].employeeId").doesNotExist());

        ArgumentCaptor<GetEmployeeJourneyV2Command> captor = ArgumentCaptor.forClass(GetEmployeeJourneyV2Command.class);
        verify(getEmployeeJourneyV2UseCase).get(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
    }

    @Test
    void returns404WhenEmployeeJourneyV2IsNotFound() throws Exception {
        when(getEmployeeJourneyV2UseCase.get(any(GetEmployeeJourneyV2Command.class)))
                .thenThrow(new JourneyEmployeeNotFoundException("ESP", "INTERNAL", "EMP001"));

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/journey-v2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Employee not found")));
    }

    @Test
    void requiresExactJourneyV2Path() throws Exception {
        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/journey-v22"))
                .andExpect(status().isNotFound());
    }

    private EmployeeJourneyTimelineView sampleJourney() {
        return new EmployeeJourneyTimelineView(
                new JourneyEmployeeHeaderView("ESP", "INTERNAL", "EMP001", "Lidia Morales"),
                List.of(
                        new JourneyEventView(
                                LocalDate.of(2023, 1, 10),
                                JourneyEventType.HIRE,
                                JourneyTrackCode.PRESENCE,
                                JourneyEventStatus.COMPLETED,
                                false,
                                Map.of(
                                        "companyCode", "ES01",
                                        "entryReasonCode", "HIRING",
                                        "exitReasonCode", "",
                                        "presenceNumber", 1
                                )
                        )
                )
        );
    }
}