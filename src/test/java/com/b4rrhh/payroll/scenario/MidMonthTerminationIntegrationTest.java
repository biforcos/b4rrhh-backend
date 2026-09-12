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
 * Quien cesa a mitad de mes cobra los dias que le tocan (ADR-059 seccion 2, backend#73).
 *
 * El lanzador resolvia el convenio, la categoria y el centro de trabajo a fin de periodo. Quien
 * cesa el 15 tiene su clasificacion cerrada con el cese: a fin de mes no hay ninguna vigente, el
 * lanzador lo contaba como entrada que falta y la unidad se saltaba con AGREEMENT_CODE_MISSING.
 * Es lo que les paso a EMP000298 y EMP000921 en la corrida del deploy#3: los dos
 * total_skipped_not_eligible de la ejecucion 1, cesados el 12 y el 26 de septiembre.
 *
 * La pregunta estaba mal formulada, no la respuesta: un calculo correcto sobre un mes incompleto
 * no es invalido. Un finiquito de medio mes es un recibo CALCULATED normal.
 *
 * Abril de 2025 (30 dias) y un cese el dia 15. En TST y sobre el esquema real, por la misma razon
 * que ExecutionScopeOnSplitMonthIntegrationTest: el fixture monta el grafo que las migraciones ya
 * siembran para ESP.
 */
@TestWebSobreEsquemaReal
class MidMonthTerminationIntegrationTest {

    private static final String RULE_SYSTEM   = "TST";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final LocalDate HIRED    = LocalDate.of(2025, 1, 1);
    private static final LocalDate APRIL_1  = LocalDate.of(2025, 4, 1);
    private static final LocalDate APRIL_15 = LocalDate.of(2025, 4, 15);

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

    @Test
    void terminatedOnTheFifteenth_isCalculatedForTheDaysWorked_andSkipsNothing() {
        String emp = hireTerminatedOn(APRIL_15);

        CalculationRun run = launchSingleEmployee(emp);

        assertEquals("COMPLETED", run.status());
        assertEquals(1, run.totalCandidates());
        assertEquals(1, run.totalCalculated());
        assertEquals(0, run.totalSkippedNotEligible(), "el cese a mitad de mes no es una entrada que falte");
        assertEquals(0, run.totalErrors());

        Long pid = payrollId(emp);

        // 15 dias devengados —del 1 al 15, ambos incluidos— al precio de la jornada completa.
        // El tramo acaba el dia del cese y no a fin de mes, y eso es lo que dice la cantidad.
        List<Map<String, Object>> rows = rowsOf(pid, "101");
        assertEquals(1, rows.size(), "una sola linea: la jornada no cambia dentro del tramo");
        assertRow(rows.getFirst(), "712.50", "15", "47.50");
        assertAmount(pid, "970", "712.50");
    }

    @Test
    void terminatedOnTheFifteenth_earnsHalfOfWhoStaysTheWholeMonth() {
        String leaves = hireTerminatedOn(APRIL_15);
        String stays  = hireStillActive();

        assertEquals("COMPLETED", launchSingleEmployee(leaves).status());
        assertEquals("COMPLETED", launchSingleEmployee(stays).status());

        // 30 dias frente a 15, al mismo precio diario: la mitad, no cero.
        assertAmount(payrollId(stays),  "970", "1425.00");
        assertAmount(payrollId(leaves), "970", "712.50");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String hireTerminatedOn(LocalDate endDate) {
        String emp = uniqueEmployeeNumber();
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, HIRED, endDate);
        fixtures.insertLaborClassification(empId, HIRED, endDate);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), HIRED, endDate);
        return emp;
    }

    private String hireStillActive() {
        String emp = uniqueEmployeeNumber();
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, HIRED, null);
        fixtures.insertLaborClassification(empId, HIRED);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), HIRED, null);
        return emp;
    }

    private String uniqueEmployeeNumber() {
        return "MT" + (System.nanoTime() % 1_000_000_000L);
    }

    private CalculationRun launchSingleEmployee(String employeeNumber) {
        return launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employeeNumber),
                        null),
                null));
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

    private void assertAmount(Long payrollId, String conceptCode, String expected) {
        BigDecimal actual = jdbc.queryForObject(
                "select amount from payroll.payroll_concept where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, conceptCode);
        assertEquals(0, new BigDecimal(expected).compareTo(actual), conceptCode + " expected " + expected);
    }
}
