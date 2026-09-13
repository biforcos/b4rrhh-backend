package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.scenario.PayrollScenarioFixtures;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * La reglamentación de una ejecución es la que había cuando esa ejecución empezó, y la
 * siguiente ejecución ve los cambios (backend#87).
 *
 * <p>Dos ejecuciones seguidas con un cambio en el grafo en medio: la primera calcula con el
 * grafo de antes y la segunda con el de después. Eso es lo que prueba que lo cargado vive
 * lo que dura la ejecución <b>y no más</b>: si fuera una caché del proceso, la segunda
 * seguiría calculando con las reglas viejas y este test se caería.
 *
 * <p>El cambio es de metamodelo puro —se cierra la vigencia de una asignación de concepto—,
 * no de datos del empleado: lo que se prueba es qué reglamentación ve cada corrida.
 *
 * <p>TST y no ESP por la misma razón que el resto de esta franja: las migraciones ya
 * siembran para ESP el grafo que el fixture monta.
 */
@TestWebSobreEsquemaReal
class EachPayrollExecutionReadsItsOwnRulesIntegrationTest {

    private static final String RULE_SYSTEM = "TST";
    private static final LocalDate PERIOD_START = LocalDate.of(2025, 1, 1);

    private String employeeBeforeTheChange;
    private String employeeAfterTheChange;

    @Autowired
    private LaunchPayrollCalculationUseCase launchPayrollCalculationUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpData() {
        PayrollScenarioFixtures fixtures = new PayrollScenarioFixtures(jdbcTemplate);
        fixtures.seedConceptGraph(RULE_SYSTEM);

        employeeBeforeTheChange = hire(fixtures, "A" + (System.nanoTime() % 1_000_000_000L));
        employeeAfterTheChange = hire(fixtures, "B" + (System.nanoTime() % 1_000_000_000L));
    }

    @Test
    void aChangeInTheConceptGraphBetweenTwoExecutionsIsSeenOnlyByTheSecondOne() {
        var firstRun = launchFor(employeeBeforeTheChange);
        assertEquals(1, firstRun.totalCalculated());

        // El cambio: se cierra la vigencia de DESEMPLEO_TRABAJADOR antes del fin del periodo.
        // A partir de aquí, una ejecución que lea el grafo ya no debe incluirlo en su plan.
        int closed = jdbcTemplate.update(
                "update payroll_engine.concept_assignment set valid_to = ?"
                        + " where rule_system_code = ? and concept_code = ?",
                PERIOD_START, RULE_SYSTEM, "703");
        assertEquals(1, closed, "el caso necesita que la asignación de 703 exista y se cierre");

        var secondRun = launchFor(employeeAfterTheChange);
        assertEquals(1, secondRun.totalCalculated());

        // La primera ejecución calculó con el grafo de antes, y su recibo no se toca.
        assertEquals(7, conceptCount(employeeBeforeTheChange));
        assertEquals(1, conceptCount(employeeBeforeTheChange, "703"));

        // La segunda leyó el grafo de nuevo y vio el cambio.
        assertEquals(6, conceptCount(employeeAfterTheChange));
        assertEquals(0, conceptCount(employeeAfterTheChange, "703"));
    }

    private String hire(PayrollScenarioFixtures fixtures, String employeeNumber) {
        long employeeId = fixtures.insertEmployee(RULE_SYSTEM, "INTERNAL", employeeNumber);
        fixtures.insertPresence(employeeId, 1, PERIOD_START, null);
        fixtures.insertLaborClassification(employeeId, PERIOD_START);
        fixtures.insertWorkingTime(employeeId, new BigDecimal("100.00"), PERIOD_START, null);
        return employeeNumber;
    }

    private com.b4rrhh.payroll.domain.model.CalculationRun launchFor(String employeeNumber) {
        return launchPayrollCalculationUseCase.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM,
                "202501",
                "NORMAL",
                "ENGINE",
                "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget("INTERNAL", employeeNumber),
                        null
                ),
                null
        ));
    }

    private int conceptCount(String employeeNumber) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from payroll.payroll_concept where payroll_id = ?",
                Integer.class, payrollIdOf(employeeNumber));
        assertNotNull(count);
        return count;
    }

    private int conceptCount(String employeeNumber, String conceptCode) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from payroll.payroll_concept where payroll_id = ? and concept_code = ?",
                Integer.class, payrollIdOf(employeeNumber), conceptCode);
        assertNotNull(count);
        return count;
    }

    private Long payrollIdOf(String employeeNumber) {
        Long payrollId = jdbcTemplate.queryForObject(
                "select id from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = ?",
                Long.class,
                RULE_SYSTEM, "INTERNAL", employeeNumber, "202501", "NORMAL", 1);
        assertNotNull(payrollId, "no hay recibo para " + employeeNumber);
        return payrollId;
    }
}
