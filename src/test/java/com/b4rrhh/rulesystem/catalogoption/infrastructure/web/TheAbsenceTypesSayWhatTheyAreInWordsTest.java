package com.b4rrhh.rulesystem.catalogoption.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los tipos de ausencia dicen lo que son en palabras, con la cita detrás (backend#173).
 *
 * <p>En la sesión de workspace#18: «vale que el nombre legal de la enfermedad es IT común… pero qué
 * poco intuitivo». La cita es el nombre y no se traduce (ADR-054 §3); lo que se sirve además es su
 * etiqueta común, para ponerla delante.</p>
 */
@TestWebSobreEsquemaReal
class TheAbsenceTypesSayWhatTheyAreInWordsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void theCommonSickLeaveIsASickLeaveAndStillItsCitation() throws Exception {
        mockMvc.perform(get("/catalog-options/direct")
                        .param("ruleSystemCode", "ESP")
                        .param("ruleEntityTypeCode", "EMPLOYEE_ABSENCE_TYPE")
                        .header("Accept-Language", "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.code == 'IT_COMMON')].commonLabel",
                        contains(startsWith("Baja por enfermedad"))))
                .andExpect(jsonPath("$.items[?(@.code == 'IT_COMMON')].name",
                        contains("IT Contingencia Común")))
                // Lo que ya está en palabras no lleva etiqueta común.
                .andExpect(jsonPath("$.items[?(@.code == 'VACATION')].commonLabel", contains((Object) null)));
    }
}
