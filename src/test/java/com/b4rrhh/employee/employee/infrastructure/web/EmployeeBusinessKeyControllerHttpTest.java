package com.b4rrhh.employee.employee.infrastructure.web;

import com.b4rrhh.employee.employee.application.DisplayNameComputationService;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.application.usecase.DeleteEmployeeByBusinessKeyCommand;
import com.b4rrhh.employee.employee.application.usecase.DeleteEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.application.usecase.UpdateEmployeeCommand;
import com.b4rrhh.employee.employee.application.usecase.UpdateEmployeeUseCase;
import com.b4rrhh.employee.employee.domain.exception.EmployeeNotFoundException;
import com.b4rrhh.employee.employee.domain.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmployeeBusinessKeyControllerHttpTest {

    @Mock
    private GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase;
        @Mock
        private DeleteEmployeeByBusinessKeyUseCase deleteEmployeeByBusinessKeyUseCase;
    @Mock
    private UpdateEmployeeUseCase updateEmployeeUseCase;

    private final DisplayNameComputationService displayNameComputationService =
            new DisplayNameComputationService(ruleSystemCode -> java.util.Optional.empty());

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        EmployeeBusinessKeyController controller = new EmployeeBusinessKeyController(
                getEmployeeByBusinessKeyUseCase,
                deleteEmployeeByBusinessKeyUseCase,
                updateEmployeeUseCase,
                displayNameComputationService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new EmployeeExceptionHandler())
                .build();
    }

    @Test
    void putByBusinessKeyReturnsUpdatedEmployeeWithSameBusinessKey() throws Exception {
        when(updateEmployeeUseCase.update(any(UpdateEmployeeCommand.class)))
                .thenReturn(new Employee(
                        10L,
                        "ESP",
                        "INTERNAL",
                        "EMP001",
                        "Alicia",
                        "Garcia",
                        "Perez",
                        "Ali",
                        "ACTIVE",
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        null
                ));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001")
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName": "Alicia",
                                  "lastName1": "Garcia",
                                  "lastName2": "Perez",
                                  "preferredName": "Ali"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleSystemCode").value("ESP"))
                .andExpect(jsonPath("$.employeeTypeCode").value("INTERNAL"))
                .andExpect(jsonPath("$.employeeNumber").value("EMP001"))
                .andExpect(jsonPath("$.firstName").value("Alicia"))
                .andExpect(jsonPath("$.lastName1").value("Garcia"))
                .andExpect(jsonPath("$.lastName2").value("Perez"))
                .andExpect(jsonPath("$.preferredName").value("Ali"))
                // La identidad publica del empleado es su clave de negocio, y el id de
                // persistencia no sale por el API (ADR-001 4.3, ADR-004). Es la misma
                // comprobacion que ya hacen address, contract, labor_classification,
                // presence, working_time y los dos flujos de lifecycle (#10).
                .andExpect(jsonPath("$.id").doesNotExist());

        ArgumentCaptor<UpdateEmployeeCommand> captor = ArgumentCaptor.forClass(UpdateEmployeeCommand.class);
        verify(updateEmployeeUseCase).update(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals("Alicia", captor.getValue().firstName());
    }

    @Test
    void putByBusinessKeyReturns404WhenEmployeeDoesNotExist() throws Exception {
        when(updateEmployeeUseCase.update(any(UpdateEmployeeCommand.class)))
                .thenThrow(new EmployeeNotFoundException("ESP", "INTERNAL", "EMP404"));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP404")
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName": "Alicia",
                                  "lastName1": "Garcia",
                                  "lastName2": "Perez",
                                  "preferredName": "Ali"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void putByBusinessKeyReturns400WhenRequestIsInvalid() throws Exception {
        when(updateEmployeeUseCase.update(any(UpdateEmployeeCommand.class)))
                .thenThrow(new IllegalArgumentException("firstName is required"));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001")
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName": "  ",
                                  "lastName1": "Garcia"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("firstName is required"));
    }

    @Test
    void deleteByBusinessKeyReturns204WhenEmployeeExists() throws Exception {
        doNothing().when(deleteEmployeeByBusinessKeyUseCase).delete(any(DeleteEmployeeByBusinessKeyCommand.class));

        mockMvc.perform(delete("/employees/ESP/INTERNAL/EMP001"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<DeleteEmployeeByBusinessKeyCommand> captor =
                ArgumentCaptor.forClass(DeleteEmployeeByBusinessKeyCommand.class);
        verify(deleteEmployeeByBusinessKeyUseCase).delete(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
    }

    @Test
    void deleteByBusinessKeyReturns404WhenEmployeeDoesNotExist() throws Exception {
        doThrow(new EmployeeNotFoundException("ESP", "INTERNAL", "EMP404"))
                .when(deleteEmployeeByBusinessKeyUseCase)
                .delete(any(DeleteEmployeeByBusinessKeyCommand.class));

        mockMvc.perform(delete("/employees/ESP/INTERNAL/EMP404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}