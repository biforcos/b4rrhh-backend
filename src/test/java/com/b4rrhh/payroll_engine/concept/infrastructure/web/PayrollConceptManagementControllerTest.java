package com.b4rrhh.payroll_engine.concept.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Extremo a extremo de verdad: la peticion entra por MockMvc y el concepto se
// escribe y se relee de la base, sin nada mockeado. Su sitio es la franja E2E
// (backend#31), no un H2 disfrazado de Postgres (#2).
//
// Antes sembraba a mano ESP y "un concepto igual al 101 de la V71". Sobre el
// esquema real ambos vienen de las migraciones (V49, V71): el 409 se afirma
// contra el 101 que produccion tiene, y no contra una copia.
@TestWebSobreEsquemaReal
class PayrollConceptManagementControllerTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EXISTING_SEEDED_CONCEPT_CODE = "101";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void listConcepts_returnsExistingConcepts() throws Exception {
        mockMvc.perform(get("/payroll-engine/{ruleSystemCode}/concepts", RULE_SYSTEM_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].ruleSystemCode").value(RULE_SYSTEM_CODE));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createConcept_returns201WithCreatedConcept() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("conceptCode", "TEST201");
        request.put("conceptMnemonic", "PLUS_TRANSPORTE");
        request.put("calculationType", "RATE_BY_QUANTITY");
        request.put("functionalNature", "EARNING");
        request.put("resultCompositionMode", "ACCUMULATE");
        request.put("executionScope", "SEGMENT");

        mockMvc.perform(post("/payroll-engine/{ruleSystemCode}/concepts", RULE_SYSTEM_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conceptCode").value("TEST201"))
                .andExpect(jsonPath("$.calculationType").value("RATE_BY_QUANTITY"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createConcept_returns409WhenCodeAlreadyExists() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("conceptCode", EXISTING_SEEDED_CONCEPT_CODE);
        request.put("conceptMnemonic", "DUPLICATE");
        request.put("calculationType", "DIRECT_AMOUNT");
        request.put("functionalNature", "TECHNICAL");
        request.put("resultCompositionMode", "REPLACE");
        request.put("executionScope", "SEGMENT");

        mockMvc.perform(post("/payroll-engine/{ruleSystemCode}/concepts", RULE_SYSTEM_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
