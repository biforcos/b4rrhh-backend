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
 * Extremo a extremo (backend#37): el menú de maestros lee de aquí su pertenencia, grupo,
 * orden y modo (ADR-054 §8, b4rrhh/frontend#33). Contra el esquema real: lo que se afirma
 * es la clasificación que sembró la V111, no un mock que repite lo que le digan.
 */
@TestWebSobreEsquemaReal
class RuleEntityTypeMetadataEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void theMenuMetadataComesFromTheModel() throws Exception {
        mockMvc.perform(get("/rule-entity-types"))
                .andExpect(status().isOk())
                // la lista sale en el orden del menú: primero Organización (1)
                .andExpect(jsonPath("$[0].group.displayOrder").value(1))
                // el tipo cerrado, con su clase y su grupo
                .andExpect(jsonPath("$[?(@.code == 'EMPLOYEE_TYPE')].maintenanceMode").value("CLOSED"))
                .andExpect(jsonPath("$[?(@.code == 'EMPLOYEE_TYPE')].literalClass").value("DOMAIN_VOCABULARY"))
                .andExpect(jsonPath("$[?(@.code == 'EMPLOYEE_TYPE')].group.name").value("Organización"))
                // una cita reglamentaria de referencia, en Sociedad y con su orden
                .andExpect(jsonPath("$[?(@.code == 'CONTRACT')].maintenanceMode").value("REFERENCE"))
                .andExpect(jsonPath("$[?(@.code == 'CONTRACT')].group.code").value("SOCIETY"))
                .andExpect(jsonPath("$[?(@.code == 'CONTRACT')].group.displayOrder").value(2))
                // y un nombre propio que se mantiene
                .andExpect(jsonPath("$[?(@.code == 'COMPANY')].literalClass").value("PROPER_NOUN"))
                .andExpect(jsonPath("$[?(@.code == 'COMPANY')].maintenanceMode").value("MAINTAINED"));
    }
}
