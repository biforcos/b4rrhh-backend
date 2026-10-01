package com.b4rrhh.rulesystem.layer;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * backend#156 (ADR-077): antes de las capas, crear una reglamentación por la API y darle una
 * entidad era todo lo que hacía falta. Tiene que seguir siéndolo: la reglamentación nace con sus
 * cinco capas, y la entidad que se le crea después va a su capa nacional.
 */
@TestWebSobreEsquemaReal
class ARuleSystemCreatedThroughTheApiBringsItsFiveLayersEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(roles = "ADMIN")
    void theNewRuleSystemMountsComIntAndItsOwnThreeAndTakesAnEntity() throws Exception {
        mockMvc.perform(post("/rule-systems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "AND", "name": "Andorra", "countryCode": "AND"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/rule-entities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleSystemCode": "AND", "ruleEntityTypeCode": "EMPLOYEE_PRESENCE_EXIT_REASON",
                                 "code": "TERMINATION", "name": "Baja", "startDate": "2020-01-01"}
                                """))
                .andExpect(status().isCreated());

        // La comprobacion de las cinco capas es diferida: sin esto, el rollback del test se la
        // saltaria.
        jdbcTemplate.execute("set constraints all immediate");

        assertThat(jdbcTemplate.queryForObject("""
                select string_agg(layer_code, ', ' order by level)
                  from rulesystem.rule_system_layer where rule_system_code = 'AND'
                """, String.class))
                .isEqualTo("COM, INT, AND, NOM_AND, NOM_AND_EMP");
        assertThat(jdbcTemplate.queryForObject("""
                select layer_code from rulesystem.rule_entity
                 where rule_entity_type_code = 'EMPLOYEE_PRESENCE_EXIT_REASON' and code = 'TERMINATION'
                   and layer_code = 'AND'
                """, String.class))
                .isEqualTo("AND");
    }

    // El tipo declara su nivel, sin default en la base. El que se crea por la API nace nacional
    // hasta que el backend#158 reclasifique, y sus entidades van a la capa nacional.
    @Test
    @WithMockUser(roles = "ADMIN")
    void aTypeCreatedThroughTheApiIsNationalAndItsEntitiesGoToTheNationalLayer() throws Exception {
        mockMvc.perform(post("/rule-entity-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "ZZ_PROBE_TYPE", "name": "Probe", "literalClass": "DOMAIN_VOCABULARY",
                                 "maintenanceMode": "MAINTAINED", "groupCode": "ORGANIZATION"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/rule-entities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleSystemCode": "ESP", "ruleEntityTypeCode": "ZZ_PROBE_TYPE",
                                 "code": "ZZ_ONE", "name": "Una", "startDate": "2020-01-01"}
                                """))
                .andExpect(status().isCreated());
        jdbcTemplate.execute("set constraints all immediate");

        assertThat(jdbcTemplate.queryForObject(
                "select level from rulesystem.rule_entity_type where code = 'ZZ_PROBE_TYPE'", Integer.class))
                .isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "select layer_code from rulesystem.rule_entity where rule_entity_type_code = 'ZZ_PROBE_TYPE'",
                String.class))
                .isEqualTo("ESP");
    }
}
