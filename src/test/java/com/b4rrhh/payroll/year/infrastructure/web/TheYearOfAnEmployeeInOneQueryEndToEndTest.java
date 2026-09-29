package com.b4rrhh.payroll.year.infrastructure.web;

import com.b4rrhh.payroll.scenario.PayrollScenarioFixtures;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El año de un empleado en una consulta (b4rrhh/backend#151), para la tira del
 * {@code frontend#109}: doce meses no son doce viajes por carril.
 *
 * <p>La consulta compone lo que ya sirven la presencia, los recibos, las ausencias, las entradas
 * y las marcas; no calcula ninguna de las cinco cosas de otra manera. Por eso el último test
 * compara sus contadores con lo que devuelven las consultas de ese mes.
 */
@TestWebSobreEsquemaReal
class TheYearOfAnEmployeeInOneQueryEndToEndTest {

    private static final String EMP = "EMP151001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private PayrollScenarioFixtures fixtures;
    private long employeeId;

    @BeforeEach
    void setUp() {
        fixtures = new PayrollScenarioFixtures(jdbc);
        employeeId = fixtures.insertEmployee("ESP", "INTERNAL", EMP);
        fixtures.insertPresence(employeeId, 1, LocalDate.of(2025, 11, 3), null);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anAbsenceThatCrossesMonthsAndYearsComesOnceAndWhole() throws Exception {
        fixtures.insertAbsence(employeeId, "IT_COMMON", LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 10));
        fixtures.insertAbsence(employeeId, "VACATION", LocalDate.of(2026, 3, 28), LocalDate.of(2026, 4, 3));
        fixtures.insertAbsence(employeeId, "VACATION", LocalDate.of(2025, 11, 10), LocalDate.of(2025, 11, 12));
        fixtures.insertAbsence(employeeId, "IT_COMMON", LocalDate.of(2026, 9, 1), null);

        mockMvc.perform(get("/employees/ESP/INTERNAL/{n}/year-summary", EMP).param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.absences", hasSize(3)))
                .andExpect(jsonPath("$.absences[?(@.startDate == '2025-12-20')].endDate").value("2026-01-10"))
                .andExpect(jsonPath("$.absences[?(@.startDate == '2026-03-28')].endDate").value("2026-04-03"))
                .andExpect(jsonPath("$.absences[?(@.startDate == '2026-09-01')].absenceTypeCode").value("IT_COMMON"))
                .andExpect(jsonPath("$.presences", hasSize(1)))
                .andExpect(jsonPath("$.presences[0].startDate").value("2025-11-03"))
                .andExpect(jsonPath("$.presences[0].endDate").value(nullValue()))
                .andExpect(jsonPath("$.months", hasSize(12)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aMonthWithoutPayrollHasNoStateAClosedOneIsClosedAndAnOpenOneIsOpen() throws Exception {
        fixtures.insertPayrollWithConcept("ESP", "INTERNAL", EMP, "202601", "NORMAL", 1, "DEFINITIVE", "101", BigDecimal.TEN);
        fixtures.insertPayrollWithConcept("ESP", "INTERNAL", EMP, "202602", "NORMAL", 1, "CALCULATED", "101", BigDecimal.TEN);

        mockMvc.perform(get("/employees/ESP/INTERNAL/{n}/year-summary", EMP).param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months[0].payrollPeriodCode").value("202601"))
                .andExpect(jsonPath("$.months[0].payrollState").value("CLOSED"))
                .andExpect(jsonPath("$.months[1].payrollState").value("OPEN"))
                .andExpect(jsonPath("$.months[2].payrollPeriodCode").value("202603"))
                .andExpect(jsonPath("$.months[2].payrollState").value(nullValue()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void theYearBeforeTheHireIsEmpty() throws Exception {
        mockMvc.perform(get("/employees/ESP/INTERNAL/{n}/year-summary", EMP).param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presences", hasSize(0)))
                .andExpect(jsonPath("$.absences", hasSize(0)))
                .andExpect(jsonPath("$.months", hasSize(12)))
                .andExpect(jsonPath("$.months[?(@.payrollState != null)]", hasSize(0)))
                .andExpect(jsonPath("$.months[?(@.payrollInputCount > 0)]", hasSize(0)))
                .andExpect(jsonPath("$.months[?(@.activeRetroMarkCount > 0 || @.consumedRetroMarkCount > 0)]", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void theCountersOfAMonthAreWhatTheQueriesOfThatMonthReturn() throws Exception {
        insertInput("H01", 202603, "3");
        insertInput("H02", 202603, "1");
        insertInput("H01", 202604, "2");
        long run = insertRun();
        insertMark("202602", "ACTIVE", null);
        insertMark("202602", "ACTIVE", null);
        insertMark("202602", "CONSUMED", run);
        insertMark("202602", "DISCARDED", null);

        String year = mockMvc.perform(get("/employees/ESP/INTERNAL/{n}/year-summary", EMP).param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months[2].payrollInputCount").value(2))
                .andExpect(jsonPath("$.months[2].payrollInputConceptCount").value(2))
                .andExpect(jsonPath("$.months[3].payrollInputCount").value(1))
                .andExpect(jsonPath("$.months[1].activeRetroMarkCount").value(2))
                .andExpect(jsonPath("$.months[1].consumedRetroMarkCount").value(1))
                .andReturn().getResponse().getContentAsString();

        String march = mockMvc.perform(get("/employees/ESP/INTERNAL/{n}/payroll-inputs", EMP).param("period", "202603"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Object> marchInputs = JsonPath.read(march, "$[*]");
        assertThat((Integer) JsonPath.read(year, "$.months[2].payrollInputCount")).isEqualTo(marchInputs.size());

        String marks = mockMvc.perform(get("/employees/ESP/INTERNAL/{n}/retro-marks", EMP))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Object> activeInFebruary = JsonPath.read(marks, "$[?(@.fromPeriodCode == '202602' && @.status == 'ACTIVE')]");
        assertThat((Integer) JsonPath.read(year, "$.months[1].activeRetroMarkCount")).isEqualTo(activeInFebruary.size());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anUnknownEmployeeIsNotFound() throws Exception {
        mockMvc.perform(get("/employees/ESP/INTERNAL/{n}/year-summary", "NOBODY151").param("year", "2026"))
                .andExpect(status().isNotFound());
    }

    private void insertInput(String concept, int period, String quantity) {
        jdbc.update("""
                insert into employee.employee_payroll_input
                    (rule_system_code, employee_type_code, employee_number, concept_code, period, quantity)
                values ('ESP', 'INTERNAL', ?, ?, ?, ?)
                """, EMP, concept, period, new BigDecimal(quantity));
    }

    private long insertRun() {
        return jdbc.queryForObject("""
                insert into payroll.calculation_run (
                    rule_system_code, payroll_period_code, payroll_type_code,
                    calculation_engine_code, calculation_engine_version,
                    requested_at, requested_by, status, target_selection_json, started_at)
                values ('ESP', '202603', 'NORMAL', 'ENGINE', '1.0', now(), 'test', 'RUNNING',
                        '{"selectionType":"SINGLE_CALCULATION_UNIT"}', now())
                returning id
                """, Long.class);
    }

    private void insertMark(String fromPeriod, String status, Long consumedRun) {
        boolean consumed = "CONSUMED".equals(status);
        boolean discarded = "DISCARDED".equals(status);
        jdbc.update("""
                insert into payroll.retro_mark
                    (rule_system_code, employee_type_code, employee_number, presence_number, from_period_code,
                     status, source_vertical_code, source_table,
                     consumed_at, consumed_period_code, consumed_run_id,
                     discarded_at, discarded_by, discard_reason)
                values ('ESP', 'INTERNAL', ?, 1, ?, ?, 'ABSENCE', 'employee.employee_absence',
                        ?, ?, ?, ?, ?, ?)
                """,
                EMP, fromPeriod, status,
                consumed ? java.sql.Timestamp.valueOf("2026-03-31 10:00:00") : null,
                consumed ? "202603" : null,
                consumedRun,
                discarded ? java.sql.Timestamp.valueOf("2026-03-31 10:00:00") : null,
                discarded ? "test" : null,
                discarded ? "no procede" : null);
    }
}
