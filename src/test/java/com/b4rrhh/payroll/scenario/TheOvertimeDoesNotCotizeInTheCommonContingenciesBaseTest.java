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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las horas extra no cotizan por contingencias comunes ({@code backend#121}).
 *
 * <h2>Lo que afirma</h2>
 *
 * <p>Que el modelo oficial tiene <b>tres</b> bases y que cada cuota lee la suya:
 *
 * <pre>
 *   comunes        B_CC = topes(B01)                              → 700, 702, 720, 724
 *   profesionales  B_CP = topes(B01 + horas extra)                → 701, 703, 721, 722, 723
 *   horas extra    B08  = horas extra                              → 704, 726
 *
 *   B01 = remuneracion mensual + prorrata
 * </pre>
 *
 * <p>El motor tenia una sola, montada en la {@code V88} para poder jugar con los topes, y las
 * nueve cuotas leian la misma. En la demo eso eran 138,78 EUR de horas extra cotizando por
 * contingencias comunes en el recibo de {@code EMP001000}.
 *
 * <h2>Por que el recibo con horas extra y el que no, y en este orden</h2>
 *
 * <p>Porque el segundo no prueba nada, y esa es justamente la leccion del {@code backend#120}:
 * sin horas extra la base de comunes y la de profesionales <b>valen lo mismo</b>, asi que un
 * escenario comodo da verde con la simplificacion puesta y con ella quitada. El primero es el
 * unico que distingue las dos.
 *
 * <p>Sobre {@code ESP} y no sobre una reglamentacion de prueba, por lo mismo que
 * {@link TheNetPayIsWhatIsEarnedMinusWhatIsDeductedTest}: el grafo de las fixtures no tiene el
 * {@code 102}, asi que alli el defecto no existia. Vivia en el catalogo que calcula la demo.
 */
@TestWebSobreEsquemaReal
class TheOvertimeDoesNotCotizeInTheCommonContingenciesBaseTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PAYROLL_TYPE  = "NORMAL";
    private static final String HORAS         = "H01";

    private static final LocalDate JANUARY_1 = LocalDate.of(2025, 1, 1);

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
     * El caso que distingue: la base de comunes se queda sin las horas extra y la de
     * profesionales las recoge.
     */
    @Test
    void theCommonBaseLeavesTheOvertimeOutAndTheProfessionalOnePicksItUp() {
        Long recibo = reciboDe(empleadoConHorasExtra(new BigDecimal("10.0000")));

        BigDecimal horasExtra = importe(recibo, "102");
        assertTrue(horasExtra.signum() > 0,
                "el escenario tiene que traer horas extra cobradas o no prueba nada; el 102 vale "
                        + horasExtra);

        BigDecimal baseComunes       = importe(recibo, "B01");
        BigDecimal baseComunesTopada = importe(recibo, "B_CC");
        BigDecimal baseProfesionales = importe(recibo, "B_CP");

        assertEquals(0, baseComunes.compareTo(
                        importe(recibo, "101").add(importe(recibo, "103")).add(importe(recibo, "B02"))),
                () -> "la base de contingencias comunes es la remuneracion mensual mas la"
                        + " prorrata, y nada mas: B01=" + baseComunes + " pero 101+103+B02="
                        + importe(recibo, "101").add(importe(recibo, "103")).add(importe(recibo, "B02"))
                        + ". Si sobran " + horasExtra + " es que las horas extra siguen"
                        + " alimentando B01, que es el defecto del backend#121");

        assertEquals(0, baseProfesionales.compareTo(baseComunes.add(horasExtra)),
                () -> "la base de profesionales es la COTIZABLE mas las horas extra, no la de"
                        + " comunes ya topada: las bases minimas por grupo son solo de comunes"
                        + " (backend#121). B_CP=" + baseProfesionales + " B01=" + baseComunes
                        + " B_CC=" + baseComunesTopada + " 102=" + horasExtra);
        assertTrue(baseComunes.compareTo(baseComunesTopada) == 0,
                "en este escenario no muerde ningun tope, asi que las dos coinciden y la"
                        + " comprobacion de arriba no distingue: el caso que las separa esta en"
                        + " EachBaseIsClampedByItsOwnLimitsTest");

        assertEquals(0, importe(recibo, "B08").compareTo(horasExtra),
                "la tercera base es el importe de las horas extra, sin topes");
    }

    /**
     * Y cada cuota lee la suya. Es la mitad que el importe de las bases no cubre: se puede tener
     * las tres bases bien calculadas y las nueve cuotas leyendo la misma.
     *
     * <p>La {@code quantity} de una linea de {@code PERCENTAGE} es su operando {@code BASE}
     * ({@code backend#93}), asi que el recibo dice sobre que base se calculo cada cuota sin
     * tener que abrir el grafo.
     */
    @Test
    void eachQuotaReadsItsOwnBase() {
        Long recibo = reciboDe(empleadoConHorasExtra(new BigDecimal("10.0000")));

        BigDecimal comunes       = importe(recibo, "B_CC");
        BigDecimal profesionales = importe(recibo, "B_CP");
        BigDecimal horasExtra    = importe(recibo, "B08");

        assertTrue(profesionales.compareTo(comunes) > 0,
                "el escenario tiene que separar las dos bases o no distingue nada: B_CC="
                        + comunes + " B_CP=" + profesionales);

        for (String cuota : new String[] {"700", "702", "720", "724"}) {
            assertEquals(0, baseDe(recibo, cuota).compareTo(comunes),
                    () -> "el " + cuota + " cotiza por contingencias comunes y tiene que leer"
                            + " B_CC=" + comunes + ", no " + baseDe(recibo, cuota));
        }
        for (String cuota : new String[] {"701", "703", "721", "722", "723"}) {
            assertEquals(0, baseDe(recibo, cuota).compareTo(profesionales),
                    () -> "el " + cuota + " es recaudacion conjunta y tiene que leer B_CP="
                            + profesionales + ", no " + baseDe(recibo, cuota));
        }
        for (String cuota : new String[] {"704", "726"}) {
            assertEquals(0, baseDe(recibo, cuota).compareTo(horasExtra),
                    () -> "el " + cuota + " es la cotizacion adicional por horas extraordinarias"
                            + " y se calcula sobre la base de horas extra=" + horasExtra
                            + ", no sobre " + baseDe(recibo, cuota));
        }

        // Y los dos tipos de la Orden de cotizacion: 4,70 % el trabajador, 23,60 % la empresa.
        assertEquals(0, tipoDe(recibo, "704").compareTo(new BigDecimal("4.70")),
                "la cotizacion adicional del trabajador es el 4,70 %");
        assertEquals(0, tipoDe(recibo, "726").compareTo(new BigDecimal("23.60")),
                "la cotizacion adicional de la empresa es el 23,60 %");

        // La de la empresa no descuenta del liquido: suma en el total de aportacion empresarial.
        BigDecimal aportacion = BigDecimal.ZERO;
        for (String cuota : new String[] {"720", "721", "722", "723", "724", "726", "727"}) {
            aportacion = aportacion.add(importe(recibo, cuota));
        }
        assertEquals(0, importe(recibo, "725").compareTo(aportacion),
                "el 725 suma sus sumandos, el 726 de horas extraordinarias y el 727 de accidentes"
                        + " de trabajo incluidos");
    }

    /**
     * Los cuatro bloques del recuadro se leen de arriba abajo, y cada uno cierra con la suma de
     * lo de encima ({@code backend#121}).
     *
     * <p>Son las invariantes nuevas del paso: sin ellas se puede imprimir un recuadro que suma a
     * la vista y no corresponde con lo que el motor uso para calcular.
     */
    @Test
    void everyBlockOfTheBoxAddsUp() {
        Long recibo = reciboDe(empleadoConHorasExtra(new BigDecimal("10.0000")));

        assertEquals(0, importe(recibo, "B01").compareTo(
                        importe(recibo, "B03").add(importe(recibo, "B04"))),
                "1. comunes: remuneracion mensual + prorrata = base de cotizacion");
        assertEquals(0, importe(recibo, "B07").compareTo(
                        importe(recibo, "B05").add(importe(recibo, "B06"))),
                "2. profesionales: base de comunes + horas extraordinarias = base de cotizacion");
        assertEquals(0, importe(recibo, "B05").compareTo(importe(recibo, "B01")),
                "2. profesionales: el apartado arranca de la base cotizable, no de la topada");
        assertEquals(0, importe(recibo, "B08").compareTo(importe(recibo, "102")),
                "3. horas extraordinarias: la base es el importe de las horas extra");
        assertEquals(0, importe(recibo, "B09").compareTo(importe(recibo, "970")),
                "4. la base sujeta a retencion del IRPF es el total devengado (ADR-070 §4)");

        // La remuneracion mensual esta definida por exclusion, no por lista: es la base de
        // cotizacion menos la prorrata. Hoy eso es el salario base, y el dia que haya un plus
        // cotizable saldra aqui sin que nadie toque el catalogo.
        assertEquals(0, importe(recibo, "B03").compareTo(importe(recibo, "101")),
                "la remuneracion mensual de este recibo es su salario base");
    }

    /**
     * Y el caso ciego, escrito para que se vea que lo es.
     *
     * <p>Sin horas extra las tres bases no se distinguen: {@code B_CP = B_CC} y {@code B08 = 0}.
     * Un test que solo mirara esto daria verde con una base o con tres.
     */
    @Test
    void withoutOvertimeTheThreeBasesAreIndistinguishable_whichIsWhyItProvesNothing() {
        Long recibo = reciboDe(empleadoSinHorasExtra());

        assertEquals(0, importe(recibo, "102").signum(), "este recibo no tiene horas extra");
        assertEquals(0, importe(recibo, "B_CP").compareTo(importe(recibo, "B_CC")),
                "sin horas extra las dos bases valen lo mismo, y por eso este caso no distingue"
                        + " el modelo bueno del simplificado");
        assertEquals(0, importe(recibo, "B08").signum(),
                "y la tercera base vale cero, asi que no se imprime");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String empleadoConHorasExtra(BigDecimal horas) {
        String emp = empleadoSinHorasExtra();
        jdbc.update("insert into employee.employee_payroll_input"
                        + " (rule_system_code, employee_type_code, employee_number, concept_code,"
                        + "  period, quantity)"
                        + " values (?, ?, ?, ?, ?, ?)",
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, Integer.parseInt(PERIOD), horas);
        return emp;
    }

    private String empleadoSinHorasExtra() {
        String emp = "BB" + (System.nanoTime() % 1_000_000_000L);
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, JANUARY_1, null);
        fixtures.insertLaborClassification(empId, JANUARY_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), JANUARY_1, null);
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

    /** El importe de periodo de un concepto, sumando sus tramos si los tiene. */
    private BigDecimal importe(Long payrollId, String conceptCode) {
        return jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, conceptCode);
    }

    /** Sobre que base se calculo una cuota: la {@code quantity} de su paso. */
    private BigDecimal baseDe(Long payrollId, String conceptCode) {
        return jdbc.queryForObject(
                "select coalesce(sum(quantity), 0) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, conceptCode);
    }

    /** Con que tipo: la {@code rate} de su paso. */
    private BigDecimal tipoDe(Long payrollId, String conceptCode) {
        return jdbc.queryForObject(
                "select max(rate) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, conceptCode);
    }
}
