package com.b4rrhh.geo.territory.infrastructure.web;

import com.b4rrhh.geo.address.application.service.PostalAddressValidator;
import com.b4rrhh.geo.address.domain.model.PostalAddress;
import com.b4rrhh.geo.address.domain.model.PostalAddressInput;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El territorio se lee por país, no por reglamentación (backend#169). El backend#155 decidió que una
 * dirección se valida por su país —un empleado de FRA que vive en Valencia tiene calle española—, y
 * la consulta del backend#154 servía provincias y tipos de vía por la reglamentación de quien
 * preguntaba: FRA no monta la capa ESP, y el formulario no habría encontrado ni la provincia.
 *
 * <p>Ahora el país se resuelve a su capa nacional, la de la reglamentación que lo tiene como suyo, y
 * la validación usa la misma resolución. Un país sin reglamentación propia no tiene territorio: sus
 * listas vienen vacías, y {@code GET /territory/{país}} lo dice.</p>
 */
@TestWebSobreEsquemaReal
@WithMockUser(roles = "ADMIN")
class TheTerritoryIsReadByCountryEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostalAddressValidator validator;

    @Test
    void theProvincesOfSpainAreTheFiftyTwoWhoeverAsks() throws Exception {
        mockMvc.perform(get("/territory/ESP/provinces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(52)))
                .andExpect(jsonPath("$[?(@.code == '46')].region.code").value("10"));
        mockMvc.perform(get("/territory/ESP/street-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(93)));
    }

    @Test
    void aCountryWithATerritorySaysSo() throws Exception {
        mockMvc.perform(get("/territory/ESP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("ESP"))
                .andExpect(jsonPath("$.hasTerritory").value(true))
                .andExpect(jsonPath("$.message").value(nullValue()));
    }

    @Test
    void aCountryWithoutARuleSystemHasNoTerritoryAndSaysWhy() throws Exception {
        mockMvc.perform(get("/territory/ITA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasTerritory").value(false))
                .andExpect(jsonPath("$.message", containsString("ITA")));
        mockMvc.perform(get("/territory/ITA/provinces")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/territory/ITA/street-types")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/territory/ITA/municipalities").param("name", "Roma"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/territory/ITA/postal-codes/00100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.known").value(false))
                .andExpect(jsonPath("$.message", containsString("ITA")));
    }

    @Test
    void aSpanishAddressOfAnEmployeeOfFranceEntersNormalized() {
        PostalAddress address = validator.validate("FRA", new PostalAddressInput(
                "CL", "de Colón", "12", null, null, null, null, null, null, null,
                "46250", "46", "46001", "ESP"), LocalDate.of(2026, 1, 1));

        assertThat(address.normalizationIssue()).isNull();
        assertThat(address.normalized()).isTrue();
    }
}
