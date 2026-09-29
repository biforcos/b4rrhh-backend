package com.b4rrhh.employee.lifecycle.infrastructure.rest;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un alta es una contratación por construcción: el motivo de entrada no es una pregunta
 * (b4rrhh/backend#143).
 *
 * <p>El revisor de la demo: «el motivo de entrada en la contratación debería ser HIRING (rehire
 * ya tiene un workflow, y transfer… ni siquiera tenemos claro qué es transfer)». El formulario
 * ofrecía los tres y el alta aceptaba cualquiera, así que se podía contratar a alguien «por
 * readmisión» sin pasar por la readmisión.
 *
 * <p>Sin motivo, el alta entra como {@code HIRING}. Con {@code HIRING} explícito, igual: es lo
 * que ya mandaban los clientes y no hay por qué romperlos. Con otro, se rechaza nombrando el
 * camino bueno. {@code TRANSFER_IN} se queda en el catálogo —hay presencias sembradas con él—
 * pero sin flujo que lo use hasta que se defina qué es.
 */
@TestWebSobreEsquemaReal
class AHireIsAlwaysAHiringEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(roles = "ADMIN")
    void aHireWithoutReasonEntersAsHiring() throws Exception {
        String employeeNumber = com.jayway.jsonpath.JsonPath.read(mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireBody(null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.initialPresence.entryReasonCode").value("HIRING"))
                .andReturn().getResponse().getContentAsString(), "$.employeeNumber");

        String stored = jdbcTemplate.queryForObject("""
                select p.entry_reason_code
                  from employee.presence p
                  join employee.employee e on e.id = p.employee_id
                 where e.rule_system_code = 'ESP' and e.employee_number = ?
                """, String.class, employeeNumber);
        assertThat(stored).isEqualTo("HIRING");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anExplicitHiringIsStillAccepted() throws Exception {
        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireBody("HIRING")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.initialPresence.entryReasonCode").value("HIRING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aHireAsTransferInIsRejectedSayingItHasNoFlow() throws Exception {
        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireBody("TRANSFER_IN")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("HIRE_ENTRY_REASON_NOT_HIRING"))
                .andExpect(jsonPath("$.message").value(containsString("TRANSFER_IN")))
                .andExpect(jsonPath("$.message").value(containsString("contratación")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aHireAsRehireIsRejectedPointingToTheRehire() throws Exception {
        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireBody("REHIRE")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("HIRE_ENTRY_REASON_NOT_HIRING"))
                .andExpect(jsonPath("$.message").value(containsString("readmisión")));
    }

    private static String hireBody(String entryReasonCode) {
        String reason = entryReasonCode == null ? "" : "\"entryReasonCode\": \"" + entryReasonCode + "\",";
        return """
                {
                  "ruleSystemCode": "ESP",
                  "employeeTypeCode": "INTERNAL",
                  "firstName": "Ana",
                  "lastName1": "Lopez",
                  "hireDate": "2026-04-01",
                  %s
                  "companyCode": "ES01",
                  "workCenterCode": "MAIN_OFFICE",
                  "costCenterDistribution": { "items": [ { "costCenterCode": "CC_ADMIN", "allocationPercentage": 100.0 } ] },
                  "contract": { "contractTypeCode": "100", "contractSubtypeCode": "01" },
                  "laborClassification": { "agreementCode": "99002405011982", "agreementCategoryCode": "99002405-G3" },
                  "workingTime": { "workingTimePercentage": 100 }
                }
                """.formatted(reason);
    }
}
