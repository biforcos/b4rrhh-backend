package com.b4rrhh.payroll.scenario;

import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.OperandRole;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import com.b4rrhh.payroll_engine.metamodel.domain.port.RuleSystemMetamodelRepository;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cada base se recorta con los topes que son suyos ({@code backend#121}).
 *
 * <h2>Lo que protege</h2>
 *
 * <p>Con tres bases hay dos juegos de topes, y el parecido entre ellos es la trampa: <b>el tope
 * maximo es el mismo numero</b> y el minimo no. El de contingencias comunes es la base minima
 * <i>del grupo de cotizacion</i> del empleado; el de profesionales es el <i>tope minimo de
 * cotizacion</i> de la Orden, igual para los once grupos.
 *
 * <p>Una base de profesionales cableada al tope de comunes sale bien para los grupos bajos
 * —donde las dos cifras coinciden— y mal para los altos, sin que nada falle. Este test mira las
 * dos cosas: que el grafo enchufa cada cadena a sus nodos, y que esos nodos resuelven filas
 * distintas en un recibo de verdad.
 *
 * <h2>Y que la tercera base no tiene ninguno</h2>
 *
 * <p>La cotizacion adicional por horas extraordinarias se calcula sobre el importe de las horas,
 * sin recortar. Que «no tiene topes» sea una afirmacion y no un olvido se ve poniendo la base de
 * comunes por encima del tope maximo: la de horas extra se queda como estaba.
 */
@TestWebSobreEsquemaReal
class EachBaseIsClampedByItsOwnLimitsTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PAYROLL_TYPE  = "NORMAL";
    private static final String HORAS         = "H01";

    /** La categoria del grupo 01, cuya base minima (1.847,40) no es el tope minimo (1.323,00). */
    private static final String CATEGORIA_GRUPO_01 = "99002405-G1";

    private static final LocalDate JANUARY_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate HOY       = LocalDate.of(2026, 9, 30);

    @Autowired
    private LaunchPayrollCalculationUseCase launch;

    @Autowired
    private RuleSystemMetamodelRepository reglamentaciones;

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
     * Las dos cadenas de recorte, en el catalogo: primero el techo, despues el suelo, y cada una
     * con sus propios nodos de tope.
     *
     * <p>Rojo si alguien quita un {@code LEAST} o un {@code GREATEST}, y rojo tambien —que es lo
     * que un test de importes no pilla— si alguien cablea la base de profesionales al tope de
     * comunes.
     */
    @Test
    void eachChainClampsWithItsOwnLimitNodes() {
        RuleSystemMetamodel esp = reglamentaciones.load(RULE_SYSTEM, HOY);

        assertClamp(esp, "B_CC_MAX", CalculationType.LEAST,    "B01",      "P_TOPE_MAX");
        assertClamp(esp, "B_CC",     CalculationType.GREATEST, "B_CC_MAX", "P_TOPE_MIN");
        assertClamp(esp, "B_CP_MAX", CalculationType.LEAST,    "B07",      "P_TOPE_MAX_CP");
        assertClamp(esp, "B_CP",     CalculationType.GREATEST, "B_CP_MAX", "P_TOPE_MIN_CP");

        // Y la tercera base no pasa por ninguna cadena: nadie la recorta.
        assertTrue(esp.operandsOf("B08").isEmpty(),
                "la base de horas extraordinarias no tiene operandos: es lo que se alimenta de las"
                        + " horas y nada mas");
    }

    /**
     * Y los dos nodos de tope minimo resuelven filas distintas en un recibo del grupo 01.
     *
     * <p>Es la mitad que la estructura no cubre: se pueden tener los cuatro nodos bien enchufados
     * y que el de profesionales lea la fila de comunes. Con un grupo alto las dos cifras se
     * separan 524,40 EUR y el fallo deja de ser invisible.
     */
    @Test
    void theTwoMinimumLimitsResolveDifferentRowsForAHighGroup() {
        Long recibo = reciboDe(empleadoDelGrupo01());

        assertEquals(0, importe(recibo, "P_TOPE_MIN").compareTo(new BigDecimal("1847.40")),
                "el tope minimo de comunes es la base minima del grupo 01");
        assertEquals(0, importe(recibo, "P_TOPE_MIN_CP").compareTo(new BigDecimal("1323.00")),
                "el de profesionales es el tope minimo de cotizacion, que no es el del grupo;"
                        + " si sale 1847,40 es que esta leyendo la fila de comunes");
    }

    /**
     * El suelo de contingencias comunes muerde, y <b>la base de profesionales no se entera</b>.
     *
     * <p>Media jornada deja la base por debajo del minimo del grupo. Sin el {@code GREATEST} de
     * la cadena de comunes, la base de comunes se quedaria en lo devengado y las cuatro cuotas de
     * comunes bajarian con ella.
     *
     * <p>Y la otra mitad, que es la que este test existe para sujetar desde la correccion del
     * {@code backend#121}: <b>las bases minimas por grupo son solo de contingencias comunes</b>.
     * La de profesionales se determina con las mismas normas —remuneracion mas prorrata mas horas
     * extraordinarias— y se limita por el tope minimo y el maximo, no por el minimo del grupo
     * (Orden PJC/297/2026, arts. 1.2 y 2.2). Apoyarla en la de comunes ya topada le metia por la
     * puerta de atras un minimo que no le toca.
     */
    @Test
    void whenTheCommonFloorBitesTheProfessionalBaseDoesNotInheritIt() {
        Long recibo = reciboDe(empleadoAMediaJornadaConHorasExtra());

        BigDecimal devengada = importe(recibo, "B01");
        BigDecimal topada    = importe(recibo, "B_CC");
        assertTrue(devengada.compareTo(topada) < 0,
                "el escenario tiene que quedarse por debajo del minimo o no prueba el suelo:"
                        + " B01=" + devengada + " B_CC=" + topada);
        assertEquals(0, topada.compareTo(new BigDecimal("1323.00")),
                "y el suelo es la base minima del grupo 05");

        assertEquals(0, importe(recibo, "B05").compareTo(devengada),
                () -> "el apartado 2 arranca de la base COTIZABLE y no de la topada: B05="
                        + importe(recibo, "B05") + " tiene que ser B01=" + devengada
                        + " y no B_CC=" + topada);
        assertEquals(0, importe(recibo, "B07").compareTo(devengada.add(importe(recibo, "102"))),
                "y la suma del apartado 2 es esa base mas las horas extraordinarias");
    }

    /**
     * El caso que no se puede arreglar sumando horas: un grupo alto por debajo de su minimo.
     *
     * <p>El grupo 01 tiene la base minima mas alta del catalogo —1.847,40— y el tope minimo de
     * cotizacion es mucho menor. Un salario por debajo de ese minimo deja <b>la base de
     * profesionales por DEBAJO de la de comunes</b>, y ninguna cantidad de horas extra las
     * iguala.
     *
     * <p>Es el caso que el cableado anterior no podia dar: con {@code B05 = B_CC}, la base
     * profesional era la de comunes mas algo, o sea siempre mayor o igual. Rojo con aquel
     * cableado y verde con este.
     */
    @Test
    void aHighGroupBelowItsOwnMinimumHasAProfessionalBaseLowerThanTheCommonOne() {
        Long recibo = reciboDe(empleadoDelGrupo01ConLaBaseEntreLosDosMinimos());

        BigDecimal cotizable     = importe(recibo, "B01");
        BigDecimal comunes       = importe(recibo, "B_CC");
        BigDecimal profesionales = importe(recibo, "B_CP");

        assertEquals(0, comunes.compareTo(new BigDecimal("1847.40")),
                () -> "el escenario tiene que tocar el minimo del grupo 01 o no prueba nada:"
                        + " B01=" + cotizable + " B_CC=" + comunes);
        assertTrue(cotizable.compareTo(comunes) < 0, "y quedarse por debajo de el");
        assertTrue(cotizable.compareTo(new BigDecimal("1323.00")) > 0,
                () -> "y POR ENCIMA del tope minimo de profesionales, o el suelo mordería"
                        + " tambien ahi y el caso dejaria de distinguir: B01=" + cotizable);

        assertEquals(0, profesionales.compareTo(cotizable),
                () -> "sin horas extra, la base de profesionales es la base cotizable: B_CP="
                        + profesionales + " tiene que ser " + cotizable);
        assertTrue(profesionales.compareTo(comunes) < 0,
                () -> "y queda POR DEBAJO de la de comunes, que es lo que el cableado anterior no"
                        + " podia dar: B_CP=" + profesionales + " B_CC=" + comunes);
    }

    /**
     * El techo muerde en las dos, y es el mismo numero.
     *
     * <p>Con la base de comunes ya en el maximo, sumarle las horas extra dejaria la de
     * profesionales por encima del tope. El {@code LEAST} de su cadena la devuelve al maximo;
     * sin el, la empresa cotizaria por encima del tope maximo legal y nada se quejaria.
     *
     * <p>La base de horas extraordinarias, mientras tanto, no se toca: no tiene topes.
     */
    @Test
    void theCeilingBitesOnBothBasesAndLeavesTheOvertimeBaseAlone() {
        fixtures.setDailyRate(RULE_SYSTEM, new BigDecimal("200.00"));
        Long recibo = reciboDe(empleadoConHorasExtra(new BigDecimal("10.0000")));

        BigDecimal horasExtra = importe(recibo, "102");
        assertTrue(horasExtra.signum() > 0, "hacen falta horas extra para separar las dos bases");
        assertTrue(importe(recibo, "B01").compareTo(new BigDecimal("4909.50")) > 0,
                "el escenario tiene que pasarse del tope maximo o no prueba el techo: B01="
                        + importe(recibo, "B01"));

        assertEquals(0, importe(recibo, "B_CC").compareTo(new BigDecimal("4909.50")),
                "la base de comunes se queda en el tope maximo");
        assertEquals(0, importe(recibo, "B_CP").compareTo(new BigDecimal("4909.50")),
                () -> "y la de profesionales tambien: sin su LEAST valdria "
                        + importe(recibo, "B07") + ", que es el tope mas las horas extra");

        assertEquals(0, importe(recibo, "B08").compareTo(horasExtra),
                "la base de horas extraordinarias no se recorta: no tiene topes");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void assertClamp(
            RuleSystemMetamodel esp, String conceptCode, CalculationType tipo,
            String izquierda, String derecha) {

        PayrollConcept concepto = esp.findConcept(conceptCode).orElse(null);
        assertNotNull(concepto, "el catalogo ESP tiene que declarar " + conceptCode);
        assertEquals(tipo, concepto.getCalculationType(),
                conceptCode + " recorta con " + tipo);

        Map<OperandRole, String> operandos = esp.operandsOf(conceptCode).stream()
                .collect(Collectors.toMap(
                        PayrollConceptOperand::getOperandRole,
                        o -> o.getSourceObject().getObjectCode()));
        assertEquals(izquierda, operandos.get(OperandRole.LEFT),
                conceptCode + " recorta " + izquierda);
        assertEquals(derecha, operandos.get(OperandRole.RIGHT),
                () -> conceptCode + " se recorta con " + derecha + " y no con "
                        + operandos.get(OperandRole.RIGHT) + ": el tope minimo de las"
                        + " contingencias profesionales no es la base minima del grupo");
    }

    private String empleadoConHorasExtra(BigDecimal horas) {
        return conHorasExtra(empleado(new BigDecimal("100.00"), null), horas);
    }

    private String empleadoAMediaJornadaConHorasExtra() {
        return conHorasExtra(empleado(new BigDecimal("50.00"), null), new BigDecimal("10.0000"));
    }

    private String empleadoDelGrupo01() {
        return empleado(new BigDecimal("100.00"), CATEGORIA_GRUPO_01);
    }

    /**
     * Grupo 01 al 60 % de jornada, que es la ventana que hace falta: su remuneracion queda
     * <b>entre</b> el tope minimo de cotizacion (1.323,00) y la base minima de su grupo
     * (1.847,40), asi que el suelo de comunes muerde y el de profesionales no.
     *
     * <p>A media jornada no valdria: la base caeria por debajo de los dos y las dos subirian.
     */
    private String empleadoDelGrupo01ConLaBaseEntreLosDosMinimos() {
        return empleado(new BigDecimal("60.00"), CATEGORIA_GRUPO_01);
    }

    private String conHorasExtra(String emp, BigDecimal horas) {
        jdbc.update("insert into employee.employee_payroll_input"
                        + " (rule_system_code, employee_type_code, employee_number, concept_code,"
                        + "  period, quantity)"
                        + " values (?, ?, ?, ?, ?, ?)",
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, Integer.parseInt(PERIOD), horas);
        return emp;
    }

    private String empleado(BigDecimal jornada, String categoria) {
        String emp = "TP" + (System.nanoTime() % 1_000_000_000L);
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, JANUARY_1, null);
        if (categoria == null) {
            fixtures.insertLaborClassification(empId, JANUARY_1);
        } else {
            fixtures.insertLaborClassification(empId, JANUARY_1, null, categoria);
        }
        fixtures.insertWorkingTime(empId, jornada, JANUARY_1, null);
        fixtures.insertExtraPaymentRegime(empId, false, JANUARY_1, null);
        return emp;
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

        return jdbc.queryForObject(
                "select id from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and status = 'CALCULATED'",
                Long.class, RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE);
    }

    private BigDecimal importe(Long payrollId, String conceptCode) {
        return jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, conceptCode);
    }
}
