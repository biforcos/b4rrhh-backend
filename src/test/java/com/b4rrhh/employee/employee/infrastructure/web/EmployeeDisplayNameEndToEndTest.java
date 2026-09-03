package com.b4rrhh.employee.employee.infrastructure.web;

import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.usecase.HireEmployeeUseCase;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Extremo a extremo (backend#42): el nombre para mostrar sale de la API con el formato
 * normativo que la V115 siembra para ESP, contra el esquema real.
 *
 * El alta entra en minúsculas a propósito: con un nombre ya capitalizado, la
 * concatenación a pelo que se usa cuando no hay formato produce exactamente lo mismo que
 * FULL_TITLE_CASE, y el test no distinguiría una base sembrada de una que no lo está.
 * Ésa es justo la trampa que dejó pasar el formato puesto a mano durante meses.
 *
 * El sustituto («Bifor») gana sobre el formato y se muestra solo, sin apellidos: es la
 * semántica que fijó Juan, y el sustituto y el nombre son distintos para que el test no
 * pueda pasar por casualidad.
 */
@TestWebSobreEsquemaReal
class EmployeeDisplayNameEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HireEmployeeUseCase hireEmployeeUseCase;

    @Test
    @WithMockUser(roles = "ADMIN")
    void employeeWithoutSubstituteIsServedWithTheSeededEspFormat() throws Exception {
        String employeeNumber = hire("juan antonio", "biforcos", "amor", null);

        mockMvc.perform(get("/employees/ESP/INTERNAL/{employeeNumber}", employeeNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredName").doesNotExist())
                .andExpect(jsonPath("$.displayName").value("Juan Antonio Biforcos Amor"));

        mockMvc.perform(get("/employees").param("q", employeeNumber).param("ruleSystemCode", "ESP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].employeeNumber").value(employeeNumber))
                .andExpect(jsonPath("$.items[0].displayName").value("Juan Antonio Biforcos Amor"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void employeeWithSubstituteIsServedWithTheSubstituteAndNothingElse() throws Exception {
        String employeeNumber = hire("juan antonio", "biforcos", "amor", "Bifor");

        mockMvc.perform(get("/employees/ESP/INTERNAL/{employeeNumber}", employeeNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredName").value("Bifor"))
                .andExpect(jsonPath("$.displayName").value("Bifor"));

        mockMvc.perform(get("/employees").param("q", employeeNumber).param("ruleSystemCode", "ESP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].employeeNumber").value(employeeNumber))
                .andExpect(jsonPath("$.items[0].displayName").value("Bifor"));
    }

    /** El escenario base de {@code HireEmployeeBaselineFlywayIntegrationTest}, sobre el convenio real de la V61. */
    private String hire(String firstName, String lastName1, String lastName2, String preferredName) {
        return hireEmployeeUseCase.hire(new HireEmployeeCommand(
                "ESP",
                "INTERNAL",
                firstName,
                lastName1,
                lastName2,
                preferredName,
                LocalDate.of(2026, 4, 1),
                "HIRING",
                "ES01",
                "MAIN_OFFICE",
                new HireEmployeeCommand.HireEmployeeContractCommand("IND", "FT1"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("99002405011982", "99002405-G1"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("100"))
        )).employee().employeeNumber();
    }
}
