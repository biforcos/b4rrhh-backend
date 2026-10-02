package com.b4rrhh.rulesystem.layer;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El Ámbito del frontend ofrece lo que sirve {@code GET /rule-systems}, y eso son
 * reglamentaciones, nunca capas: en {@code INT} no se calcula nómina ni se da de alta a nadie
 * (ADR-077, b4rrhh/frontend#127).
 *
 * <p>La garantía es del backend, no del cliente: el cliente no filtra nada, porque no hay nada
 * que filtrar. Por eso el test está aquí. Primero comprueba que las capas existen —si no, que no
 * salgan no diría nada— y después que la lista no las nombra.
 */
@TestWebSobreEsquemaReal
class TheRuleSystemListOffersAssembliesNeverLayersEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(roles = "ADMIN")
    void theListNamesTheRuleSystemsAndNoneOfTheLayersTheyMount() throws Exception {
        assertThat(jdbcTemplate.queryForList(
                "select code from rulesystem.layer where code in ('COM', 'INT', 'NOM_ESP', 'NOM_ESP_EMP')",
                String.class))
                .containsExactlyInAnyOrder("COM", "INT", "NOM_ESP", "NOM_ESP_EMP");

        mockMvc.perform(get("/rule-systems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code").value(hasItem("ESP")))
                .andExpect(jsonPath("$[*].code").value(not(hasItem("COM"))))
                .andExpect(jsonPath("$[*].code").value(not(hasItem("INT"))))
                .andExpect(jsonPath("$[*].code").value(not(hasItem("NOM_ESP"))))
                .andExpect(jsonPath("$[*].code").value(not(hasItem("NOM_ESP_EMP"))));
    }
}
