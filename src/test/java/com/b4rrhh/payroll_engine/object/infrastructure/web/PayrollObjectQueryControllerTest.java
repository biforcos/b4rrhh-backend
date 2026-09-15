package com.b4rrhh.payroll_engine.object.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El catalogo de objetos, que desde el b4rrhh/designer#11 esta en el contrato.
 *
 * <p>Estaba servido y no declarado, y el designer lo llamaba igual con la ruta escrita a mano.
 *
 * <p>Los dos casos se comprobaron antes de escribirlos en el contrato, y el segundo por poco: el
 * controlador no tiene manejador propio, asi que parecia que un tipo desconocido saldria en un 500
 * mudo. No: {@code PayrollConceptManagementExceptionHandler} es un advice <b>de paquete</b> sobre
 * todo {@code com.b4rrhh.payroll_engine} y ya recogia el {@code IllegalArgumentException}. Este
 * test es lo que lo dejo ver, y lo que impide que alguien estreche aquel advice sin enterarse de
 * que esta ruta dependia de el.
 */
@TestWebSobreEsquemaReal
class PayrollObjectQueryControllerTest {

    private static final String RULE_SYSTEM_CODE = "ESP";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void listaLasRanurasDeTipoTablaDeEsp() throws Exception {
        mockMvc.perform(get("/payroll-engine/{ruleSystemCode}/objects", RULE_SYSTEM_CODE)
                        .param("type", "TABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].objectCode", hasItem("P02_DAILY_AMOUNT_TABLE")))
                .andExpect(jsonPath("$[*].objectTypeCode", everyItem(is("TABLE"))))
                .andExpect(jsonPath("$[*].ruleSystemCode", everyItem(is(RULE_SYSTEM_CODE))));
    }

    /** Y un tipo que no existe se contesta con palabras, no con un 500 mudo. */
    @Test
    @WithMockUser(roles = "ADMIN")
    void unTipoQueNoExisteSeContestaConUn400QueDiceAlgo() throws Exception {
        mockMvc.perform(get("/payroll-engine/{ruleSystemCode}/objects", RULE_SYSTEM_CODE)
                        .param("type", "BANANA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
