package com.b4rrhh.geo.territory.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La API de consulta del territorio (backend#154, ADR-078), de extremo a extremo sobre el
 * esquema real: los criterios del «Cómo se demuestra» del issue.
 */
@TestWebSobreEsquemaReal
@WithMockUser(roles = "ADMIN")
class TheTerritoryIsQueriedEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void theCountriesAreTheTwoHundredFortyNineWithTheirNameInTheRequestedLanguage() throws Exception {
        mockMvc.perform(get("/territory/ESP/countries").header("Accept-Language", "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(249)))
                .andExpect(jsonPath("$[?(@.code == 'ESP')].name").value("España"))
                .andExpect(jsonPath("$[?(@.code == 'FRA')].name").value("Francia"));
    }

    @Test
    void theFiftyTwoProvincesComeWithTheirRegion() throws Exception {
        mockMvc.perform(get("/territory/ESP/provinces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(52)))
                .andExpect(jsonPath("$[?(@.code == '46')].name").value("Valencia/València"))
                .andExpect(jsonPath("$[?(@.code == '46')].isoCode").value("ES-V"))
                .andExpect(jsonPath("$[?(@.code == '46')].region.code").value("10"))
                .andExpect(jsonPath("$[?(@.code == '46')].region.name").value("Comunitat Valenciana"))
                .andExpect(jsonPath("$[?(@.code == '46')].region.isoCode").value("ES-VC"))
                .andExpect(jsonPath("$[?(@.code == '51')].region.name").value("Ceuta"));
    }

    @Test
    void theStreetTypesAreTheNinetyThreeOfTheCatastro() throws Exception {
        mockMvc.perform(get("/territory/ESP/street-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(93)))
                .andExpect(jsonPath("$[?(@.code == 'CL')].name").value("CALLE"))
                .andExpect(jsonPath("$[?(@.code == 'UR')].name").value("URBANIZACION"));
    }

    @Test
    void searchingValeFindsBothValenciasWithTheirProvinces() throws Exception {
        mockMvc.perform(get("/territory/ESP/municipalities")
                        .param("countryCode", "ESP").param("name", "Vale").param("date", "2026-10-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == '46250')].name").value("València"))
                .andExpect(jsonPath("$[?(@.code == '46250')].province.name").value("Valencia/València"))
                .andExpect(jsonPath("$[?(@.code == '46250')].region.name").value("Comunitat Valenciana"))
                .andExpect(jsonPath("$[?(@.code == '10203')].name").value("Valencia de Alcántara"))
                .andExpect(jsonPath("$[?(@.code == '10203')].province.name").value("Cáceres"));
    }

    @Test
    void aMunicipalityIsFoundOnlyOnDatesItIsInForceOn() throws Exception {
        // Usansolo entra con la relación de 2024: en 2023 no sale, en 2024 sí.
        mockMvc.perform(get("/territory/ESP/municipalities")
                        .param("countryCode", "ESP").param("name", "Usansolo").param("date", "2023-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/territory/ESP/municipalities")
                        .param("countryCode", "ESP").param("name", "Usansolo").param("date", "2024-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("48916"));
    }

    // Entre 2021 y 2025 el INE no dio ninguna baja, así que ningún municipio real tiene
    // end_date. El de la prueba se inserta dentro de la transacción del test y se va con ella.
    @Test
    void aMunicipalityWithAnEndDateIsNotFoundAfterIt() throws Exception {
        jdbcTemplate.update("""
                insert into geo.municipality (country_code, code, name, province_code, start_date, end_date)
                values ('ESP', '46999', 'Valfingida', '46', date '2021-01-01', date '2024-12-31')
                """);

        mockMvc.perform(get("/territory/ESP/municipalities")
                        .param("countryCode", "ESP").param("name", "Valfing").param("date", "2024-06-01"))
                .andExpect(jsonPath("$[*].code").value(hasItem("46999")));
        mockMvc.perform(get("/territory/ESP/municipalities")
                        .param("countryCode", "ESP").param("name", "Valfing").param("date", "2025-01-01"))
                .andExpect(jsonPath("$[*].code").value(not(hasItem("46999"))));
    }

    @Test
    void aPostalCodeSaysItsProvinceByItsFirstTwoDigits() throws Exception {
        mockMvc.perform(get("/territory/ESP/postal-codes/46363").param("countryCode", "ESP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.province.code").value("46"))
                .andExpect(jsonPath("$.message").value(nullValue()));
        mockMvc.perform(get("/territory/ESP/postal-codes/28001").param("countryCode", "ESP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.province.code").value("28"))
                .andExpect(jsonPath("$.province.name").value("Madrid"))
                .andExpect(jsonPath("$.suggestedMunicipality.code").value("28079"))
                .andExpect(jsonPath("$.suggestedMunicipality.name").value("Madrid"));
    }

    @Test
    void aPostalCodeWhoseFirstTwoDigitsAreNoProvinceHasNoneAndSaysWhy() throws Exception {
        mockMvc.perform(get("/territory/ESP/postal-codes/99999").param("countryCode", "ESP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.province").value(nullValue()))
                .andExpect(jsonPath("$.suggestedMunicipality").value(nullValue()))
                .andExpect(jsonPath("$.message").value("Las dos primeras cifras de 99999 no son una provincia."));
    }

    @Test
    void whatIsNotAQuestionIsRejected() throws Exception {
        mockMvc.perform(get("/territory/ESP/postal-codes/4600A").param("countryCode", "ESP"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TERRITORY_INVALID_QUERY"));
        mockMvc.perform(get("/territory/ESP/municipalities").param("countryCode", "ESP").param("name", "V"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TERRITORY_INVALID_QUERY"));
        mockMvc.perform(get("/territory/ZZZ/provinces"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TERRITORY_RULE_SYSTEM_NOT_FOUND"));
    }
}
