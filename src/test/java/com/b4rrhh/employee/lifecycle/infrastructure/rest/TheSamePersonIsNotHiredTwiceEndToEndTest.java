package com.b4rrhh.employee.lifecycle.infrastructure.rest;

import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.usecase.TerminateEmployeeUseCase;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La misma persona no se da de alta dos veces (b4rrhh/backend#141).
 *
 * <p>La primera nota del revisor de la demo: «¿Cómo aseguramos que no demos al mismo empleado
 * 2 veces? DNI en la contratación para revisar que no exista ya». El alta pedía nombre y
 * apellidos y nada más que identificara a la persona, así que dos altas de la misma persona
 * eran dos empleados.
 *
 * <p>Ahora el alta pide el documento y lo guarda como identificador principal. Si ya es de otro
 * empleado del mismo sistema de reglas, se niega y lo nombra, con su estado: si está cesado, el
 * camino bueno es readmitirlo; si está de alta, no hay nada que hacer.
 */
@TestWebSobreEsquemaReal
class TheSamePersonIsNotHiredTwiceEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TerminateEmployeeUseCase terminateEmployeeUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(roles = "ADMIN")
    void theHireKeepsTheDocumentAsThePrimaryIdentifier() throws Exception {
        String dni = DatosDePrueba.dni();
        String employeeNumber = hire(dni);

        String stored = jdbcTemplate.queryForObject("""
                select i.identifier_type_code || ':' || i.identifier_value || ':' || i.is_primary
                  from employee.identifier i
                  join employee.employee e on e.id = i.employee_id
                 where e.rule_system_code = 'ESP' and e.employee_number = ?
                """, String.class, employeeNumber);
        assertThat(stored).isEqualTo("NATIONAL_ID:" + dni + ":true");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aHireWithoutDocumentIsRejected() throws Exception {
        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireBody(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("identifier")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void theSecondHireOfAnActiveEmployeeIsRefusedNamingHim() throws Exception {
        String dni = DatosDePrueba.dni();
        String first = hire(dni);

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireBody(dni)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HIRE_IDENTIFIER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value(containsString("Este DNI ya es " + first)))
                .andExpect(jsonPath("$.message").value(containsString("de alta")))
                .andExpect(jsonPath("$.details.employeeNumber").value(first))
                .andExpect(jsonPath("$.details.employeeTypeCode").value("INTERNAL"))
                .andExpect(jsonPath("$.details.active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void theSecondHireOfACeasedEmployeeOffersTheRehire() throws Exception {
        String dni = DatosDePrueba.dni();
        String first = hire(dni);
        terminateEmployeeUseCase.terminate(new TerminateEmployeeCommand(
                "ESP", "INTERNAL", first, LocalDate.of(2026, 5, 13), "TERMINATION"));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireBody(dni)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HIRE_IDENTIFIER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value(containsString("Este DNI ya es " + first + " (cesado el 13/05/2026)")))
                .andExpect(jsonPath("$.message").value(containsString("readmisión")))
                .andExpect(jsonPath("$.details.active").value(false))
                .andExpect(jsonPath("$.details.ceasedOn").value("2026-05-13"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void theDocumentIsComparedWithoutCaseOrSpaces() throws Exception {
        String dni = DatosDePrueba.dni();
        hire(dni);

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireBody(" " + dni.toLowerCase() + " ")))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aSpanishForeignerIdIsAccepted() throws Exception {
        // NIE X1234567L: la X vale 0, y 01234567 % 23 = 19, que es la L.
        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireBody("X1234567L")))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void theRehireRefusesADocumentOfAnotherEmployee() throws Exception {
        String other = DatosDePrueba.dni();
        hire(other);
        String own = DatosDePrueba.dni();
        String rehired = hire(own);
        terminateEmployeeUseCase.terminate(new TerminateEmployeeCommand(
                "ESP", "INTERNAL", rehired, LocalDate.of(2026, 5, 13), "TERMINATION"));

        mockMvc.perform(post("/employees/ESP/INTERNAL/{employeeNumber}/rehire", rehired)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(other)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REHIRE_IDENTIFIER_OF_ANOTHER_EMPLOYEE"))
                .andExpect(jsonPath("$.message").value(containsString("Este DNI ya es ")));

        mockMvc.perform(post("/employees/ESP/INTERNAL/{employeeNumber}/rehire", rehired)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(own)))
                .andExpect(status().isCreated());
    }

    private String hire(String dni) throws Exception {
        String response = mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireBody(dni)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.employeeNumber");
    }

    private static String hireBody(String dni) {
        String identifier = dni == null ? "" : """
                  "identifier": { "identifierTypeCode": "NATIONAL_ID", "identifierValue": "%s", "issuingCountryCode": "ESP" },
                """.formatted(dni);
        return """
                {
                  "ruleSystemCode": "ESP",
                  "employeeTypeCode": "INTERNAL",
                  "firstName": "Ana",
                  "lastName1": "Lopez",
                  %s
                  "hireDate": "2026-04-01",
                  "companyCode": "ES01",
                  "workCenterCode": "MAIN_OFFICE",
                  "costCenterDistribution": { "items": [ { "costCenterCode": "CC_ADMIN", "allocationPercentage": 100.0 } ] },
                  "contract": { "contractTypeCode": "100", "contractSubtypeCode": "01" },
                  "laborClassification": { "agreementCode": "99002405011982", "agreementCategoryCode": "99002405-G3" },
                  "workingTime": { "workingTimePercentage": 100 }
                }
                """.formatted(identifier);
    }

    private static String rehireBody(String dni) {
        return """
                {
                  "rehireDate": "2026-07-01",
                  "entryReasonCode": "REHIRE",
                  "companyCode": "ES01",
                  "identifier": { "identifierTypeCode": "NATIONAL_ID", "identifierValue": "%s", "issuingCountryCode": "ESP" },
                  "workCenter": { "workCenterCode": "MAIN_OFFICE" },
                  "costCenterDistribution": { "items": [ { "costCenterCode": "CC_ADMIN", "allocationPercentage": 100.0 } ] },
                  "contract": { "contractTypeCode": "100", "contractSubtypeCode": "01" },
                  "laborClassification": { "agreementCode": "99002405011982", "agreementCategoryCode": "99002405-G3" },
                  "workingTime": { "workingTimePercentage": 100 }
                }
                """.formatted(dni);
    }
}
