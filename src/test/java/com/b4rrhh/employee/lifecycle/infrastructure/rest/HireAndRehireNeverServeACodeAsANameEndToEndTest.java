package com.b4rrhh.employee.lifecycle.infrastructure.rest;

import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.usecase.TerminateEmployeeUseCase;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El alta y la readmision no sirven el codigo en el hueco del nombre (backend#36).
 *
 * <p>Ponerlo era peor que dejarlo vacio: un null es una ausencia visible y el cliente decide
 * que hacer; el codigo metido ahi es una ausencia invisible, porque nadie puede saber si el
 * centro se llama asi de verdad o si es que no lo resolvio nadie.
 *
 * <p>El escenario usa a proposito codigos cuyo nombre en el catalogo <b>es distinto</b>:
 * {@code MAIN_OFFICE} se llama «Main Office» y {@code CC_ADMIN} se llama «Administration».
 * Con un fixture donde el codigo y el nombre coinciden, esta prueba no distinguiria nada, que
 * es como el defecto sobrevivio desde el 005c563.
 *
 * <p>La segunda peticion va con {@code Accept-Language} y con la traduccion sembrada: es la
 * forma fuerte de la afirmacion. No es que aqui no se pueda resolver el literal —el resolutor
 * lo tiene delante—, es que <b>no se resuelve</b>, porque la respuesta de un comando dice lo
 * que acaba de crear y no la ficha. Quien quiera el nombre lo pide a la vertical que lo sirve.
 */
@TestWebSobreEsquemaReal
class HireAndRehireNeverServeACodeAsANameEndToEndTest {

    private static final String WORK_CENTER_CODE = "MAIN_OFFICE";
    private static final String WORK_CENTER_NAME = "Main Office";
    private static final String COST_CENTER_CODE = "CC_ADMIN";
    private static final String COST_CENTER_NAME = "Administration";
    private static final String COST_CENTER_NAME_ES = "Administración";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TerminateEmployeeUseCase terminateEmployeeUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser(roles = "ADMIN")
    void hireServesTheWorkCenterCodeAndNoNameAtAll() throws Exception {
        String response = mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireBody(LocalDate.of(2026, 4, 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.initialWorkCenter.workCenterCode").value(WORK_CENTER_CODE))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode workCenter = objectMapper.readTree(response).get("initialWorkCenter");

        // Ni el nombre ni el codigo disfrazado de nombre: el campo no esta.
        assertThat(workCenter.has("workCenterName"))
                .as("el alta no declara workCenterName: no lo tiene y no lo inventa")
                .isFalse();
        assertThat(workCenter.toString())
                .doesNotContain(WORK_CENTER_NAME);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void hireLeavesTheCostCenterNameEmptyEvenWhenTheLabelIsThereToBeResolved() throws Exception {
        seedSpanishCostCenterTranslation();

        mockMvc.perform(post("/employees/hire")
                        .header("Accept-Language", "es-ES")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireBody(LocalDate.of(2026, 4, 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.costCenter.items[0].costCenterCode").value(COST_CENTER_CODE))
                // Ni el codigo, ni el literal base, ni la traduccion sembrada: null.
                .andExpect(jsonPath("$.costCenter.items[0].costCenterName").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rehireLeavesTheCostCenterNameEmptyToo() throws Exception {
        seedSpanishCostCenterTranslation();

        String hireResponse = mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hireBody(LocalDate.of(2026, 4, 3))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String employeeNumber = objectMapper.readTree(hireResponse).get("employeeNumber").asText();

        terminateEmployeeUseCase.terminate(new TerminateEmployeeCommand(
                "ESP", "INTERNAL", employeeNumber, LocalDate.of(2026, 5, 31), "TERMINATION"));

        mockMvc.perform(post("/employees/ESP/INTERNAL/{employeeNumber}/rehire", employeeNumber)
                        .header("Accept-Language", "es-ES")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(LocalDate.of(2026, 7, 1))))
                // 201 y no 200: la readmision creo una presencia nueva (result.created()).
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newCostCenter.items[0].costCenterCode").value(COST_CENTER_CODE))
                .andExpect(jsonPath("$.newCostCenter.items[0].costCenterName").doesNotExist());
    }

    /**
     * Con la traduccion sembrada, el mismo codigo tiene tres literales posibles en el recibo:
     * el codigo, el nombre base y la traduccion. Los tres son distintos entre si, asi que la
     * asercion de arriba solo pasa si de verdad no hay ninguno.
     */
    private void seedSpanishCostCenterTranslation() {
        jdbcTemplate.update("""
                insert into rulesystem.rule_entity_translation (rule_entity_id, language_code, name)
                select id, 'es-ES', ? from rulesystem.rule_entity
                 where rule_system_code = 'ESP'
                   and rule_entity_type_code = 'COST_CENTER'
                   and code = ?
                """, COST_CENTER_NAME_ES, COST_CENTER_CODE);
    }

    private String hireBody(LocalDate hireDate) {
        return """
                {
                  "ruleSystemCode": "ESP",
                  "employeeTypeCode": "INTERNAL",
                  "firstName": "Ana",
                  "lastName1": "Lopez",
                  "hireDate": "%s",
                  "entryReasonCode": "HIRING",
                  "companyCode": "ES01",
                  "workCenterCode": "%s",
                  "costCenterDistribution": { "items": [ { "costCenterCode": "%s", "allocationPercentage": 100.0 } ] },
                  "contract": { "contractTypeCode": "100", "contractSubtypeCode": "01" },
                  "laborClassification": { "agreementCode": "99002405011982", "agreementCategoryCode": "99002405-G3" },
                  "workingTime": { "workingTimePercentage": 100 }
                }
                """.formatted(hireDate, WORK_CENTER_CODE, COST_CENTER_CODE);
    }

    private String rehireBody(LocalDate rehireDate) {
        return """
                {
                  "rehireDate": "%s",
                  "entryReasonCode": "HIRING",
                  "companyCode": "ES01",
                  "laborClassification": { "agreementCode": "99002405011982", "agreementCategoryCode": "99002405-G3" },
                  "contract": { "contractTypeCode": "100", "contractSubtypeCode": "01" },
                  "workCenter": { "workCenterCode": "%s" },
                  "costCenterDistribution": { "items": [ { "costCenterCode": "%s", "allocationPercentage": 100.0 } ] },
                  "workingTime": { "workingTimePercentage": 100 }
                }
                """.formatted(rehireDate, WORK_CENTER_CODE, COST_CENTER_CODE);
    }
}
