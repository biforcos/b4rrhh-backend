package com.b4rrhh.rulesystem.layer;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * backend#166 (ADR-077): desde una reglamentación se ve lo de todas sus capas, pero sólo se escribe
 * en la nacional. Un país de {@code INT} desde ESP es un 409 que nombra la capa y el camino bueno;
 * desde {@code /layers/INT} se mantiene, y lo ven las demás.
 */
@TestWebSobreEsquemaReal
class ARuleSystemDoesNotWriteWhatAnotherLayerDefinesEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser
    void aCountryIsNotCorrectedFromEspButFromIntAndFraSeesIt() throws Exception {
        mockMvc.perform(put("/rule-entities/ESP/COUNTRY/FRA/1900-01-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "France (corregido desde ESP)"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("defined in layer INT")))
                .andExpect(jsonPath("$.message", containsString("/layers/INT/rule-entities/COUNTRY/FRA")));

        mockMvc.perform(put("/layers/INT/rule-entities/COUNTRY/FRA/1900-01-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "France (corregido en INT)"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/rule-entities")
                        .param("ruleSystemCode", "FRA")
                        .param("ruleEntityTypeCode", "COUNTRY")
                        .param("code", "FRA"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("France (corregido en INT)"));
    }

    @Test
    @WithMockUser
    void aCountryIsNotDeletedNorClosedFromEspButFromInt() throws Exception {
        mockMvc.perform(delete("/rule-entities/ESP/COUNTRY/ATA/1900-01-01"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("defined in layer INT")));
        mockMvc.perform(post("/rule-entities/ESP/COUNTRY/ATA/1900-01-01/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endDate": "2025-12-31"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("defined in layer INT")));

        mockMvc.perform(delete("/layers/INT/rule-entities/COUNTRY/ATA/1900-01-01"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/rule-entities")
                        .param("ruleSystemCode", "FRA")
                        .param("ruleEntityTypeCode", "COUNTRY")
                        .param("code", "ATA"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    void aSharedTypeIsNotCreatedFromARuleSystem() throws Exception {
        mockMvc.perform(post("/rule-entities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleSystemCode": "ESP", "ruleEntityTypeCode": "COUNTRY", "code": "ZZZ",
                                 "name": "Probe Country", "startDate": "2020-01-01"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("/layers/INT/rule-entities")));
    }

    // Lo nacional sigue escribiéndose desde su reglamentación: la guarda no lo toca.
    @Test
    @WithMockUser
    void aNationalEntityIsStillCorrectedFromItsRuleSystem() throws Exception {
        mockMvc.perform(put("/rule-entities/ESP/EMPLOYEE_PRESENCE_EXIT_REASON/RETIREMENT/1900-01-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Jubilación (corregida)"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.layerCode").value("ESP"));
    }

    // Lo hacía ya el frontend#131; aquí se afirma contra el esquema real: una referencia de FRA
    // basta para que INT no lo borre, aunque ESP no lo use.
    @Test
    @WithMockUser
    void aCountryUsedOnlyByFraIsNotDeletedFromInt() throws Exception {
        Long employeeId = jdbcTemplate.queryForObject("""
                insert into employee.employee (rule_system_code, employee_type_code, employee_number, first_name, last_name_1)
                values ('FRA', 'INTERNAL', 'T166FRA01', 'Test', 'Test')
                returning id
                """, Long.class);
        jdbcTemplate.update("""
                insert into employee.address (employee_id, address_number, address_type_code, street_name, locality, country_code, start_date)
                values (?, 1, 'FISCAL', 'Rue 1', 'Andorra la Vella', 'AND', date '2026-01-01')
                """, employeeId);

        mockMvc.perform(delete("/layers/INT/rule-entities/COUNTRY/AND/1900-01-01"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("1 addresses")));
    }
}
