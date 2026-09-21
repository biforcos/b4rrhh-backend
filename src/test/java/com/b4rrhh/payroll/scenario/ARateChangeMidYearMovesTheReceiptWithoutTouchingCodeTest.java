package com.b4rrhh.payroll.scenario;

import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Un tipo de cotizacion cambia a mitad de ano y el recibo cambia con el, sin tocar codigo
 * ({@code backend#105}, criterio 2).
 *
 * <h2>Por que esta es la comprobacion y no otra</h2>
 *
 * <p>Porque es <b>la unica que distingue leer la tabla de haber copiado el mismo numero</b>. Un
 * test que afirmara «el 700 sale al 4,70 %» seria verde con la constante de Java y verde con la
 * lectura del catalogo: no dice nada. Este solo puede ser verde si el valor sale de la fila.
 *
 * <p>Y la forma del escenario es la que importa: <b>dos vigencias del mismo tipo dentro del mismo
 * ano</b>, y dos recibos de periodos distintos. Si la fecha de busqueda estuviera mal —o si no se
 * buscara— los dos darian lo mismo.
 *
 * <p>Se toca {@code CC_TRAB}, que es el tipo del trabajador con mas peso (4,70 %), y se mueve a
 * un numero que no existe en ningun sitio del catalogo ni del codigo ({@code 9,99 %}) para que
 * nadie pueda confundirlo con un valor sembrado.
 *
 * <h2>Lo que este test ensucia y como se limpia</h2>
 *
 * <p>Escribe en {@code ss_cotizacion_tipos}, que es catalogo sembrado por migracion para ESP y
 * compartido por todos los tests que calculan sobre ESP. Lo deshace en los dos extremos: en el
 * {@code @AfterEach} porque dejarlo puesto tumba a quien venga detras —y lo tumba lejos, con un
 * «no hay tipo vigente» que no se parece a este test—, y en el {@code @BeforeEach} porque el que
 * ensucia puede no llegar a su final.
 */
@TestWebSobreEsquemaReal
class ARateChangeMidYearMovesTheReceiptWithoutTouchingCodeTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final LocalDate JANUARY_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate JULY_1    = LocalDate.of(2025, 7, 1);
    private static final LocalDate JUNE_30   = LocalDate.of(2025, 6, 30);

    /** Un tipo que no existe ni en la V88 ni en ninguna constante: si sale, sale de la fila. */
    private static final BigDecimal SECOND_HALF_RATE = new BigDecimal("9.99");

    @Autowired
    private LaunchPayrollCalculationUseCase launch;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    private PayrollScenarioFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new PayrollScenarioFixtures(jdbc);
        restoreTheSeededRate();
    }

    @AfterEach
    void tearDown() {
        restoreTheSeededRate();
    }

    @Test
    void theSameEmployeePaysOneRateInAprilAndAnotherInAugust() {
        String emp = hireForTheWholeYear();

        Map<String, Object> april = calculateAndReadCcTrabajador(emp, "202504");
        assertEquals(0, new BigDecimal("4.70").compareTo((BigDecimal) april.get("rate")),
                "en abril manda la vigencia sembrada por la V88");

        // La ley cambia a mitad de ano: se cierra la fila vigente y se anade otra. Sin tocar
        // codigo, que es la frase entera de este issue.
        declareASecondVigencia();

        Map<String, Object> august = calculateAndReadCcTrabajador(emp, "202508");
        assertEquals(0, SECOND_HALF_RATE.compareTo((BigDecimal) august.get("rate")),
                "en agosto manda la vigencia nueva");

        // Y no es solo la tarifa escrita en la linea: el importe se movio con ella.
        assertTrue(((BigDecimal) august.get("amount")).compareTo((BigDecimal) april.get("amount")) > 0,
                "un tipo mas alto sobre la misma base tiene que dar mas deduccion: abril "
                        + april.get("amount") + " agosto " + august.get("amount"));

        // El recibo de abril sigue diciendo lo que decia. Declarar una vigencia nueva no
        // reescribe el pasado: lo que ya se calculo se calculo con el tipo de su fecha.
        assertEquals(0, new BigDecimal("4.70").compareTo(
                        (BigDecimal) readCcTrabajador(emp, "202504").get("rate")),
                "declarar una vigencia nueva no toca los recibos anteriores");
    }

    /** Y si no hay fila para la fecha, se para: no se inventa un tipo por omision. */
    @Test
    void aMissingVigenciaIsNotZeroAndIsNotTheLastKnownRate() {
        String emp = hireForTheWholeYear();
        jdbc.update("update payroll_engine.ss_cotizacion_tipos set valid_to = ?"
                + " where rule_system_code = ? and contingency_code = 'CC_TRAB'", JUNE_30, RULE_SYSTEM);

        launchFor(emp, "202508");
        vaciarLaSesion();

        // La unidad no se calcula, y el recibo no existe con un 0,00 dentro.
        assertEquals(0, (int) jdbc.queryForObject(
                "select count(*) from payroll.payroll where employee_number = ?"
                        + " and payroll_period_code = '202508' and status = 'CALCULATED'",
                Integer.class, emp),
                "sin tipo vigente no hay recibo: un cero silencioso seria peor que un fallo");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void restoreTheSeededRate() {
        jdbc.update("delete from payroll_engine.ss_cotizacion_tipos"
                        + " where rule_system_code = ? and contingency_code = 'CC_TRAB' and valid_from > ?",
                RULE_SYSTEM, JANUARY_1);
        jdbc.update("update payroll_engine.ss_cotizacion_tipos set valid_to = null"
                + " where rule_system_code = ? and contingency_code = 'CC_TRAB'", RULE_SYSTEM);
    }

    private void declareASecondVigencia() {
        jdbc.update("update payroll_engine.ss_cotizacion_tipos set valid_to = ?"
                        + " where rule_system_code = ? and contingency_code = 'CC_TRAB' and valid_from = ?",
                JUNE_30, RULE_SYSTEM, JANUARY_1);
        jdbc.update("insert into payroll_engine.ss_cotizacion_tipos"
                + " (rule_system_code, contingency_code, rate, valid_from, valid_to)"
                + " values (?, 'CC_TRAB', ?, ?, null)", RULE_SYSTEM, SECOND_HALF_RATE, JULY_1);
    }

    private String hireForTheWholeYear() {
        String emp = "TP" + (System.nanoTime() % 1_000_000_000L);
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, JANUARY_1, null);
        fixtures.insertLaborClassification(empId, JANUARY_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), JANUARY_1, null);
        return emp;
    }

    private Map<String, Object> calculateAndReadCcTrabajador(String employeeNumber, String period) {
        launchFor(employeeNumber, period);
        vaciarLaSesion();
        return readCcTrabajador(employeeNumber, period);
    }

    private void launchFor(String employeeNumber, String period) {
        launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, period, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employeeNumber),
                        null),
                null));
    }

    private Map<String, Object> readCcTrabajador(String employeeNumber, String period) {
        Map<String, Object> line = jdbc.queryForMap(
                "select c.rate, c.amount from payroll.payroll_concept c"
                        + " join payroll.payroll p on p.id = c.payroll_id"
                        + " where p.employee_number = ? and p.payroll_period_code = ?"
                        + "   and p.status = 'CALCULATED' and c.concept_code = '700'",
                employeeNumber, period);
        assertNotNull(line.get("rate"), "la linea del 700 tiene que traer su tarifa");
        return line;
    }

    /** Vacia la sesion antes de contar por JDBC, como el resto de los tests de escenario. */
    private void vaciarLaSesion() {
        entityManager.flush();
        entityManager.clear();
    }
}
