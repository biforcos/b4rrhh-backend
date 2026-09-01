package com.b4rrhh.payroll.scenario;

import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// No hace ninguna peticion HTTP, pero necesita lo mismo que la franja E2E: la
// aplicacion entera (el motor de nomina no se monta a trozos), en transaccion,
// sobre el esquema real. Declarar eso a mano seria otro contexto y otro clon;
// con la anotacion compartida entra en el mismo que los *EndToEndTest (#2).
//
// El sistema de reglas no puede ser ESP: las migraciones siembran para ESP el
// mismo grafo de conceptos, el binding y la tabla del convenio 99002405011982
// y el perfil de la categoria 99002405-G2 (V61, V66/V67, V71-V91, V87), y el
// fixture insertaria todo eso por segunda vez. Bajo TST el grafo es solo el que
// el escenario declara, que es lo que estas cifras afirman.
@TestWebSobreEsquemaReal
class PayrollHappyPathIntegrationTest {

    private static final String RULE_SYSTEM   = "TST";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202501";
    private static final String PAYROLL_TYPE  = "NORMAL";

    @Autowired
    private LaunchPayrollCalculationUseCase launch;

    @Autowired
    private JdbcTemplate jdbc;

    private PayrollScenarioFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new PayrollScenarioFixtures(jdbc);
        fixtures.seedConceptGraph(RULE_SYSTEM);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String uniqueEmployeeNumber() {
        return "HP" + (System.nanoTime() % 1_000_000_000L);
    }

    private CalculationRun launchSingleEmployee(String employeeNumber) {
        return launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employeeNumber),
                        null)));
    }

    private Long payrollId(String employeeNumber, int presenceNumber) {
        return jdbc.queryForObject(
                "select id from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = ? and status = ?",
                Long.class,
                RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE, presenceNumber, "CALCULATED");
    }

    private BigDecimal amount(Long payrollId, String conceptCode) {
        return jdbc.queryForObject(
                "select amount from payroll.payroll_concept where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, conceptCode);
    }

    private void assertAmount(Long payrollId, String conceptCode, String expected) {
        assertEquals(0, new BigDecimal(expected).compareTo(amount(payrollId, conceptCode)),
                conceptCode + " expected " + expected);
    }

    // Use when a concept may appear in multiple rows (e.g. SALARIO_BASE with two rate tiers)
    private void assertTotalAmount(Long payrollId, String conceptCode, String expected) {
        BigDecimal actual = jdbc.queryForObject(
                "select sum(amount) from payroll.payroll_concept where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, conceptCode);
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                conceptCode + " sum expected " + expected);
    }

    // ── Scenario 1: partial-month hire ───────────────────────────────────────

    @Test
    void partialMonthHire() {
        // Jan 15 → open; 17 days; SALARIO_BASE = 17 × 47.50 = 807.50
        String emp = uniqueEmployeeNumber();
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, LocalDate.of(2025, 1, 15), null);
        fixtures.insertLaborClassification(empId, LocalDate.of(2025, 1, 15));
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"),
                LocalDate.of(2025, 1, 15), null);

        var run = launchSingleEmployee(emp);

        assertEquals("COMPLETED", run.status());
        assertEquals(1, run.totalCalculated());

        Long pid = payrollId(emp, 1);
        assertNotNull(pid);

        assertEquals(7, jdbc.queryForObject(
                "select count(*) from payroll.payroll_concept where payroll_id = ?",
                Integer.class, pid));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from payroll.payroll_segment where payroll_id = ?",
                Integer.class, pid));

        assertAmount(pid, "101", "807.50");
        assertAmount(pid, "700",  "37.95");
        assertAmount(pid, "703",  "12.52");
        assertAmount(pid, "800", "121.13");
        assertAmount(pid, "970", "807.50");
        assertAmount(pid, "980", "171.60");
        assertAmount(pid, "990", "635.90");
    }

    // ── Scenario 2: mid-month termination ────────────────────────────────────

    @Test
    void midMonthTermination() {
        // Jan 1 → Jan 20; 20 days; SALARIO_BASE = 20 × 47.50 = 950.00
        String emp = uniqueEmployeeNumber();
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 20));
        fixtures.insertLaborClassification(empId, LocalDate.of(2025, 1, 1));
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"),
                LocalDate.of(2025, 1, 1), null);

        var run = launchSingleEmployee(emp);

        assertEquals("COMPLETED", run.status());
        assertEquals(1, run.totalCalculated());

        Long pid = payrollId(emp, 1);
        assertNotNull(pid);

        assertEquals(7, jdbc.queryForObject(
                "select count(*) from payroll.payroll_concept where payroll_id = ?",
                Integer.class, pid));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from payroll.payroll_segment where payroll_id = ?",
                Integer.class, pid));

        assertAmount(pid, "101", "950.00");
        assertAmount(pid, "700",  "44.65");
        assertAmount(pid, "703",  "14.73");
        assertAmount(pid, "800", "142.50");
        assertAmount(pid, "970", "950.00");
        assertAmount(pid, "980", "201.88");
        assertAmount(pid, "990", "748.12");
    }

    // ── Scenario 3: two clean presences in the same period ───────────────────

    @Test
    void twoPresencesSamePeriod() {
        // Presence 1: Jan 1–15 (15 days), Presence 2: Jan 16–30 (15 days)
        // Both: SALARIO_BASE = 15 × 47.50 = 712.50
        String emp = uniqueEmployeeNumber();
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, LocalDate.of(2025, 1,  1), LocalDate.of(2025, 1, 15));
        fixtures.insertPresence(empId, 2, LocalDate.of(2025, 1, 16), LocalDate.of(2025, 1, 30));
        fixtures.insertLaborClassification(empId, LocalDate.of(2025, 1, 1));
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"),
                LocalDate.of(2025, 1, 1), null);

        var run = launchSingleEmployee(emp);

        assertEquals("COMPLETED", run.status());
        assertEquals(2, run.totalCalculated());

        Long pid1 = payrollId(emp, 1);
        Long pid2 = payrollId(emp, 2);
        assertNotNull(pid1);
        assertNotNull(pid2);

        for (Long pid : new Long[]{pid1, pid2}) {
            assertEquals(7, jdbc.queryForObject(
                    "select count(*) from payroll.payroll_concept where payroll_id = ?",
                    Integer.class, pid));
            assertEquals(1, jdbc.queryForObject(
                    "select count(*) from payroll.payroll_segment where payroll_id = ?",
                    Integer.class, pid));

            assertAmount(pid, "101", "712.50");
            assertAmount(pid, "700",  "33.49");
            assertAmount(pid, "703",  "11.04");
            assertAmount(pid, "800", "106.88");
            assertAmount(pid, "970", "712.50");
            assertAmount(pid, "980", "151.41");
            assertAmount(pid, "990", "561.09");
        }
    }

    // ── Scenario 4: two presences each with a working-time change ────────────

    @Test
    void twoPresencesWithSegments() {
        // Presence 1: Jan 1–15
        //   Seg 1a: Jan 1–9  (9d, 100%)  → 9  × 47.50 × 1.00 = 427.50
        //   Seg 1b: Jan 10–15 (6d, 50%)  → 6  × 47.50 × 0.50 = 142.50
        //   SALARIO_BASE = 570.00; net = 448.87
        //
        // Presence 2: Jan 16–30
        //   Seg 2a: Jan 16–25 (10d, 100%) → 10 × 47.50 × 1.00 = 475.00
        //   Seg 2b: Jan 26–30  (5d, 50%)  →  5 × 47.50 × 0.50 = 118.75
        //   SALARIO_BASE = 593.75; net = 467.58
        String emp = uniqueEmployeeNumber();
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, LocalDate.of(2025, 1,  1), LocalDate.of(2025, 1, 15));
        fixtures.insertPresence(empId, 2, LocalDate.of(2025, 1, 16), LocalDate.of(2025, 1, 30));
        fixtures.insertLaborClassification(empId, LocalDate.of(2025, 1, 1));

        // Presence-1 working times — explicit end dates prevent bleed into presence 2
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"),
                LocalDate.of(2025, 1,  1), LocalDate.of(2025, 1,  9));
        fixtures.insertWorkingTime(empId, new BigDecimal("50.00"),
                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 15));

        // Presence-2 working times
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"),
                LocalDate.of(2025, 1, 16), LocalDate.of(2025, 1, 25));
        fixtures.insertWorkingTime(empId, new BigDecimal("50.00"),
                LocalDate.of(2025, 1, 26), null);

        var run = launchSingleEmployee(emp);

        assertEquals("COMPLETED", run.status());
        assertEquals(2, run.totalCalculated());

        Long pid1 = payrollId(emp, 1);
        Long pid2 = payrollId(emp, 2);
        assertNotNull(pid1);
        assertNotNull(pid2);

        // Presence 1 — concept 101 appears as 2 rows (rate 47.50 for 100%, rate 23.75 for 50%)
        // because the collapse key includes the rate; segments with different rates stay separate
        assertEquals(8, jdbc.queryForObject(
                "select count(*) from payroll.payroll_concept where payroll_id = ?",
                Integer.class, pid1));
        assertEquals(2, jdbc.queryForObject(
                "select count(*) from payroll.payroll_segment where payroll_id = ?",
                Integer.class, pid1));

        assertTotalAmount(pid1, "101", "570.00");
        assertAmount(pid1, "700",  "26.79");
        assertAmount(pid1, "703",   "8.84");
        assertAmount(pid1, "800",  "85.50");
        assertAmount(pid1, "970", "570.00");
        assertAmount(pid1, "980", "121.13");
        assertAmount(pid1, "990", "448.87");

        // Presence 2 — same: 2 rows for 101 (rate 47.50 and rate 23.75)
        assertEquals(8, jdbc.queryForObject(
                "select count(*) from payroll.payroll_concept where payroll_id = ?",
                Integer.class, pid2));
        assertEquals(2, jdbc.queryForObject(
                "select count(*) from payroll.payroll_segment where payroll_id = ?",
                Integer.class, pid2));

        assertTotalAmount(pid2, "101", "593.75");
        assertAmount(pid2, "700",  "27.91");
        assertAmount(pid2, "703",   "9.20");
        assertAmount(pid2, "800",  "89.06");
        assertAmount(pid2, "970", "593.75");
        assertAmount(pid2, "980", "126.17");
        assertAmount(pid2, "990", "467.58");
    }
}
