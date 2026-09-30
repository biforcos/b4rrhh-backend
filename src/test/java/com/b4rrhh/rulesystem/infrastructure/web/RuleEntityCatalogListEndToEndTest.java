package com.b4rrhh.rulesystem.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * backend#152: la lista de mantenimiento de Catálogos es {@code GET /rule-entities}, y no
 * pasaba por el resolutor. Con {@code Accept-Language: es-ES} devolvía «Company Mobile»
 * mientras la V114 tiene «Móvil de empresa», que es lo que sí salía en los desplegables de la
 * ficha. Ahora viajan los dos: el almacenado, que es el que se edita, y la etiqueta.
 */
@TestWebSobreEsquemaReal
class RuleEntityCatalogListEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void theCatalogListCarriesTheStoredNameAndTheLabelInTheRequestedLanguage() throws Exception {
        mockMvc.perform(get("/rule-entities")
                        .param("ruleSystemCode", "ESP")
                        .param("ruleEntityTypeCode", "CONTACT_TYPE")
                        .header("Accept-Language", "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'COMPANY_MOBILE')].name").value("Company Mobile"))
                .andExpect(jsonPath("$[?(@.code == 'COMPANY_MOBILE')].label").value("Móvil de empresa"))
                .andExpect(jsonPath("$[?(@.code == 'EMAIL')].label").value("Correo electrónico"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void withoutALanguageTheLabelIsTheStoredName() throws Exception {
        mockMvc.perform(get("/rule-entities")
                        .param("ruleSystemCode", "ESP")
                        .param("ruleEntityTypeCode", "CONTACT_TYPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'COMPANY_MOBILE')].label").value("Company Mobile"));
    }

    // Una cita reglamentaria no se traduce (ADR-052 §2): su etiqueta es su nombre, pida el
    // cliente el idioma que pida.
    @Test
    @WithMockUser(roles = "ADMIN")
    void aRegulatoryCitationIsItsOwnLabel() throws Exception {
        mockMvc.perform(get("/rule-entities")
                        .param("ruleSystemCode", "ESP")
                        .param("ruleEntityTypeCode", "CONTRACT")
                        .param("code", "100")
                        .header("Accept-Language", "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Indefinido ordinario (jornada completa)"))
                .andExpect(jsonPath("$[0].label").value("Indefinido ordinario (jornada completa)"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void theOccurrenceByBusinessKeyCarriesTheLabelToo() throws Exception {
        mockMvc.perform(get("/rule-entities/ESP/CONTACT_TYPE/COMPANY_MOBILE/1900-01-01")
                        .header("Accept-Language", "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Company Mobile"))
                .andExpect(jsonPath("$.label").value("Móvil de empresa"));
    }
}
