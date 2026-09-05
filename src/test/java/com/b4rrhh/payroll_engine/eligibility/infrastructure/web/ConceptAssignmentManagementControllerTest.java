package com.b4rrhh.payroll_engine.eligibility.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Extremo a extremo de verdad: la peticion entra por MockMvc y la asignacion se
// escribe, se actualiza, se borra y se relee de la base, sin nada mockeado. Su
// sitio es la franja E2E (backend#31), no un H2 disfrazado de Postgres (#2).
//
// El sistema de reglas no puede ser ESP: las migraciones le siembran
// concept_assignment (V73, V74, V77, V88, V91) y "la lista esta vacia cuando
// no hay ninguna" dejaria de ser verdad. TST no lo siembra nadie, y
// payroll_engine no tiene clave ajena a rulesystem.rule_system, asi que no
// hace falta insertarlo.
@TestWebSobreEsquemaReal
class ConceptAssignmentManagementControllerTest {

    private static final String RULE_SYSTEM_CODE = "TST";
    private static final String CONCEPT_CODE = "SALARIO_BASE";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("insert into payroll_engine.payroll_object "
                        + "(rule_system_code, object_type_code, object_code, created_at, updated_at) "
                        + "values (?, 'CONCEPT', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                RULE_SYSTEM_CODE, CONCEPT_CODE);
        Long objectId = jdbc.queryForObject(
                "select id from payroll_engine.payroll_object "
                        + "where rule_system_code = ? and object_type_code = 'CONCEPT' and object_code = ?",
                Long.class, RULE_SYSTEM_CODE, CONCEPT_CODE
        );
        jdbc.update("insert into payroll_engine.payroll_concept "
                        + "(object_id, concept_mnemonic, calculation_type, functional_nature, "
                        + "payslip_order_code, execution_scope, created_at, updated_at) "
                        + "values (?, 'SB', 'DIRECT_AMOUNT', 'EARNING', '101', 'PERIOD', "
                        + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                objectId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listAssignments_returnsEmptyArrayWhenNoneExist() throws Exception {
        mockMvc.perform(get("/payroll-engine/{rs}/assignments", RULE_SYSTEM_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAssignment_returns201WithPersistedRow() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("conceptCode", CONCEPT_CODE);
        body.put("validFrom", "2025-01-01");
        body.put("priority", 10);

        mockMvc.perform(post("/payroll-engine/{rs}/assignments", RULE_SYSTEM_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignmentCode").value(notNullValue()))
                .andExpect(jsonPath("$.conceptCode").value(CONCEPT_CODE))
                .andExpect(jsonPath("$.priority").value(10))
                .andExpect(jsonPath("$.ruleSystemCode").value(RULE_SYSTEM_CODE));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAssignment_returns404WhenConceptDoesNotExist() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("conceptCode", "DOES_NOT_EXIST");
        body.put("validFrom", "2025-01-01");
        body.put("priority", 0);

        mockMvc.perform(post("/payroll-engine/{rs}/assignments", RULE_SYSTEM_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAssignment_returns200WithUpdatedFields() throws Exception {
        Map<String, Object> createBody = new LinkedHashMap<>();
        createBody.put("conceptCode", CONCEPT_CODE);
        createBody.put("validFrom", "2025-01-01");
        createBody.put("priority", 10);

        var created = mockMvc.perform(post("/payroll-engine/{rs}/assignments", RULE_SYSTEM_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn();

        String assignmentCode = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("assignmentCode").asText();

        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("validFrom", "2025-06-01");
        updateBody.put("validTo", "2026-12-31");
        updateBody.put("priority", 99);
        updateBody.put("companyCode", "EMP1");

        mockMvc.perform(put("/payroll-engine/{rs}/assignments/{code}", RULE_SYSTEM_CODE, assignmentCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentCode").value(assignmentCode))
                .andExpect(jsonPath("$.conceptCode").value(CONCEPT_CODE))
                .andExpect(jsonPath("$.validFrom").value("2025-06-01"))
                .andExpect(jsonPath("$.validTo").value("2026-12-31"))
                .andExpect(jsonPath("$.priority").value(99))
                .andExpect(jsonPath("$.companyCode").value("EMP1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAssignment_returns404WhenAssignmentCodeIsUnknown() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("validFrom", "2025-01-01");
        body.put("priority", 5);

        mockMvc.perform(put("/payroll-engine/{rs}/assignments/{code}", RULE_SYSTEM_CODE, "999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAssignment_returns404WhenAssignmentCodeIsNotNumeric() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("validFrom", "2025-01-01");
        body.put("priority", 5);

        mockMvc.perform(put("/payroll-engine/{rs}/assignments/{code}", RULE_SYSTEM_CODE, "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAssignment_returns404WhenAssignmentCodeIsUnknown() throws Exception {
        mockMvc.perform(delete("/payroll-engine/{rs}/assignments/{code}",
                        RULE_SYSTEM_CODE, "999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAssignment_returns404WhenAssignmentCodeIsNotNumeric() throws Exception {
        mockMvc.perform(delete("/payroll-engine/{rs}/assignments/{code}",
                        RULE_SYSTEM_CODE, "not-a-uuid"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAssignment_returns204AndRemovesRow() throws Exception {
        // POST to create an assignment
        Map<String, Object> createBody = new LinkedHashMap<>();
        createBody.put("conceptCode", CONCEPT_CODE);
        createBody.put("validFrom", "2026-01-01");
        createBody.put("priority", 5);

        var result = mockMvc.perform(post("/payroll-engine/{rs}/assignments", RULE_SYSTEM_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract the assignmentCode from the response
        var responseBody = objectMapper.readTree(result.getResponse().getContentAsString());
        String assignmentCode = responseBody.get("assignmentCode").asText();

        // DELETE it
        mockMvc.perform(delete("/payroll-engine/{rs}/assignments/{code}",
                        RULE_SYSTEM_CODE, assignmentCode))
                .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/payroll-engine/{rs}/assignments", RULE_SYSTEM_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.assignmentCode == '" + assignmentCode + "')]").doesNotExist());
    }
}
