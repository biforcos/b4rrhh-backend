package com.b4rrhh.payroll.scenario;

import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El tipo de desempleo lo decide el contrato del tramo ({@code backend#124}).
 *
 * <h2>Lo que estaba mal</h2>
 *
 * <p>El {@code backend#123} dejó el desempleo en 5,50 + 1,55 para todo el mundo, que es el tipo de
 * la <b>contratación indefinida</b>. La contratación de duración determinada cotiza al 8,30: 6,70
 * la empresa y 1,60 la persona trabajadora (Orden PJC/297/2026, art. 33.2.a).1.º y 2.º). En la
 * semilla hay 182 empleados con contrato 401 o 402 cotizando de menos.
 *
 * <h2>Lo que la Orden dice y el nombre del contrato no</h2>
 *
 * <p>El art. 33.2.a).1.º mete en el tipo de la indefinida, además de los indefinidos, <b>los
 * formativos, los de relevo y los de sustitución e interinidad</b>. De los ocho contratos del
 * catálogo que no son indefinidos, seis cotizan como si lo fueran. Por eso la modalidad se declara
 * en una tabla con su vigencia y no se deduce del código: deducirla daría seis veces mal.
 *
 * <h2>Hasta dónde llega, y dónde se queda</h2>
 *
 * <p>El contrato viaja por el tramo —la partición ya corta por él desde el {@code backend#47}— y de
 * ahí sale la modalidad. Lo que <b>no</b> se parte todavía es la base: la cotización es mensual
 * (art. 1.1 de la Orden) y sus conceptos son de ámbito {@code PERIOD}, así que un cambio de
 * contrato a mitad de mes deja el mes entero al tipo del último tramo. Eso tiene su test aquí, que
 * dice lo que pasa y no que esté bien.
 */
@TestWebSobreEsquemaReal
class TheUnemploymentRateDependsOnTheKindOfContractTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final LocalDate ENERO_1  = LocalDate.of(2025, 1, 1);
    private static final LocalDate ABRIL_15 = LocalDate.of(2025, 4, 15);
    private static final LocalDate ABRIL_16 = LocalDate.of(2025, 4, 16);

    private static final String INDEFINIDO = "100";
    private static final String TEMPORAL   = "401";
    private static final String FORMATIVO  = "421";

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
    }

    /**
     * Dos empleados iguales salvo el contrato: cuotas de desempleo distintas, las dos partes.
     *
     * <p>Rojo antes del {@code backend#124}: los dos pagaban 5,50 y 1,55.
     */
    @Test
    void twoEmployeesAlikeExceptForTheContractPayDifferentUnemployment() {
        Long indefinido = reciboDe(empleadoCon(INDEFINIDO));
        Long temporal   = reciboDe(empleadoCon(TEMPORAL));

        assertEquals(0, tipo(indefinido, "P_SS_DESEMPLEO_EMP").compareTo(new BigDecimal("5.50")),
                "el indefinido cotiza al 5,50 de empresa (art. 33.2.a).1.o)");
        assertEquals(0, tipo(temporal, "P_SS_DESEMPLEO_EMP").compareTo(new BigDecimal("6.70")),
                "y el de duracion determinada al 6,70 (art. 33.2.a).2.o)");

        assertEquals(0, tipo(indefinido, "P_SS_DESEMPLEO").compareTo(new BigDecimal("1.55")),
                "la persona trabajadora con contrato indefinido, al 1,55");
        assertEquals(0, tipo(temporal, "P_SS_DESEMPLEO").compareTo(new BigDecimal("1.60")),
                "y con contrato de duracion determinada, al 1,60");

        // Y la misma base: lo unico que les separa es el tipo.
        assertEquals(0, importe(indefinido, "B_CP").compareTo(importe(temporal, "B_CP")),
                "los dos escenarios tienen que tener la misma base o no prueban el tipo");
        assertTrue(importe(temporal, "721").compareTo(importe(indefinido, "721")) > 0,
                "la cuota de la empresa del temporal es mayor: "
                        + importe(temporal, "721") + " frente a " + importe(indefinido, "721"));
        assertTrue(importe(temporal, "703").compareTo(importe(indefinido, "703")) > 0,
                "y la del trabajador tambien");
    }

    /**
     * Un formativo es temporal y cotiza como un indefinido, que es lo que el nombre no dice.
     *
     * <p>Este es el caso que hace falta que sea un dato y no una regla sobre el código: el 421 es
     * un contrato de duración determinada y el art. 33.2.a).1.º lo nombra expresamente en el tipo
     * de la indefinida. Una regla del tipo «si no empieza por 1, es temporal» le cobraría de más.
     */
    @Test
    void aTrainingContractIsTemporaryAndStillPaysTheIndefiniteRate() {
        Long recibo = reciboDe(empleadoCon(FORMATIVO));

        assertEquals(0, tipo(recibo, "P_SS_DESEMPLEO_EMP").compareTo(new BigDecimal("5.50")),
                "el formativo esta nombrado en el art. 33.2.a).1.o: cotiza al 5,50");
    }

    /**
     * Si el contrato cambia a mitad de mes, hoy manda el del ultimo tramo, y eso <b>no es lo que
     * dice la Orden</b>.
     *
     * <p>Lo correcto seria una base por tramo, cada una con su tipo. No se puede montar todavia:
     * la base de cotizacion y las cuotas son conceptos de ambito {@code PERIOD} —la base es
     * mensual por el art. 1.1 de la Orden— y el motor las resuelve una vez, con el contexto del
     * ultimo tramo. Partir la base por tramo es un cambio del modelo de cotizacion y no de este
     * issue.
     *
     * <p>Este test no afirma que el resultado sea correcto: afirma <b>que el caso existe y cuanto
     * se desvia</b>, para que el dia que se parta la base este escrito lo que tiene que cambiar.
     * Un empleado que pasa de indefinido a temporal el dia 16 cotiza hoy el mes entero al 6,70;
     * lo que le tocaria es medio mes a cada tipo.
     */
    @Test
    void aMidMonthChangeOfContractIsNotSplitYetAndTakesTheLastContractRate() {
        String emp = empleadoCon(INDEFINIDO, ENERO_1, ABRIL_15);
        fixtures.insertContract(empIdDe(emp), ABRIL_16, null, TEMPORAL);

        Long recibo = reciboDe(emp);

        List<BigDecimal> tipos = jdbc.queryForList(
                "select amount from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = 'P_SS_DESEMPLEO_EMP'"
                        + " order by execution_order",
                BigDecimal.class, recibo);

        assertEquals(1, tipos.size(),
                "el tipo se resuelve una vez, porque la cuota es de ambito PERIOD");
        assertEquals(0, tipos.get(0).compareTo(new BigDecimal("6.70")),
                "y con el contrato del ultimo tramo: el mes entero al tipo del temporal");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String empleadoCon(String contractCode) {
        return empleadoCon(contractCode, ENERO_1, null);
    }

    private String empleadoCon(String contractCode, LocalDate desde, LocalDate hasta) {
        String emp = "TP" + (System.nanoTime() % 1_000_000_000L);
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, ENERO_1, null);
        fixtures.insertLaborClassification(empId, ENERO_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), ENERO_1, null);
        fixtures.insertExtraPaymentRegime(empId, false, ENERO_1, null);
        fixtures.insertContract(empId, desde, hasta, contractCode);
        return emp;
    }

    private long empIdDe(String employeeNumber) {
        return jdbc.queryForObject(
                "select id from employee.employee where rule_system_code = ?"
                        + " and employee_type_code = ? and employee_number = ?",
                Long.class, RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber);
    }

    private Long reciboDe(String employeeNumber) {
        CalculationRun run = launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employeeNumber),
                        null),
                null));
        assertEquals("COMPLETED", run.status(), () -> "la corrida no salio limpia: "
                + jdbc.queryForList(
                        "select message_code, severity_code, message from payroll.calculation_run_message"
                                + " where run_id = ?", run.id()));
        entityManager.flush();
        entityManager.clear();

        return jdbc.queryForObject(
                "select id from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and status = 'CALCULATED'",
                Long.class, RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE);
    }

    /** El tipo que resolvio el motor: es el importe del concepto tecnico que lo trae. */
    private BigDecimal tipo(Long payrollId, String conceptCode) {
        return jdbc.queryForObject(
                "select amount from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = ?"
                        + " order by execution_order limit 1",
                BigDecimal.class, payrollId, conceptCode);
    }

    private BigDecimal importe(Long payrollId, String conceptCode) {
        return jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, conceptCode);
    }
}
