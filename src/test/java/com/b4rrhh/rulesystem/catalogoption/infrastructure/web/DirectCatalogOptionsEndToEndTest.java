package com.b4rrhh.rulesystem.catalogoption.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Extremo a extremo (backend#31): las opciones de catalogo con {@code referenceDate}
 * contra el esquema real. Es el primer caso que esta franja tenia que cubrir: esta misma
 * consulta daba 500 en Postgres con la fecha puesta (#30) y los tests con el repositorio
 * mockeado no podian verlo, porque un mock devuelve lo que le digas aunque el JPQL sea
 * invalido. Aqui la consulta se ejecuta de verdad, y se afirma sobre el cuerpo, no solo
 * sobre el estado.
 */
@TestWebSobreEsquemaReal
class DirectCatalogOptionsEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void directOptionsWithReferenceDateRunTheRealQueryAndReturnTheSeededCatalog() throws Exception {
        mockMvc.perform(get("/catalog-options/direct")
                        .param("ruleSystemCode", "ESP")
                        .param("ruleEntityTypeCode", "EMPLOYEE_PRESENCE_ENTRY_REASON")
                        .param("referenceDate", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleSystemCode").value("ESP"))
                .andExpect(jsonPath("$.ruleEntityTypeCode").value("EMPLOYEE_PRESENCE_ENTRY_REASON"))
                .andExpect(jsonPath("$.referenceDate").value("2026-06-01"))
                .andExpect(jsonPath("$.items.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.items[?(@.code == 'HIRING')].name").value("Hiring"));
    }
}
