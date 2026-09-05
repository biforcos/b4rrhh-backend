package com.b4rrhh.payroll_engine.concept.infrastructure.web;

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
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Extremo a extremo de verdad: la peticion entra por MockMvc y los operandos y
// feeds se escriben y se releen de la base. No hay nada mockeado, asi que su
// sitio es la franja E2E (backend#31), no un H2 disfrazado de Postgres (#2).
//
// ESP viene sembrado por las migraciones y no se vuelve a insertar. Los tres
// objetos de este test (SALARIO_BASE, T_DIAS_PRESENCIA, T_PRECIO_DIA como
// object_code) no los siembra nadie: los conceptos reales van por numero (101,
// 700, ...) y SALARIO_BASE es solo el mnemonico del 101.
//
// Los ambitos no son decorativos: SALARIO_BASE es SEGMENT y lee una cantidad
// SEGMENT (los dias de presencia) y un precio PERIOD. Al reves —un concepto
// PERIOD leyendo un operando SEGMENT— es la arista que ADR-058 prohibe, y el
// ultimo test de operandos comprueba que el PUT la rechaza.
@TestWebSobreEsquemaReal
class ConceptWiringControllerTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String TARGET_CONCEPT_CODE = "SALARIO_BASE";
    private static final String SOURCE_QUANTITY_CODE = "T_DIAS_PRESENCIA";
    private static final String SOURCE_RATE_CODE = "T_PRECIO_DIA";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        seedConcept(TARGET_CONCEPT_CODE, "SALARIO_BASE", "RATE_BY_QUANTITY",
                "EARNING", "SEGMENT");
        seedConcept(SOURCE_QUANTITY_CODE, "T_DIAS_PRESENCIA", "DIRECT_AMOUNT",
                "TECHNICAL", "SEGMENT");
        seedConcept(SOURCE_RATE_CODE, "T_PRECIO_DIA", "DIRECT_AMOUNT",
                "TECHNICAL", "PERIOD");
    }

    private void seedConcept(String objectCode, String mnemonic, String calculationType,
                             String functionalNature, String executionScope) {
        jdbc.update("insert into payroll_engine.payroll_object "
                        + "(rule_system_code, object_type_code, object_code, created_at, updated_at) "
                        + "values (?, 'CONCEPT', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                RULE_SYSTEM_CODE, objectCode);
        Long objectId = jdbc.queryForObject(
                "select id from payroll_engine.payroll_object "
                        + "where rule_system_code = ? and object_type_code = 'CONCEPT' and object_code = ?",
                Long.class, RULE_SYSTEM_CODE, objectCode
        );
        jdbc.update("insert into payroll_engine.payroll_concept "
                        + "(object_id, concept_mnemonic, calculation_type, functional_nature, "
                        + "payslip_order_code, execution_scope, created_at, updated_at) "
                        + "values (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                objectId, mnemonic, calculationType, functionalNature,
                objectCode, executionScope);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listOperands_returnsEmptyArrayWhenNoneConfigured() throws Exception {
        mockMvc.perform(get("/payroll-engine/{rs}/concepts/{c}/operands",
                        RULE_SYSTEM_CODE, TARGET_CONCEPT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listOperands_returns404WhenConceptDoesNotExist() throws Exception {
        mockMvc.perform(get("/payroll-engine/{rs}/concepts/{c}/operands",
                        RULE_SYSTEM_CODE, "DOES_NOT_EXIST"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void putOperands_replacesAllAndReturnsPersistedList() throws Exception {
        Map<String, Object> quantity = new LinkedHashMap<>();
        quantity.put("operandRole", "QUANTITY");
        quantity.put("sourceObjectCode", SOURCE_QUANTITY_CODE);
        Map<String, Object> rate = new LinkedHashMap<>();
        rate.put("operandRole", "RATE");
        rate.put("sourceObjectCode", SOURCE_RATE_CODE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("operands", List.of(quantity, rate));

        mockMvc.perform(put("/payroll-engine/{rs}/concepts/{c}/operands",
                        RULE_SYSTEM_CODE, TARGET_CONCEPT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].operandRole").value("QUANTITY"))
                .andExpect(jsonPath("$[0].sourceObjectCode").value(SOURCE_QUANTITY_CODE))
                .andExpect(jsonPath("$[1].operandRole").value("RATE"))
                .andExpect(jsonPath("$[1].sourceObjectCode").value(SOURCE_RATE_CODE));

        // Subsequent GET sees the same persisted list
        mockMvc.perform(get("/payroll-engine/{rs}/concepts/{c}/operands",
                        RULE_SYSTEM_CODE, TARGET_CONCEPT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void putOperands_emptyListClearsAllOperands() throws Exception {
        // First PUT a non-empty operand list
        Map<String, Object> quantity = new LinkedHashMap<>();
        quantity.put("operandRole", "QUANTITY");
        quantity.put("sourceObjectCode", SOURCE_QUANTITY_CODE);

        Map<String, Object> nonEmptyBody = new LinkedHashMap<>();
        nonEmptyBody.put("operands", List.of(quantity));

        mockMvc.perform(put("/payroll-engine/{rs}/concepts/{c}/operands",
                        RULE_SYSTEM_CODE, TARGET_CONCEPT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonEmptyBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Then PUT an empty operands list
        Map<String, Object> emptyBody = new LinkedHashMap<>();
        emptyBody.put("operands", List.of());

        mockMvc.perform(put("/payroll-engine/{rs}/concepts/{c}/operands",
                        RULE_SYSTEM_CODE, TARGET_CONCEPT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // GET operands and assert the list is empty
        mockMvc.perform(get("/payroll-engine/{rs}/concepts/{c}/operands",
                        RULE_SYSTEM_CODE, TARGET_CONCEPT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void putOperands_returns422WhenSourceDoesNotExist() throws Exception {
        Map<String, Object> bogus = new LinkedHashMap<>();
        bogus.put("operandRole", "QUANTITY");
        bogus.put("sourceObjectCode", "NOT_A_CONCEPT");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("operands", List.of(bogus));

        mockMvc.perform(put("/payroll-engine/{rs}/concepts/{c}/operands",
                        RULE_SYSTEM_CODE, TARGET_CONCEPT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void putOperands_returns422WhenPeriodConceptReadsSegmentOperand() throws Exception {
        // T_PRECIO_DIA es PERIOD; T_DIAS_PRESENCIA es SEGMENT. Un precio del mes no
        // puede leer "los dias del tramo": no es un numero (ADR-058).
        Map<String, Object> segmentQuantity = new LinkedHashMap<>();
        segmentQuantity.put("operandRole", "QUANTITY");
        segmentQuantity.put("sourceObjectCode", SOURCE_QUANTITY_CODE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("operands", List.of(segmentQuantity));

        mockMvc.perform(put("/payroll-engine/{rs}/concepts/{c}/operands",
                        RULE_SYSTEM_CODE, SOURCE_RATE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("QUANTITY"),
                        containsString("ESP/" + SOURCE_RATE_CODE),
                        containsString("ESP/" + SOURCE_QUANTITY_CODE))));

        // La arista prohibida no se ha guardado
        mockMvc.perform(get("/payroll-engine/{rs}/concepts/{c}/operands",
                        RULE_SYSTEM_CODE, SOURCE_RATE_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void putFeeds_replacesAllAndReturnsPersistedList() throws Exception {
        Map<String, Object> feed = new LinkedHashMap<>();
        feed.put("sourceObjectCode", SOURCE_QUANTITY_CODE);
        feed.put("invertSign", false);
        feed.put("effectiveFrom", "2025-01-01");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("feeds", List.of(feed));

        mockMvc.perform(put("/payroll-engine/{rs}/concepts/{c}/feeds",
                        RULE_SYSTEM_CODE, TARGET_CONCEPT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sourceObjectCode").value(SOURCE_QUANTITY_CODE))
                .andExpect(jsonPath("$[0].invertSign").value(false))
                .andExpect(jsonPath("$[0].effectiveFrom").value("2025-01-01"));
    }
}
