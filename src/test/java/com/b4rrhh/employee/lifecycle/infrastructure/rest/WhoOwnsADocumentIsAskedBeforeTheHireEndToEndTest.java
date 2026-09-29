package com.b4rrhh.employee.lifecycle.infrastructure.rest;

import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.usecase.TerminateEmployeeUseCase;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * De quién es un documento se pregunta antes del alta (b4rrhh/backend#149).
 *
 * <p>La segunda revisión a distancia: «hasta que no rellenas toda la contratación no se habilita
 * el botón contratar, y sólo cuando se envía al servidor se devuelve que ya existe». El 409 del
 * {@code backend#141} sabe quién es el dueño; esta consulta deja preguntarlo con el documento
 * recién tecleado. Es cortesía para la pantalla: la garantía sigue siendo el alta.
 */
@TestWebSobreEsquemaReal
class WhoOwnsADocumentIsAskedBeforeTheHireEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TerminateEmployeeUseCase terminateEmployeeUseCase;

    @Test
    @WithMockUser(roles = "ADMIN")
    void aDocumentOfAnActiveEmployeeNamesHim() throws Exception {
        String dni = DatosDePrueba.dni();
        String owner = hire(dni);

        mockMvc.perform(ask(dni))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeTypeCode").value("INTERNAL"))
                .andExpect(jsonPath("$.employeeNumber").value(owner))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.ceasedOn").doesNotExist())
                .andExpect(jsonPath("$.message").value("Este DNI ya es " + owner + ", que está de alta"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aDocumentOfACeasedEmployeeSaysSince() throws Exception {
        String dni = DatosDePrueba.dni();
        String owner = hire(dni);
        terminateEmployeeUseCase.terminate(new TerminateEmployeeCommand(
                "ESP", "INTERNAL", owner, LocalDate.of(2026, 5, 13), "TERMINATION"));

        mockMvc.perform(ask(dni))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeNumber").value(owner))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.ceasedOn").value("2026-05-13"))
                .andExpect(jsonPath("$.message").value("Este DNI ya es " + owner + " (cesado el 13/05/2026)"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aFreeDocumentAnswersEmpty() throws Exception {
        mockMvc.perform(ask(DatosDePrueba.dni()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(emptyString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void theDocumentIsComparedWithoutCaseOrSpaces() throws Exception {
        String dni = DatosDePrueba.dni();
        String owner = hire(dni);

        mockMvc.perform(ask(" " + dni.toLowerCase() + " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeNumber").value(owner));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void theQueryNeedsTheThreeKeys() throws Exception {
        mockMvc.perform(get("/employees/identifier-owner")
                        .param("ruleSystemCode", "ESP")
                        .param("identifierTypeCode", "NATIONAL_ID"))
                .andExpect(status().isBadRequest());
    }

    private static org.springframework.test.web.servlet.RequestBuilder ask(String value) {
        return get("/employees/identifier-owner")
                .param("ruleSystemCode", "ESP")
                .param("identifierTypeCode", "NATIONAL_ID")
                .param("identifierValue", value);
    }

    private String hire(String dni) throws Exception {
        String response = mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                  "identifier": { "identifierTypeCode": "NATIONAL_ID", "identifierValue": "%s", "issuingCountryCode": "ESP" },
                                  "hireDate": "2026-04-01",
                                  "companyCode": "ES01",
                                  "workCenterCode": "MAIN_OFFICE",
                                  "costCenterDistribution": { "items": [ { "costCenterCode": "CC_ADMIN", "allocationPercentage": 100.0 } ] },
                                  "contract": { "contractTypeCode": "100", "contractSubtypeCode": "01" },
                                  "laborClassification": { "agreementCode": "99002405011982", "agreementCategoryCode": "99002405-G3" },
                                  "workingTime": { "workingTimePercentage": 100 }
                                }
                                """.formatted(dni)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.employeeNumber");
    }
}
