package com.b4rrhh.payroll.infrastructure.web;

import com.b4rrhh.payroll.scenario.PayrollScenarioFixtures;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Invalidar no pide motivo (b4rrhh/backend#150).
 *
 * <p>La segunda revisión a distancia: «¿Y para qué vale el motivo, no parece que sirva para
 * nada?». No servía: el texto libre que se escribía en la caja moría en
 * {@code payroll.status_reason_code} y nada lo leía. Ahora la columna cuenta lo único que puede
 * contar con verdad, <b>qué camino</b> invalidó el recibo, y lo pone el servicio.
 */
@TestWebSobreEsquemaReal
class InvalidatingAsksForNoReasonEndToEndTest {

    private static final String PERIOD = "202501";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private PayrollScenarioFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new PayrollScenarioFixtures(jdbc);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void theBulkInvalidationLeavesTheRowMarkedAsBulk() throws Exception {
        calculatedPayrollOf("EMP150001");

        mockMvc.perform(post("/payrolls/invalidate-bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "payrollPeriodCode": "%s",
                                  "payrollTypeCode": "NORMAL",
                                  "targetSelection": {
                                    "selectionType": "SINGLE_EMPLOYEE",
                                    "employee": { "employeeTypeCode": "INTERNAL", "employeeNumber": "EMP150001" }
                                  }
                                }
                                """.formatted(PERIOD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvalidated").value(1))
                .andExpect(jsonPath("$.statusReasonCode").doesNotExist());

        assertThat(statusAndReasonOf("EMP150001")).isEqualTo("NOT_VALID:BULK_INVALIDATION");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void theSingleInvalidationLeavesTheRowMarkedAsManual() throws Exception {
        calculatedPayrollOf("EMP150002");

        mockMvc.perform(post("/payrolls/ESP/INTERNAL/EMP150002/{period}/NORMAL/1/invalidate", PERIOD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_VALID"));

        assertThat(statusAndReasonOf("EMP150002")).isEqualTo("NOT_VALID:MANUAL_INVALIDATION");
    }

    private void calculatedPayrollOf(String employeeNumber) {
        long employeeId = fixtures.insertEmployee("ESP", "INTERNAL", employeeNumber);
        fixtures.insertPresence(employeeId, 1, LocalDate.of(2025, 1, 1), null);
        fixtures.insertPayrollWithConcept("ESP", "INTERNAL", employeeNumber, PERIOD, "NORMAL", 1,
                "CALCULATED", "101", new BigDecimal("1000.00"));
    }

    // El test es una sola transaccion: lo que JPA tenga pendiente se vuelca antes de leer por SQL.
    private String statusAndReasonOf(String employeeNumber) {
        entityManager.flush();
        return jdbc.queryForObject("""
                select status || ':' || coalesce(status_reason_code, '<null>')
                  from payroll.payroll
                 where rule_system_code = 'ESP' and employee_type_code = 'INTERNAL'
                   and employee_number = ? and payroll_period_code = ?
                """, String.class, employeeNumber, PERIOD);
    }
}
