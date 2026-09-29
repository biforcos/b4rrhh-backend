package com.b4rrhh.rulesystem.catalogoption.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los desplegables de motivos hablan el idioma que se les pide (b4rrhh/backend#143).
 *
 * <p>El revisor de la demo: «los motivos de entrada siguen en inglés (raro)». Las traducciones
 * estaban sembradas desde la V114 y el adaptador de opciones sabía leerlas desde el backend#24;
 * lo que faltaba era el cable: el controlador no leía {@code Accept-Language} y el caso de uso
 * llamaba al puerto sin idioma, así que todos los desplegables de la aplicación salían con el
 * literal base aunque el frontend mandase {@code es-ES} en cada petición.
 */
@TestWebSobreEsquemaReal
class TheReasonsOfTheDropdownsSpeakTheLanguageAskedTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void entryReasonsComeInSpanishWhenAskedInSpanish() throws Exception {
        mockMvc.perform(get("/catalog-options/direct")
                        .param("ruleSystemCode", "ESP")
                        .param("ruleEntityTypeCode", "EMPLOYEE_PRESENCE_ENTRY_REASON")
                        .header("Accept-Language", "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.code == 'HIRING')].name").value("Contratación"))
                .andExpect(jsonPath("$.items[?(@.code == 'REHIRE')].name").value("Readmisión"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void exitReasonsComeInSpanishWhenAskedInSpanish() throws Exception {
        mockMvc.perform(get("/catalog-options/direct")
                        .param("ruleSystemCode", "ESP")
                        .param("ruleEntityTypeCode", "EMPLOYEE_PRESENCE_EXIT_REASON")
                        .header("Accept-Language", "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.code == 'TERMINATION')].name").value("Cese"))
                .andExpect(jsonPath("$.items[?(@.code == 'RETIREMENT')].name").value("Jubilación"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void withoutTheHeaderTheBaseLiteralStays() throws Exception {
        mockMvc.perform(get("/catalog-options/direct")
                        .param("ruleSystemCode", "ESP")
                        .param("ruleEntityTypeCode", "EMPLOYEE_PRESENCE_ENTRY_REASON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.code == 'HIRING')].name").value("Hiring"));
    }
}
