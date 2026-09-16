package com.b4rrhh.payroll_engine.table.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contra el esquema real, que es donde las cuatro tablas de ESP y sus cuatro
 * vinculaciones vienen de las migraciones (V66, V67, V69, V70, V75, V133) y no
 * de una copia sembrada aqui.
 *
 * La cuarta es P03_99002405011982, el precio de la hora extra por categoria, y
 * la trae el backend#104. Que este recuento se mueva al declarar una tabla es
 * justo lo que se le pide: dice cuantas hay, no cuantas habia.
 *
 * Lo que se afirma es la diferencia que abrio el backend#95: esto lista
 * TABLAS, y la ranura P02_DAILY_AMOUNT_TABLE -que es lo unico que devuelve
 * objects?type=TABLE- no es una de ellas.
 */
@TestWebSobreEsquemaReal
class PayrollTableListingControllerTest {

    private static final String RULE_SYSTEM_CODE = "ESP";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void listaLasCuatroTablasDeEspConSusFilasYSuVinculacion() throws Exception {
        mockMvc.perform(get("/payroll-engine/{ruleSystemCode}/tables", RULE_SYSTEM_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tableCode", contains(
                        "P02_99002405011982", "P03_99002405011982",
                        "PC_99002405011982", "SB_99002405011982")))
                .andExpect(jsonPath("$[*].ruleSystemCode", everyItem(org.hamcrest.Matchers.is(RULE_SYSTEM_CODE))))
                .andExpect(jsonPath("$[?(@.tableCode == 'SB_99002405011982')].rowCount", contains(3)))
                .andExpect(jsonPath("$[?(@.tableCode == 'SB_99002405011982')].activeRowCount", contains(3))
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cadaTablaDiceQuienLaAtaYConQueRol() throws Exception {
        mockMvc.perform(get("/payroll-engine/{ruleSystemCode}/tables", RULE_SYSTEM_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tableCode == 'SB_99002405011982')].bindings[0].bindingRoleCode",
                        contains("BASE_SALARY_TABLE")))
                .andExpect(jsonPath("$[?(@.tableCode == 'SB_99002405011982')].bindings[0].ownerTypeCode",
                        contains("AGREEMENT")))
                .andExpect(jsonPath("$[?(@.tableCode == 'SB_99002405011982')].bindings[0].ownerCode",
                        contains("99002405011982")))
                .andExpect(jsonPath("$[?(@.tableCode == 'PC_99002405011982')].bindings[0].bindingRoleCode",
                        contains("AGREEMENT_PLUS_TABLE")))
                .andExpect(jsonPath("$[?(@.tableCode == 'P02_99002405011982')].bindings[0].bindingRoleCode",
                        contains("P02_DAILY_AMOUNT_TABLE")))
                .andExpect(jsonPath("$[?(@.tableCode == 'P03_99002405011982')].bindings[0].bindingRoleCode",
                        contains("P03_HOURLY_OVERTIME_TABLE")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void laRanuraNoSaleComoTabla_queEsLoQueVeniaViendoElDesigner() throws Exception {
        mockMvc.perform(get("/payroll-engine/{ruleSystemCode}/tables", RULE_SYSTEM_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tableCode", not(hasItem("P02_DAILY_AMOUNT_TABLE"))))
                .andExpect(jsonPath("$[*].tableCode", not(empty())));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unSistemaDeReglasSinTablasDevuelveListaVacia() throws Exception {
        mockMvc.perform(get("/payroll-engine/{ruleSystemCode}/tables", "FRA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }
}
