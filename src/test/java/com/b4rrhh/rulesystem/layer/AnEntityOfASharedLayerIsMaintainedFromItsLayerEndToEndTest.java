package com.b4rrhh.rulesystem.layer;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * frontend#131 (ADR-077): una entidad de {@code COM} o de {@code INT} se mantiene desde su capa.
 *
 * <p>Desde una reglamentación sólo se ve: Catálogos la enseña «definida en INT» y no ofrece
 * nada (frontend#127). Hasta aquí no había dónde mantenerla, porque todo el API de entidades iba
 * por reglamentación. Lo que se comprueba es el criterio del issue: se corrige un país en INT y
 * lo ven ESP, FRA y PRT, que es la misma fila.
 */
@TestWebSobreEsquemaReal
class AnEntityOfASharedLayerIsMaintainedFromItsLayerEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser
    void theLayersComeWithTheirLevelAndWhoMountsThem() throws Exception {
        mockMvc.perform(get("/layers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("COM"))
                .andExpect(jsonPath("$[0].level").value(1))
                // Las tres, no sólo las tres: en la batería entera otros tests dejan reglamentaciones
                // confirmadas, y todas montan COM.
                .andExpect(jsonPath("$[0].mountedBy", hasItems("ESP", "FRA", "PRT")))
                .andExpect(jsonPath("$[1].code").value("INT"))
                .andExpect(jsonPath("$[1].level").value(2))
                .andExpect(jsonPath("$[?(@.code == 'ESP')].level").value(3))
                .andExpect(jsonPath("$[?(@.code == 'ESP')].mountedBy[0]").value("ESP"))
                .andExpect(jsonPath("$[?(@.code == 'NOM_ESP_EMP')].level").value(5));
    }

    @Test
    @WithMockUser
    void aCountryCorrectedInIntIsSeenCorrectedFromEspFraAndPrt() throws Exception {
        mockMvc.perform(put("/layers/INT/rule-entities/COUNTRY/FRA/1900-01-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "France (corregido en INT)"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.layerCode").value("INT"))
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.name").value("France (corregido en INT)"))
                .andExpect(jsonPath("$.ruleSystemCode").doesNotExist());

        for (String ruleSystemCode : new String[] {"ESP", "FRA", "PRT"}) {
            mockMvc.perform(get("/rule-entities")
                            .param("ruleSystemCode", ruleSystemCode)
                            .param("ruleEntityTypeCode", "COUNTRY")
                            .param("code", "FRA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("France (corregido en INT)"));
        }
    }

    @Test
    @WithMockUser
    void theLayerListsOnlyItsOwnEntities() throws Exception {
        mockMvc.perform(get("/layers/INT/rule-entities").param("ruleEntityTypeCode", "COUNTRY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(249)))
                .andExpect(jsonPath("$[0].layerCode").value("INT"));

        // Un tipo nacional no vive en INT: la lista viene vacía, no con las filas de ESP.
        mockMvc.perform(get("/layers/INT/rule-entities").param("ruleEntityTypeCode", "EMPLOYEE_PRESENCE_EXIT_REASON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    void aCountryCreatedInIntIsSeenFromEveryRuleSystem() throws Exception {
        mockMvc.perform(post("/layers/INT/rule-entities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleEntityTypeCode": "COUNTRY", "code": "ZZZ", "name": "Probe Country",
                                 "startDate": "2020-01-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.layerCode").value("INT"));
        jdbcTemplate.execute("set constraints all immediate");

        mockMvc.perform(get("/rule-entities")
                        .param("ruleSystemCode", "PRT")
                        .param("ruleEntityTypeCode", "COUNTRY")
                        .param("code", "ZZZ"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].layerCode").value("INT"));
    }

    // El nivel del tipo manda (ADR-077 §4). Sin esta comprobación, el trigger diferido lo
    // rechazaría igual, pero al confirmar y con un 500.
    @Test
    @WithMockUser
    void aTypeIsNotCreatedInALayerOfAnotherLevel() throws Exception {
        mockMvc.perform(post("/layers/ESP/rule-entities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleEntityTypeCode": "COUNTRY", "code": "ZZZ", "name": "Probe Country",
                                 "startDate": "2020-01-01"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void aCountryIsClosedInIntAndAnUnusedOneDeleted() throws Exception {
        mockMvc.perform(post("/layers/INT/rule-entities/COUNTRY/ATA/1900-01-01/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endDate": "2025-12-31"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.endDate").value("2025-12-31"));

        mockMvc.perform(delete("/layers/INT/rule-entities/COUNTRY/ATA/1900-01-01"))
                .andExpect(status().isNoContent());

        // Por el API y no por JdbcTemplate: el borrado de JPA sigue pendiente dentro de la
        // transacción del test, y sólo una consulta de JPA lo vacía antes de leer.
        mockMvc.perform(get("/rule-entities")
                        .param("ruleSystemCode", "FRA")
                        .param("ruleEntityTypeCode", "COUNTRY")
                        .param("code", "ATA"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    void anUnknownLayerIsNotFound() throws Exception {
        mockMvc.perform(get("/layers/NOPE/rule-entities"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/layers/NOPE/rule-entities/COUNTRY/FRA/1900-01-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "France"}
                                """))
                .andExpect(status().isNotFound());
    }
}
