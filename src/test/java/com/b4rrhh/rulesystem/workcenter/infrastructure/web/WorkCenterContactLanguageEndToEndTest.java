package com.b4rrhh.rulesystem.workcenter.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Extremo a extremo (backend#27): el tipo de contacto de centro de trabajo con
 * {@code Accept-Language} sirviendo una traduccion sembrada, contra el esquema real. Este
 * vertical resolvia el literal dentro de los casos de uso y lo metia en el modelo de
 * dominio; ahora la conversion vive en el assembler de la capa web, y este test afirma que
 * el idioma viaja de la cabecera al resolutor de verdad: sin cabecera, el literal base;
 * con ella, la traduccion.
 */
@TestWebSobreEsquemaReal
class WorkCenterContactLanguageEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(roles = "ADMIN")
    void contactsServeTheSeededTranslationWhenAcceptLanguageAsksForIt() throws Exception {
        jdbcTemplate.update("""
                insert into rulesystem.rule_entity_translation (rule_entity_id, language_code, name)
                select id, 'es-ES', 'Correo electrónico' from rulesystem.rule_entity
                 where rule_system_code = 'ESP'
                   and rule_entity_type_code = 'CONTACT_TYPE'
                   and code = 'EMAIL'
                """);

        mockMvc.perform(post("/work-centers/ESP/MAIN_OFFICE/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contactTypeCode": "EMAIL",
                                  "contactValue": "hq@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contactTypeName").value("Email"));

        mockMvc.perform(get("/work-centers/ESP/MAIN_OFFICE/contacts")
                        .header("Accept-Language", "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contactTypeCode").value("EMAIL"))
                .andExpect(jsonPath("$[0].contactTypeName").value("Correo electrónico"));

        // Sin cabecera, el literal base: la traduccion solo sale cuando se pide.
        mockMvc.perform(get("/work-centers/ESP/MAIN_OFFICE/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contactTypeName").value("Email"));
    }
}
