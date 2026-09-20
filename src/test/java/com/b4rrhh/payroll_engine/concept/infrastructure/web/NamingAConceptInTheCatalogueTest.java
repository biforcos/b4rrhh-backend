package com.b4rrhh.payroll_engine.concept.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ponerle nombre a un concepto desde el catalogo ({@code backend#109}).
 *
 * <p>El endpoint existe porque el nombre tiene que poder cambiarse: sin el, «el literal no se
 * mueve al tocar el catalogo» seria una afirmacion que no se puede provocar, y una promesa que
 * nadie puede romper tampoco se puede comprobar.
 */
@TestWebSobreEsquemaReal
class NamingAConceptInTheCatalogueTest {

    private static final String RULE_SYSTEM = "ESP";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void theListServesTheNameAndTheMnemonicAsTwoDifferentThings() throws Exception {
        mockMvc.perform(get("/payroll-engine/{rs}/concepts", RULE_SYSTEM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.conceptCode == '101')].conceptMnemonic")
                        .value("SALARIO_BASE"))
                .andExpect(jsonPath("$[?(@.conceptCode == '101')].label")
                        .value("Salario base"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void namingAConceptReplacesTheNameItHad() throws Exception {
        mockMvc.perform(patch("/payroll-engine/{rs}/concepts/{code}/label", RULE_SYSTEM, "102")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Horas extraordinarias de fuerza mayor\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Horas extraordinarias de fuerza mayor"))
                // El mnemonico no se ha movido: es el identificador y no es lo que se estaba
                // cambiando. Dos campos, dos trabajos.
                .andExpect(jsonPath("$.conceptMnemonic").value("IMPORTE_HORAS_EXTRA"));

        // Y no ha estrenado una segunda fila: el nombre de un concepto en un idioma es uno.
        Integer filas = jdbc.queryForObject("""
                select count(*)
                  from payroll_engine.payroll_concept_label l
                  join payroll_engine.payroll_object o on o.id = l.object_id
                 where o.rule_system_code = ? and o.object_code = '102'
                """, Integer.class, RULE_SYSTEM);
        org.junit.jupiter.api.Assertions.assertEquals(1, filas);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aBlankNameIsRejectedBecauseItIsAGapAndNotAnAbsence() throws Exception {
        mockMvc.perform(patch("/payroll-engine/{rs}/concepts/{code}/label", RULE_SYSTEM, "101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void namingAConceptThatDoesNotExistIsNotFound() throws Exception {
        mockMvc.perform(patch("/payroll-engine/{rs}/concepts/{code}/label", RULE_SYSTEM, "NO_EXISTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Un nombre sin concepto\"}"))
                .andExpect(status().isNotFound());
    }
}
