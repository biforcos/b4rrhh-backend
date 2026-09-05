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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * El ambito de ejecucion decide el recibo en un mes con la jornada partida (ADR-058, backend#64).
 *
 * Abril de 2025 (30 dias) y una jornada que cambia el dia 16. Sobre el esquema real y con la
 * aplicacion entera, como PayrollHappyPathIntegrationTest, y en TST por la misma razon: el
 * fixture monta el grafo que las migraciones ya siembran para ESP.
 *
 * Dos cosas se comprueban aqui y en ningun otro sitio:
 *
 *  1. El mismo concepto, SALARIO_BASE, declarado en SEGMENT y en PERIOD da recibos distintos
 *     con la misma jornada partida. Hasta backend#64 daban el mismo, porque nadie leia el
 *     ambito y lo unico que partia el recibo eran las ventanas de jornada (backend#46).
 *  2. El tope de cotizacion es PERIOD, y por eso topar un mes partido en dos da el mismo
 *     numero que topar el mes entero. Antes el tope se prorrateaba con los dias del primer
 *     tramo y en un mes partido salia la mitad (2454,75 en el piloto del backend#46).
 */
@TestWebSobreEsquemaReal
class ExecutionScopeOnSplitMonthIntegrationTest {

    private static final String RULE_SYSTEM   = "TST";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final LocalDate APRIL_1  = LocalDate.of(2025, 4, 1);
    private static final LocalDate APRIL_15 = LocalDate.of(2025, 4, 15);
    private static final LocalDate APRIL_16 = LocalDate.of(2025, 4, 16);

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

    // ── 1. SALARIO_BASE en SEGMENT y en PERIOD ────────────────────────────────

    @Test
    void salarioBaseInSegmentScope_splitWorkingTime_yieldsOneRowPerSegment() {
        // El fixture declara la cadena del salario base en SEGMENT, como la semilla ESP (V119).
        String emp = hireWithSplitWorkingTime(new BigDecimal("100.00"), new BigDecimal("50.00"));

        CalculationRun run = launchSingleEmployee(emp);
        assertEquals("COMPLETED", run.status());
        Long pid = payrollId(emp);

        // 15 x 47,50 y 15 x 23,75: dos lineas, porque el precio cambia con la jornada.
        List<Map<String, Object>> rows = rowsOf(pid, "101");
        assertEquals(2, rows.size(), "una linea por tramo de jornada");
        assertRow(rows.get(0), "712.50", "15", "47.50");
        assertRow(rows.get(1), "356.25", "15", "23.75");
        assertAmount(pid, "970", "1068.75");
    }

    @Test
    void salarioBaseInPeriodScope_splitWorkingTime_yieldsOneRowOverTheWholePeriod() {
        // Se cambia el ambito de los cuatro conceptos de la cadena, no solo el del 101: un
        // PERIOD no puede leer un operando SEGMENT (backend#63).
        fixtures.setExecutionScope(RULE_SYSTEM, "PERIOD", "101", "D01", "J01", "P01");
        String emp = hireWithSplitWorkingTime(new BigDecimal("100.00"), new BigDecimal("50.00"));

        CalculationRun run = launchSingleEmployee(emp);
        assertEquals("COMPLETED", run.status());
        Long pid = payrollId(emp);

        // La regla esta definida sobre el periodo entero: 30 dias devengados y la jornada del
        // periodo, que es la media ponderada por dias (75 %): 47,50 x 0,75 = 35,63; 30 x 35,63.
        List<Map<String, Object>> rows = rowsOf(pid, "101");
        assertEquals(1, rows.size(), "una sola linea sobre el periodo");
        assertRow(rows.get(0), "1068.90", "30", "35.63");
        assertAmount(pid, "970", "1068.90");
    }

    // ── 2. El tope de cotizacion sobre un mes partido ─────────────────────────

    @Test
    void contributionCap_isAppliedOnceOverThePeriod_soASplitMonthCapsLikeAWholeMonth() {
        // Tope 4909,50 y suelo 1323,00 (grupo 05, 2025) y un precio diario que se pasa del
        // tope: 200 x 30 = 6000 > 4909,50, asi que el tope muerde en los dos empleados.
        fixtures.seedContributionCaps(RULE_SYSTEM, new BigDecimal("1323.00"), new BigDecimal("4909.50"));
        fixtures.setDailyRate(RULE_SYSTEM, new BigDecimal("200.00"));

        String wholeMonth = hireWithOneWorkingTime(new BigDecimal("100.00"));
        String splitMonth = hireWithSplitWorkingTime(new BigDecimal("100.00"), new BigDecimal("100.00"));

        assertEquals("COMPLETED", launchSingleEmployee(wholeMonth).status());
        assertEquals("COMPLETED", launchSingleEmployee(splitMonth).status());
        Long pidWhole = payrollId(wholeMonth);
        Long pidSplit = payrollId(splitMonth);

        // La misma base bruta (6000) por los dos caminos: una linea o dos, segun la jornada.
        assertEquals(1, rowsOf(pidWhole, "101").size());
        assertEquals(1, rowsOf(pidSplit, "101").size(), "misma jornada en los dos tramos: el colapso las funde");
        assertAmount(pidWhole, "970", "6000.00");
        assertAmount(pidSplit, "970", "6000.00");

        // Y el mismo tope: B_CC_MAX = min(6000, 4909,50) una vez sobre el mes, en los dos.
        // Solo se ve a traves de los porcentajes, porque las bases no salen en el recibo:
        // 4909,50 x 4,70 % = 230,75 y 4909,50 x 1,55 % = 76,10.
        assertAmount(pidWhole, "700", "230.75");
        assertAmount(pidSplit, "700", "230.75");
        assertAmount(pidWhole, "703", "76.10");
        assertAmount(pidSplit, "703", "76.10");
        assertEquals(amount(pidWhole, "980"), amount(pidSplit, "980"));
        assertEquals(amount(pidWhole, "990"), amount(pidSplit, "990"));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String hireWithOneWorkingTime(BigDecimal percentage) {
        String emp = uniqueEmployeeNumber();
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, LocalDate.of(2025, 1, 1), null);
        fixtures.insertLaborClassification(empId, LocalDate.of(2025, 1, 1));
        fixtures.insertWorkingTime(empId, percentage, LocalDate.of(2025, 1, 1), null);
        return emp;
    }

    private String hireWithSplitWorkingTime(BigDecimal untilApril15, BigDecimal fromApril16) {
        String emp = uniqueEmployeeNumber();
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, LocalDate.of(2025, 1, 1), null);
        fixtures.insertLaborClassification(empId, LocalDate.of(2025, 1, 1));
        fixtures.insertWorkingTime(empId, untilApril15, LocalDate.of(2025, 1, 1), APRIL_15);
        fixtures.insertWorkingTime(empId, fromApril16, APRIL_16, null);
        return emp;
    }

    private String uniqueEmployeeNumber() {
        return "SC" + (System.nanoTime() % 1_000_000_000L);
    }

    private CalculationRun launchSingleEmployee(String employeeNumber) {
        return launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employeeNumber),
                        null)));
    }

    private Long payrollId(String employeeNumber) {
        Long id = jdbc.queryForObject(
                "select id from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = 1 and status = ?",
                Long.class,
                RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE, "CALCULATED");
        assertNotNull(id);
        assertEquals(APRIL_1, jdbc.queryForObject(
                "select min(segment_start) from payroll.payroll_segment where payroll_id = ?",
                LocalDate.class, id));
        return id;
    }

    private List<Map<String, Object>> rowsOf(Long payrollId, String conceptCode) {
        return jdbc.queryForList(
                "select amount, quantity, rate from payroll.payroll_concept"
                        + " where payroll_id = ? and concept_code = ? order by line_number",
                payrollId, conceptCode);
    }

    private void assertRow(Map<String, Object> row, String amount, String quantity, String rate) {
        assertEquals(0, new BigDecimal(amount).compareTo((BigDecimal) row.get("amount")), "importe");
        assertEquals(0, new BigDecimal(quantity).compareTo((BigDecimal) row.get("quantity")), "cantidad");
        assertEquals(0, new BigDecimal(rate).compareTo((BigDecimal) row.get("rate")), "precio");
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
}
