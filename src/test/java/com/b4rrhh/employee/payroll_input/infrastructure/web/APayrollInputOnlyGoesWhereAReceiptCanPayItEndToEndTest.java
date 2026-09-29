package com.b4rrhh.employee.payroll_input.infrastructure.web;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Una entrada de nómina sólo se acepta donde un recibo puede pagarla (b4rrhh/backend#142).
 *
 * <p>El revisor de la demo: «nada me impide meter un concepto de nómina variable (las horas extras)
 * a un período en el que el empleado no está de alta (al menos un día)». Esa entrada no tiene
 * recibo que la consuma: ni se calcula ni se paga, y nadie lo dice. Rechazarla cuesta un mensaje;
 * aceptarla cuesta una entrada que desaparece. Lo mismo, medido desde el b4rrhh/frontend#92, para
 * un empleado que no existe y un concepto que no existe: las dos daban 201 y dejaban la fila.
 */
@TestWebSobreEsquemaReal
class APayrollInputOnlyGoesWhereAReceiptCanPayItEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String employeeNumber;

    @BeforeEach
    void hireFromTheFifteenthOfApril() throws Exception {
        employeeNumber = JsonPath.read(mockMvc.perform(post("/employees/hire")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DatosDePrueba.conDni("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                  "hireDate": "2026-04-15",
                                  "companyCode": "ES01",
                                  "workCenterCode": "MAIN_OFFICE",
                                  "costCenterDistribution": { "items": [ { "costCenterCode": "CC_ADMIN", "allocationPercentage": 100.0 } ] },
                                  "contract": { "contractTypeCode": "100", "contractSubtypeCode": "01" },
                                  "laborClassification": { "agreementCode": "99002405011982", "agreementCategoryCode": "99002405-G3" },
                                  "workingTime": { "workingTimePercentage": 100 }
                                }
                                """)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.employeeNumber");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aMonthWithoutASingleDayOfPresenceIsRefusedNamingThePresences() throws Exception {
        mockMvc.perform(post("/employees/ESP/INTERNAL/{n}/payroll-inputs", employeeNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conceptCode\":\"H01\",\"period\":202603,\"quantity\":10}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAYROLL_INPUT_OUTSIDE_PRESENCE"))
                .andExpect(jsonPath("$.message").value(containsString("desde el 15/04/2026")))
                .andExpect(jsonPath("$.message").value(containsString("202603 no tiene ni un día")));

        assertThat(rows()).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aMonthWithOneDayOfPresenceIsAccepted() throws Exception {
        mockMvc.perform(post("/employees/ESP/INTERNAL/{n}/payroll-inputs", employeeNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conceptCode\":\"H01\",\"period\":202604,\"quantity\":10}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aCorrectionIsCheckedToo() throws Exception {
        jdbcTemplate.update("""
                insert into employee.employee_payroll_input
                    (rule_system_code, employee_type_code, employee_number, concept_code, period, quantity)
                values ('ESP', 'INTERNAL', ?, 'H01', 202602, 5)
                """, employeeNumber);

        mockMvc.perform(put("/employees/ESP/INTERNAL/{n}/payroll-inputs/H01", employeeNumber)
                        .param("period", "202602")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":8}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAYROLL_INPUT_OUTSIDE_PRESENCE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anEmployeeThatDoesNotExistIsA404() throws Exception {
        mockMvc.perform(post("/employees/ESP/INTERNAL/NOEXISTE/payroll-inputs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conceptCode\":\"H01\",\"period\":202609,\"quantity\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("NOEXISTE")));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from employee.employee_payroll_input where employee_number = 'NOEXISTE'",
                Integer.class)).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aConceptThatDoesNotExistIsRefusedNamingIt() throws Exception {
        mockMvc.perform(post("/employees/ESP/INTERNAL/{n}/payroll-inputs", employeeNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conceptCode\":\"NOPE\",\"period\":202604,\"quantity\":1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAYROLL_INPUT_CONCEPT_INVALID"))
                .andExpect(jsonPath("$.message").value(containsString("NOPE")));

        assertThat(rows()).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aConceptThatIsCalculatedIsNotAnInput() throws Exception {
        String calculated = jdbcTemplate.queryForObject("""
                select o.object_code
                  from payroll_engine.payroll_concept c
                  join payroll_engine.payroll_object o on o.id = c.object_id
                 where o.rule_system_code = 'ESP' and c.calculation_type <> 'EMPLOYEE_INPUT'
                 order by o.object_code
                 limit 1
                """, String.class);

        mockMvc.perform(post("/employees/ESP/INTERNAL/{n}/payroll-inputs", employeeNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conceptCode\":\"" + calculated + "\",\"period\":202604,\"quantity\":1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAYROLL_INPUT_CONCEPT_INVALID"))
                .andExpect(jsonPath("$.message").value(containsString("no es de entrada")));
    }

    private int rows() {
        return jdbcTemplate.queryForObject(
                "select count(*) from employee.employee_payroll_input where employee_number = ?",
                Integer.class, employeeNumber);
    }
}
