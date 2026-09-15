package com.b4rrhh.payroll_engine.object.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El alta de ranuras, que hasta el backend#98 se llamaba «crear una tabla» y colgaba de /tables.
 *
 * <p>Este test no existia: el POST no tenia ninguno de controlador, solo de servicio. Y lo que
 * afirma es justo lo que el nombre viejo escondia: <b>que lo creado NO es una tabla</b>. Se crea una
 * ranura y acto seguido se pregunta por las tablas del sistema de reglas; si apareciera ahi, el
 * nombre nuevo estaria tan mal como el viejo.
 */
@TestWebSobreEsquemaReal
class PayrollBindingRoleControllerTest {

    private static final String RULE_SYSTEM_CODE = "ESP";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void creaLaRanuraYLaDevuelveConSuCodigo() throws Exception {
        mockMvc.perform(post("/payroll-engine/{ruleSystemCode}/binding-roles", RULE_SYSTEM_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bindingRoleCode\":\"P03_DAILY_AMOUNT_TABLE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruleSystemCode").value(RULE_SYSTEM_CODE))
                .andExpect(jsonPath("$.bindingRoleCode").value("P03_DAILY_AMOUNT_TABLE"));
    }

    /**
     * La afirmacion que da sentido al renombrado: una ranura recien creada no es una tabla y no
     * sale en la lista de tablas. No saldra hasta que una vinculacion por convenio le ate una que
     * si tenga filas, y eso ocurre en otro sitio (ADR-063).
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void loCreadoNoApareceEntreLasTablas() throws Exception {
        mockMvc.perform(post("/payroll-engine/{ruleSystemCode}/binding-roles", RULE_SYSTEM_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bindingRoleCode\":\"P04_DAILY_AMOUNT_TABLE\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/payroll-engine/{ruleSystemCode}/tables", RULE_SYSTEM_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tableCode", not(org.hamcrest.Matchers.hasItem("P04_DAILY_AMOUNT_TABLE"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void laMismaRanuraDosVecesEsUnConflicto() throws Exception {
        mockMvc.perform(post("/payroll-engine/{ruleSystemCode}/binding-roles", RULE_SYSTEM_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bindingRoleCode\":\"P05_DAILY_AMOUNT_TABLE\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/payroll-engine/{ruleSystemCode}/binding-roles", RULE_SYSTEM_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bindingRoleCode\":\"P05_DAILY_AMOUNT_TABLE\"}"))
                .andExpect(status().isConflict());
    }

    /** La que ya existe en las migraciones de ESP tambien choca, y por el mismo motivo. */
    @Test
    @WithMockUser(roles = "ADMIN")
    void laRanuraQueYaTraen_lasMigraciones_tambienChoca() throws Exception {
        mockMvc.perform(post("/payroll-engine/{ruleSystemCode}/binding-roles", RULE_SYSTEM_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bindingRoleCode\":\"P02_DAILY_AMOUNT_TABLE\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sinCodigoNoSeCreaNada() throws Exception {
        mockMvc.perform(post("/payroll-engine/{ruleSystemCode}/binding-roles", RULE_SYSTEM_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bindingRoleCode\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
