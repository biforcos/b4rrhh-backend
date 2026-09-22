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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>La base no sabe si se pago</b> ({@code backend#119}, paso 4 de {@code b4rrhh/workspace#9},
 * ADR-070).
 *
 * <h2>Lo que afirma</h2>
 *
 * <p>Para el mismo salario, la base de cotizacion es <b>identica</b> en los dos regimenes de pagas
 * extras. Lo que cambia es la puerta por la que la prorrata entra en el recibo: en regimen
 * prorrateado entra como devengo —se paga, tributa y sube el total devengado—; en el otro entra
 * como linea del recuadro de bases, que cotiza y no se paga.
 *
 * <h2>Por que es cierta por construccion</h2>
 *
 * <p>Los dos conceptos, {@code 103} y {@code B02}, son el mismo importe multiplicado por dos
 * coeficientes que <b>suman uno siempre</b>, y <b>los dos alimentan {@code B01}</b>. Asi la base
 * recibe la prorrata exactamente una vez sea cual sea el regimen, y no hace falta que nadie se
 * acuerde de nada.
 *
 * <p>Esto no quita el test: que algo sea cierto por construccion se comprueba igual, porque la
 * construccion se puede deshacer sin querer —basta con que alguien le quite un feed a {@code B01}
 * o cambie un coeficiente— y ese es justo el dia en que hace falta que alguien lo diga.
 *
 * <h2>Los tres casos</h2>
 *
 * <p>El gemelo —mismo salario, distinto regimen— es el que ensena la invariante. El del mes
 * partido es el que ensena que el regimen vive en el tramo y no en el empleado. Y el del tope es
 * el que ensena que no hubo que hacer nada para que los topes siguieran mordiendo.
 */
@TestWebSobreEsquemaReal
class TheBaseDoesNotKnowWhetherTheProrationWasPaidTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final LocalDate JANUARY_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate APRIL_15  = LocalDate.of(2025, 4, 15);
    private static final LocalDate APRIL_16  = LocalDate.of(2025, 4, 16);

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
     * El gemelo del issue: dos empleados iguales en todo salvo en el regimen.
     */
    @Test
    void twoEmployeesWithTheSameSalaryAndDifferentRegimesShareTheirBaseAndNotTheirNetPay() {
        Long prorrateado   = reciboDe(empleadoConRegimen(true));
        Long noProrrateado = reciboDe(empleadoConRegimen(false));

        assertEquals(importe(noProrrateado, "B01"), importe(prorrateado, "B01"),
                "la base cotizable es la misma: la prorrata entra en ella por las dos puertas");
        assertEquals(importe(noProrrateado, "B_CC"), importe(prorrateado, "B_CC"),
                "y con ella la base de contingencias comunes");
        assertEquals(importe(noProrrateado, "700"), importe(prorrateado, "700"),
                "y las cuotas, que salen de B_CC");

        assertNotEquals(importe(noProrrateado, "990"), importe(prorrateado, "990"),
                "el liquido si cambia: al prorrateado se le paga la prorrata y al otro no");
        assertNotEquals(importe(noProrrateado, "800"), importe(prorrateado, "800"),
                "y la retencion tambien: se retiene sobre lo que se paga, no sobre lo que cotiza");

        // Y a cada uno le entra por UNA de las dos puertas, no por las dos ni por ninguna.
        assertEquals(List.of("103"), puertasConImporte(prorrateado),
                "al prorrateado la prorrata le entra por los devengos, que es donde se le paga");
        assertEquals(List.of("B02"), puertasConImporte(noProrrateado),
                "y al otro por la puerta que solo cotiza");

        // Y el recuadro de bases ensena la misma linea a los dos: la prorrata del mes, venga por
        // donde venga. Antes del backend#121 imprimia el B02, o sea la prorrata SOLO a quien no la
        // cobra; a quien la cobra no le salia ninguna, porque su B02 vale cero.
        assertEquals(List.of("B04"), lineasDeProrrataEnElRecuadro(prorrateado),
                "el recuadro de bases ensena una linea de prorrata");
        assertEquals(List.of("B04"), lineasDeProrrataEnElRecuadro(noProrrateado),
                "y la misma, en los dos regimenes");
        assertEquals(importe(noProrrateado, "B04"), importe(prorrateado, "B04"),
                "y con el mismo importe, que es la invariante de este paso vista en el papel");
    }

    /**
     * La cuenta, escrita: la prorrata es la suma de las cuatro pagas entre doce, y <b>entre doce</b>
     * y no un 8,33 % que redondearia dos veces ({@code backend#61}).
     */
    @Test
    void theProrationIsTheSumOfTheFourExtraPaymentsDividedByTwelve() {
        Long recibo = reciboDe(empleadoConRegimen(true));

        BigDecimal sumaDeLasPagas = BigDecimal.ZERO;
        for (String paga : List.of("PE_1", "PE_2", "PE_3", "PE_4")) {
            sumaDeLasPagas = sumaDeLasPagas.add(pasoDe(recibo, paga));
        }

        assertEquals(0, sumaDeLasPagas.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP)
                        .compareTo(pasoDe(recibo, "P_PRORRATA")),
                "(PE_1 + PE_2 + PE_3 + PE_4) / 12");
        assertEquals(0, pasoDe(recibo, "P_PRORRATA").compareTo(importe(recibo, "103")),
                "y eso es lo que se paga, sin nada en medio");
        assertEquals(0, BigDecimal.valueOf(12).compareTo(pasoDe(recibo, "P_MESES_ANO")),
                "el divisor es un concepto del grafo y vale doce, no un 8,33 escrito a mano");
    }

    /**
     * El regimen vive en el tramo, no en el empleado: quien cambia a mitad de mes cobra media
     * prorrata por una puerta y media por la otra, y su base sigue sin enterarse.
     */
    @Test
    void anEmployeeWhoChangesRegimeMidMonthUsesBothDoors_andTheBaseStillDoesNotKnow() {
        String emp = numeroUnico();
        long empId = altaBasica(emp);
        fixtures.insertExtraPaymentRegime(empId, false, JANUARY_1, APRIL_15);
        fixtures.insertExtraPaymentRegime(empId, true, APRIL_16, null);
        Long partido = reciboDe(emp);

        assertEquals(List.of("103", "B02"), puertasConImporte(partido),
                "las dos puertas en el mismo recibo, una por tramo");

        // Y la suma de las dos es la prorrata entera del mes: lo que cambia es por donde entra,
        // nunca cuanto.
        Long entero = reciboDe(empleadoConRegimen(true));
        assertEquals(0, importe(partido, "103").add(importe(partido, "B02"))
                        .compareTo(importe(entero, "103")),
                "103 + B02 del mes partido es la prorrata entera de un mes sin partir");
        assertEquals(importe(entero, "B01"), importe(partido, "B01"),
                "y la base cotizable es la misma que la de quien no cambio de regimen");
    }

    /**
     * Los topes: no habia nada que hacer, y esto es lo que lo demuestra.
     *
     * <p>{@code B_CC_MAX} es {@code LEAST(B01, P_TOPE_MAX)} y se aplica <b>despues</b> de
     * {@code B01}, asi que la prorrata entra en la base y el tope la recorta sin que nadie lo haya
     * programado. El caso es un salario que <b>solo con la prorrata</b> pasa del tope: sin ella
     * la base estaria por debajo y el tope no mordería.
     */
    @Test
    void theContributionCapBitesOnTheBaseThatAlreadyIncludesTheProration() {
        // El 300 no es una jornada: es el unico multiplicador que la fixture tiene a mano para
        // llegar a un salario por encima del tope. Lo que el caso necesita es una base que pase
        // del techo por la prorrata y no sin ella, y con el precio de dia de la categoria de
        // prueba —47,50— eso pide un salario de unos 4.275 euros: por debajo del tope, y por
        // encima al sumarle su tercio.
        Long recibo = reciboDe(empleadoConRegimen(true, new BigDecimal("300.00")));

        BigDecimal b01   = importe(recibo, "B01");
        BigDecimal bcc   = importe(recibo, "B_CC");
        BigDecimal tope  = pasoDe(recibo, "P_TOPE_MAX");

        assertTrue(b01.compareTo(tope) > 0,
                "el caso pide una base por encima del tope maximo; si no lo esta, el salario del "
                        + "escenario se ha quedado corto y el test no prueba nada. B01=" + b01
                        + " tope=" + tope);
        assertEquals(0, bcc.compareTo(tope),
                "la base de cotizacion es el tope, no B01: el LEAST se aplica sobre la base que ya "
                        + "lleva la prorrata dentro");

        // Y la mitad que lo hace valer algo: sin la prorrata, esta misma base NO llegaria al tope.
        assertTrue(b01.subtract(importe(recibo, "103")).compareTo(tope) < 0,
                "sin la prorrata la base quedaria por debajo del tope, asi que lo que lo hace "
                        + "morder es justo lo que este issue anade");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String empleadoConRegimen(boolean prorateadas) {
        return empleadoConRegimen(prorateadas, new BigDecimal("100.00"));
    }

    private String empleadoConRegimen(boolean prorateadas, BigDecimal jornada) {
        String emp = numeroUnico();
        long empId = altaBasica(emp, jornada);
        fixtures.insertExtraPaymentRegime(empId, prorateadas, JANUARY_1, null);
        return emp;
    }

    private long altaBasica(String emp) {
        return altaBasica(emp, new BigDecimal("100.00"));
    }

    private long altaBasica(String emp, BigDecimal jornada) {
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, JANUARY_1, null);
        fixtures.insertLaborClassification(empId, JANUARY_1);
        fixtures.insertWorkingTime(empId, jornada, JANUARY_1, null);
        return empId;
    }

    private String numeroUnico() {
        return "PB" + (System.nanoTime() % 1_000_000_000L);
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
        BigDecimal total = jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, conceptCode);
        return total.stripTrailingZeros();
    }

    /** Lo mismo, pero para los tecnicos que no se suman: el valor del ultimo tramo. */
    private BigDecimal pasoDe(Long payrollId, String conceptCode) {
        return jdbc.queryForObject(
                "select amount from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = ?"
                        + " order by execution_order desc limit 1",
                BigDecimal.class, payrollId, conceptCode);
    }

    /** Que lineas de prorrata acabaron impresas en el recibo. */
    /**
     * Las puertas por las que entro la prorrata en este recibo: las que valen algo.
     *
     * <p>Miraba las <b>lineas</b> del recibo, y desde el {@code backend#121} eso ya no sirve para
     * el {@code B02}: el recuadro de bases imprime una sola linea de prorrata —el {@code B04},
     * que suma las dos puertas— en vez de la puerta que toque. Eso es justo lo que aquel issue
     * arreglo, porque el recuadro le ensenaba la prorrata a quien no la cobra y no a quien si.
     *
     * <p>Lo que este test afirma no cambia: las puertas siguen siendo dos, sigue entrando por
     * UNA de las dos en cada tramo, y la base sigue sin enterarse. Se mira en el paso, que es
     * donde el hecho vive; la regla del cero es de la impresion y tiene su propio test.
     */
    private List<String> puertasConImporte(Long payrollId) {
        return jdbc.queryForList(
                "select concept_code from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code in ('103', 'B02') and amount <> 0"
                        + " group by concept_code order by concept_code",
                String.class, payrollId);
    }

    /** Lo que el recuadro de bases ensena como prorrata: una linea, y la misma en los dos regimenes. */
    private List<String> lineasDeProrrataEnElRecuadro(Long payrollId) {
        return jdbc.queryForList(
                "select concept_code from payroll.payroll_concept"
                        + " where payroll_id = ? and concept_code in ('103', 'B02', 'B04')"
                        + "   and payslip_section_code = 'BASES' order by concept_code",
                String.class, payrollId);
    }
}
