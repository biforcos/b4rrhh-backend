package com.b4rrhh.rulesystem.catalogoption.infrastructure.web;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Extremo a extremo: las opciones de catálogo con {@code referenceDate} contra el esquema
 * real.
 *
 * Nació en el backend#31 afirmando que la fecha **filtraba**, que era el comportamiento de
 * entonces y era correcto afirmarlo. El backend#32 lo derogó: la fecha **fecha la vigencia**,
 * y el código no vigente sigue apareciendo, marcado. Lo que se reescribe es lo que afirma, no
 * lo que ejercita — sigue siendo el caso que esta franja tenía que cubrir, porque esta misma
 * consulta daba 500 en Postgres con la fecha puesta (#30) y un repositorio mockeado no puede
 * verlo.
 */
@TestWebSobreEsquemaReal
class DirectCatalogOptionsEndToEndTest {

    private static final String TYPE = "EMPLOYEE_PRESENCE_ENTRY_REASON";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DatosDePrueba.ruleEntity(jdbcTemplate, TYPE, "TST_CLOSED", "Closed in 2020",
                LocalDate.of(1900, 1, 1), LocalDate.of(2020, 12, 31));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void directOptionsWithReferenceDateRunTheRealQueryAndReturnTheWholeCatalogMarked() throws Exception {
        mockMvc.perform(get("/catalog-options/direct")
                        .param("ruleSystemCode", "ESP")
                        .param("ruleEntityTypeCode", TYPE)
                        .param("referenceDate", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleSystemCode").value("ESP"))
                .andExpect(jsonPath("$.ruleEntityTypeCode").value(TYPE))
                .andExpect(jsonPath("$.referenceDate").value("2026-06-01"))
                .andExpect(jsonPath("$.items.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.items[?(@.code == 'HIRING')].name").value("Hiring"))
                // Vigente ese día.
                .andExpect(jsonPath("$.items[?(@.code == 'HIRING')].active").value(true))
                // Cerrado en 2020: sigue en la lista, y marcado como no vigente. Esto es lo
                // que el backend#32 cambia — antes no aparecía.
                .andExpect(jsonPath("$.items[?(@.code == 'TST_CLOSED')].active").value(false))
                .andExpect(jsonPath("$.items[?(@.code == 'TST_CLOSED')].endDate").value("2020-12-31"));
    }
}
