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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La base reguladora lee el mes anterior, y solo si esta cerrado ({@code backend#128}, ADR-074).
 *
 * <h2>Lo que este test defiende</h2>
 *
 * <p>Es la primera vez que un calculo de este motor mira <b>fuera de su periodo</b>, y la regla que se
 * fija aqui la heredan los atrasos del paso 6 de {@code b4rrhh/workspace#9}. Son tres respuestas y no
 * dos, y la del medio es la que no puede relajarse:
 *
 * <ol>
 *   <li>recibo del mes anterior <b>cerrado</b> - se lee su base de contingencias comunes entre 30;</li>
 *   <li>recibo del mes anterior que <b>todavia puede cambiar</b> - este recibo <b>no se calcula</b>;</li>
 *   <li><b>no hay</b> recibo del mes anterior - la base teorica de este mes, con aviso salvo que el
 *       empleado entrara este mes.</li>
 * </ol>
 *
 * <p>La norma, verificada contra el BOE: art. 13 del Decreto 1646/1972 ({@code BOE-A-1972-944}). El
 * 13.1 dice «el mes anterior»; el 13.2, que con retribucion mensual se divide «por treinta»; el 13.3,
 * que a quien ingreso en el mismo mes se le aplica <b>ese</b> mes, que es el caso sin aviso.
 *
 * <h2>El caso que hace que los otros digan algo</h2>
 *
 * <p>{@code unEmpleadoSinBajaNoMiraElMesAnterior}. Sin el, una implementacion que bloqueara a
 * cualquiera cuyo mes anterior no estuviera cerrado pasaria los demas tests — y dejaria sin calcular
 * los ochocientos sesenta recibos de una empresa porque el mes pasado quedo uno a medio revisar.
 */
@TestWebSobreEsquemaReal
class TheRegulatoryBaseOnlyReadsAClosedPreviousMonthTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PERIOD_BEFORE = "202503";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final LocalDate ENERO_1  = LocalDate.of(2025, 1, 1);
    private static final LocalDate ABRIL_5  = LocalDate.of(2025, 4, 5);
    private static final LocalDate ABRIL_10 = LocalDate.of(2025, 4, 10);
    private static final LocalDate ABRIL_12 = LocalDate.of(2025, 4, 12);

    /**
     * La base diaria teorica del convenio de los escenarios.
     *
     * <p>{@code P01} vale 47,50 y el convenio tiene cuatro pagas extras (V144), asi que la prorrata
     * diaria es {@code 4 x 47,50 / 12 = 15,833333} y la base reguladora diaria teorica
     * {@code 47,50 + 15,833333 = 63,333333}, que redondeada a dos decimales es <b>63,33</b>.
     *
     * <p>Escrito asi y no como una expresion a proposito: si el numero cambia, lo que hay que
     * comprobar es si cambio el precio del dia o cambio la regla.
     */
    private static final BigDecimal BASE_REGULADORA_TEORICA = new BigDecimal("63.33");

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

    /** Recibo anterior cerrado: la base reguladora es su base de contingencias comunes entre 30. */
    @Test
    void conReciboAnteriorDefinitivoLaBaseReguladoraEsLaSuyaEntreTreinta() {
        String emp = numeroUnico();
        long empId = altaBasica(emp, ENERO_1);
        fixtures.insertAbsence(empId, "IT_COMMON", ABRIL_10, ABRIL_12);
        fixtures.insertPayrollWithConcept(RULE_SYSTEM, EMPLOYEE_TYPE, emp, PERIOD_BEFORE,
                PAYROLL_TYPE, 1, "DEFINITIVE", "B_CC", new BigDecimal("3000.00"));

        Long pid = calcular(emp);

        assertEquals("CALCULATED", estado(pid));
        assertImporte(new BigDecimal("100.00"), baseReguladoraDelTramoDeBaja(pid),
                "3.000,00 entre treinta dias: art. 13.1 y 13.2 del Decreto 1646/1972");
        assertTrue(avisos(pid).isEmpty(),
                "leer un recibo cerrado es lo normal y no hay nada que avisar; avisos: " + avisos(pid));
    }

    /**
     * Recibo anterior que todavia puede cambiar: <b>este recibo no se calcula</b>.
     *
     * <p>Esta es la decision del paso, y es la que tiene que verse roja si alguien la relaja «porque
     * seguramente no cambia». Un numero que puede cambiar no se lee: la prestacion saldria de una base
     * que manana es otra, y el recibo ya estaria entregado.
     */
    @Test
    void conReciboAnteriorNoDefinitivoElReciboNoSeCalcula() {
        String emp = numeroUnico();
        long empId = altaBasica(emp, ENERO_1);
        fixtures.insertAbsence(empId, "IT_COMMON", ABRIL_10, ABRIL_12);
        fixtures.insertPayrollWithConcept(RULE_SYSTEM, EMPLOYEE_TYPE, emp, PERIOD_BEFORE,
                PAYROLL_TYPE, 1, "CALCULATED", "B_CC", new BigDecimal("3000.00"));

        Long pid = calcular(emp);

        assertEquals("NOT_VALID", estado(pid),
                "el recibo del mes anterior existe y no es definitivo: este no se calcula");
        assertEquals("PREVIOUS_PAYROLL_NOT_DEFINITIVE", motivo(pid),
                "y el motivo se guarda con el recibo, que es lo que lo hace explicable");
        assertEquals(0, (int) jdbc.queryForObject(
                        "select count(*) from payroll.payroll_concept where payroll_id = ?",
                        Integer.class, pid),
                "no se ha calculado nada, asi que no hay ni una linea");
        assertEquals(0, (int) jdbc.queryForObject(
                        "select count(*) from payroll.payroll_calculation_step where payroll_id = ?",
                        Integer.class, pid),
                "ni un paso: lo que no se calculo no deja rastro de calculo");
        assertTrue(avisos(pid).containsKey("PREVIOUS_PAYROLL_NOT_DEFINITIVE"),
                "y lo dice con palabras, no solo con un codigo de estado; avisos: " + avisos(pid));
    }

    /**
     * Sin recibo anterior y alta este mes: la base de este mes, y <b>sin aviso</b>.
     *
     * <p>Lo dice la ley: art. 13.3 del Decreto 1646/1972, «para el trabajador que haya ingresado en la
     * Empresa en el mismo mes en el que se inicie la situacion [...] referido al indicado mes». No hay
     * nada que no se sepa, asi que no hay nada que avisar.
     */
    @Test
    void sinReciboAnteriorYAltaEsteMesLaBaseEsLaDeEsteMesSinAviso() {
        String emp = numeroUnico();
        long empId = altaBasica(emp, ABRIL_5);
        fixtures.insertAbsence(empId, "IT_COMMON", ABRIL_10, ABRIL_12);

        Long pid = calcular(emp);

        assertEquals("CALCULATED", estado(pid));
        assertImporte(BASE_REGULADORA_TEORICA, baseReguladoraDelTramoDeBaja(pid));
        assertTrue(avisos(pid).isEmpty(),
                "quien entra este mes cobra sobre la base de este mes por ley; avisos: " + avisos(pid));
    }

    /** Sin recibo anterior y alta de antes: la base teorica de este mes, y el recibo <b>lo dice</b>. */
    @Test
    void sinReciboAnteriorYAltaDeAntesLaBaseEsLaTeoricaConAviso() {
        String emp = numeroUnico();
        long empId = altaBasica(emp, ENERO_1);
        fixtures.insertAbsence(empId, "IT_COMMON", ABRIL_10, ABRIL_12);

        Long pid = calcular(emp);

        assertEquals("CALCULATED", estado(pid));
        assertImporte(BASE_REGULADORA_TEORICA, baseReguladoraDelTramoDeBaja(pid));

        Map<String, String> avisos = avisos(pid);
        assertTrue(avisos.containsKey("REGULATORY_BASE_FROM_CURRENT_PERIOD"),
                "donde la salida no sabe algo del todo, lo dice; avisos: " + avisos);
        assertTrue(avisos.get("REGULATORY_BASE_FROM_CURRENT_PERIOD").contains(PERIOD_BEFORE),
                "y nombra el mes que le falta, que es lo que hace que el aviso sirva de algo: "
                        + avisos.get("REGULATORY_BASE_FROM_CURRENT_PERIOD"));
    }

    /**
     * Un empleado <b>sin baja</b> no mira el mes anterior, aunque este a medio revisar.
     *
     * <p>Es la mitad que hace que las otras digan algo. La lectura solo ocurre cuando hace falta: sin
     * baja no hay prestacion ni base durante la baja, asi que no hay ninguna razon para exigir que el
     * mes anterior de este empleado este cerrado.
     */
    @Test
    void unEmpleadoSinBajaNoMiraElMesAnterior() {
        String emp = numeroUnico();
        altaBasica(emp, ENERO_1);
        fixtures.insertPayrollWithConcept(RULE_SYSTEM, EMPLOYEE_TYPE, emp, PERIOD_BEFORE,
                PAYROLL_TYPE, 1, "CALCULATED", "B_CC", new BigDecimal("3000.00"));

        Long pid = calcular(emp);

        assertEquals("CALCULATED", estado(pid),
                "sin baja no se lee el mes anterior, asi que su estado no bloquea nada");
        assertTrue(avisos(pid).isEmpty(), "y no hay nada que avisar; avisos: " + avisos(pid));
        assertImporte(BigDecimal.ZERO, baseReguladoraCompuesta(pid),
                "la base reguladora vale cero donde no hay baja: es lo que deja contar cuantos"
                        + " recibos la tienen de verdad");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long altaBasica(String emp, LocalDate desde) {
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, desde, null);
        fixtures.insertLaborClassification(empId, desde);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), desde, null);
        return empId;
    }

    private Long calcular(String emp) {
        CalculationRun run = launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, emp),
                        null),
                null));
        assertEquals("COMPLETED", run.status(),
                "un recibo que no se calcula no es un fallo de la corrida: se cuenta como NOT_VALID");
        entityManager.flush();
        return jdbc.queryForObject(
                "select id from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = 1",
                Long.class, RULE_SYSTEM, EMPLOYEE_TYPE, emp, PERIOD, PAYROLL_TYPE);
    }

    private String estado(Long payrollId) {
        return jdbc.queryForObject("select status from payroll.payroll where id = ?",
                String.class, payrollId);
    }

    private String motivo(Long payrollId) {
        return jdbc.queryForObject("select status_reason_code from payroll.payroll where id = ?",
                String.class, payrollId);
    }

    /**
     * La base reguladora del tramo de baja, que es el unico donde vale algo.
     *
     * <p>Se pide el maximo de los pasos y no la suma: {@code BR_CC} es de ambito {@code SEGMENT} y vale
     * cero en los tramos trabajados, asi que el maximo es el del tramo de baja. Sumar daria el mismo
     * numero hoy y dejaria de darlo el dia que haya dos tramos de baja en el mes.
     */
    private BigDecimal baseReguladoraDelTramoDeBaja(Long payrollId) {
        BigDecimal max = jdbc.queryForObject(
                "select coalesce(max(amount), 0) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = 'BR_CC'",
                BigDecimal.class, payrollId);
        assertTrue(max != null, "BR_CC tiene que haberse calculado");
        return max;
    }

    private BigDecimal baseReguladoraCompuesta(Long payrollId) {
        return jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = 'BR_CC'",
                BigDecimal.class, payrollId);
    }

    /** Los avisos del recibo por codigo, sin el {@code ELIGIBLE_REAL_EXECUTION} que llevan todos. */
    private Map<String, String> avisos(Long payrollId) {
        List<Map<String, Object>> filas = jdbc.queryForList(
                "select warning_code, message from payroll.payroll_warning"
                        + " where payroll_id = ? and warning_code <> 'ELIGIBLE_REAL_EXECUTION'",
                payrollId);
        return filas.stream().collect(java.util.stream.Collectors.toMap(
                f -> (String) f.get("warning_code"), f -> (String) f.get("message")));
    }

    private static void assertImporte(BigDecimal esperado, BigDecimal real) {
        assertImporte(esperado, real, "");
    }

    private static void assertImporte(BigDecimal esperado, BigDecimal real, String porque) {
        assertEquals(0, esperado.compareTo(real),
                porque + " (esperado " + esperado + ", fue " + real + ")");
    }

    private String numeroUnico() {
        return "BR" + (System.nanoTime() % 1_000_000_000L);
    }
}
