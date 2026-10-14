package com.b4rrhh.rulesystem.workcenter.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un centro sin empresa (backend#167). La base la exige desde la V40
 * ({@code work_center_profile.company_code not null}) y el contrato la daba por opcional, así que el
 * alta sin ella llegaba hasta el {@code insert} y salía un 500: ni decía qué faltaba ni era verdad
 * que el servidor hubiera fallado. Ahora es un 400 que nombra el campo, en el alta y en la edición.
 */
@TestWebSobreEsquemaReal
@WithMockUser(roles = "ADMIN")
class AWorkCenterWithoutACompanyIsRejectedEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void creatingAWorkCenterWithoutACompanyIsABadRequestThatNamesTheField() throws Exception {
        mockMvc.perform(post("/work-centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleSystemCode":"ESP","workCenterCode":"ZZ167","name":"Sin empresa",
                                 "startDate":"2026-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("companyCode")));
    }

    // En la edición el test devolvía 200 sólo porque su transacción no llega a escribir; en
    // producción, al confirmar, saltaba el not null igual que en el alta.
    @Test
    void emptyingTheCompanyOfAWorkCenterIsABadRequestThatNamesTheField() throws Exception {
        mockMvc.perform(put("/work-centers/ESP/MAIN_OFFICE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Main Office","companyCode":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("companyCode")));
        mockMvc.perform(put("/work-centers/ESP/MAIN_OFFICE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Main Office"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("companyCode")));
    }
}
