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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El tipo de accidentes de trabajo sale de la actividad de la empresa ({@code backend#122}).
 *
 * <h2>Lo que protege</h2>
 *
 * <p>Es el <b>unico tipo de este catalogo que no es el mismo para todo el mundo</b>. Los otros
 * once se leen de {@code ss_cotizacion_tipos} por contingencia y por fecha, y dan lo mismo para
 * cualquier empleado; este se lee de la tarifa de primas por la actividad economica de la empresa,
 * porque la prima de accidentes es el precio del riesgo de esa actividad.
 *
 * <p>Por eso un escenario de un solo empleado no prueba nada aqui: con una sola empresa, un tipo
 * escrito a mano da exactamente el mismo recibo. <b>Hacen falta dos empresas.</b>
 *
 * <h2>Y la forma de la tarifa</h2>
 *
 * <p>La tarifa no lista todos los CNAE: lista entradas de dos, tres o cuatro digitos y la mas
 * especifica gana —«47 (excepto 473, 4781, 4782 y 4783)» es exactamente eso—. Buscar por
 * igualdad habria funcionado con las cuatro empresas de la semilla y habria dejado sin tipo a la
 * primera con un CNAE que nadie hubiera sembrado.
 */
@TestWebSobreEsquemaReal
class TheAtEpRateComesFromTheCompanyActivityTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final String EMPRESA_UNA = "ES01";
    private static final String EMPRESA_DOS = "ES02";

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
     * Dos empresas, dos actividades, dos cuotas. El mismo empleado en todo lo demas.
     *
     * <p>Rojo si el {@code 727} lee un tipo fijo: los dos recibos saldrian con la misma cuota y
     * con el mismo tipo, y no habria manera de notarlo mirando uno solo.
     */
    @Test
    void twoCompaniesWithDifferentActivitiesPayDifferentAtEpQuotas() {
        // Dos actividades de laboratorio, con tipos que no se parecen a los de la tarifa real
        // para que un numero escrito a mano en el codigo no pueda coincidir por casualidad.
        fixtures.seedTarifaPrimaAt(RULE_SYSTEM, "0011", new BigDecimal("1.00"), new BigDecimal("0.50"));
        fixtures.seedTarifaPrimaAt(RULE_SYSTEM, "0022", new BigDecimal("3.20"), new BigDecimal("2.80"));
        fixtures.setCompanyCnae(RULE_SYSTEM, EMPRESA_UNA, "0011");
        fixtures.setCompanyCnae(RULE_SYSTEM, EMPRESA_DOS, "0022");

        Long enLaUna = reciboDe(empleadoDe(EMPRESA_UNA));
        Long enLaDos = reciboDe(empleadoDe(EMPRESA_DOS));

        assertEquals(0, importe(enLaUna, "B_CP").compareTo(importe(enLaDos, "B_CP")),
                "los dos empleados son identicos: la base de profesionales tiene que ser la misma,"
                        + " o lo que se compara despues no es el tipo");

        assertEquals(0, tipoDe(enLaUna, "727").compareTo(new BigDecimal("1.50")),
                "el tipo es la suma de IT y IMS de la actividad de SU empresa: 1,00 + 0,50");
        assertEquals(0, tipoDe(enLaDos, "727").compareTo(new BigDecimal("6.00")),
                "y el de la otra, 3,20 + 2,80");

        assertNotEquals(0, importe(enLaUna, "727").compareTo(importe(enLaDos, "727")),
                "misma base y tipos distintos tienen que dar cuotas distintas; si salen iguales,"
                        + " el 727 no esta leyendo la actividad de la empresa");

        // Y la cuota es la base de profesionales por ese tipo, no por otra base.
        assertEquals(0, baseDe(enLaUna, "727").compareTo(importe(enLaUna, "B_CP")),
                "la cuota de accidentes se calcula sobre la base de contingencias profesionales");
    }

    /**
     * Y suma en el total de aportacion empresarial, sin tocar lo que descuenta el trabajador.
     *
     * <p>Es una cuota <b>exclusivamente</b> a cargo de la empresa: no hay parte del trabajador en
     * accidentes de trabajo. Si apareciera en el {@code 980}, al empleado se le estaria
     * descontando el seguro de su propio riesgo laboral.
     */
    @Test
    void theAtEpQuotaAddsToTheEmployerTotalAndNeverToTheDeductions() {
        fixtures.seedTarifaPrimaAt(RULE_SYSTEM, "0011", new BigDecimal("1.00"), new BigDecimal("0.50"));
        fixtures.setCompanyCnae(RULE_SYSTEM, EMPRESA_UNA, "0011");

        Long recibo = reciboDe(empleadoDe(EMPRESA_UNA));

        BigDecimal cuota = importe(recibo, "727");
        assertTrue(cuota.signum() > 0, "el escenario tiene que dar una cuota o no prueba nada");

        BigDecimal aportacion = BigDecimal.ZERO;
        for (String c : new String[] {"720", "721", "722", "723", "724", "726", "727"}) {
            aportacion = aportacion.add(importe(recibo, c));
        }
        assertEquals(0, importe(recibo, "725").compareTo(aportacion),
                "el 725 suma la cuota de accidentes con las demas de empresa");

        BigDecimal deducciones = BigDecimal.ZERO;
        for (String c : new String[] {"700", "701", "702", "703", "704", "800"}) {
            deducciones = deducciones.add(importe(recibo, c));
        }
        assertEquals(0, importe(recibo, "980").compareTo(deducciones),
                "y el total a deducir son las cuotas del trabajador y la retencion, y ninguna mas:"
                        + " los accidentes de trabajo son de la empresa");
    }

    /**
     * La entrada mas especifica gana, que es como la tarifa declara sus excepciones.
     *
     * <p>Una empresa con CNAE {@code 4781} encuentra la entrada {@code 4781} aunque la {@code 47}
     * tambien la cubra. Sin esto, la excepcion de la tarifa no se aplicaria nunca y la empresa
     * cotizaria por el tipo de la division.
     */
    @Test
    void theMostSpecificTariffEntryWins() {
        fixtures.seedTarifaPrimaAt(RULE_SYSTEM, "0033", new BigDecimal("1.00"), new BigDecimal("1.00"));
        fixtures.seedTarifaPrimaAt(RULE_SYSTEM, "003311", new BigDecimal("4.00"), new BigDecimal("1.00"));
        fixtures.setCompanyCnae(RULE_SYSTEM, EMPRESA_UNA, "003311");

        Long recibo = reciboDe(empleadoDe(EMPRESA_UNA));

        assertEquals(0, tipoDe(recibo, "727").compareTo(new BigDecimal("5.00")),
                "gana la entrada de seis digitos (4,00 + 1,00); si sale 2,00 esta ganando la de"
                        + " cuatro, que tambien cubre a este CNAE pero es menos especifica");
    }

    /**
     * Sin actividad economica, la corrida falla y dice por que.
     *
     * <p>La salida facil era devolver cero. No vale: la regla del cero no imprimiria la linea, asi
     * que el recibo saldria <b>sin cuota de accidentes y sin nada que lo dijera</b> — un numero
     * equivocado que no falla, que es la clase de defecto que este proyecto persigue.
     */
    @Test
    void withoutAnActivityTheRunFailsInsteadOfContributingZero() {
        fixtures.setCompanyCnae(RULE_SYSTEM, EMPRESA_UNA, null);

        String emp = empleadoDe(EMPRESA_UNA);
        CalculationRun run = lanzar(emp);

        assertEquals("COMPLETED_WITH_ERRORS", run.status(),
                "una empresa sin CNAE es catalogo mal parametrizado, no un caso de negocio");
        List<Map<String, Object>> mensajes = jdbc.queryForList(
                "select message_code, message from payroll.calculation_run_message where run_id = ?",
                run.id());
        assertTrue(mensajes.stream().anyMatch(m -> String.valueOf(m.get("message")).contains("CNAE")),
                "y el mensaje tiene que nombrar lo que falta: " + mensajes);
    }

    /**
     * Las excepciones que la propia tarifa nombra tienen su fila, y ganan ({@code backend#122}).
     *
     * <p>La fila del {@code 47} dice «excepto 473, 4781, 4782 y 4783». Sin esas filas, una empresa
     * de combustible para la automocion resolvia al {@code 47} y cotizaba al <b>1,65</b> en vez de
     * al <b>1,85</b>: verde y en falso, porque el texto de la fila decia lo contrario de lo que la
     * tabla podia hacer. <b>Si una fila nombra una excepcion, la excepcion tiene que tener su
     * fila</b>, o el texto es un adorno.
     *
     * <p>El issue pedia este test con el {@code 4773}, y ese codigo no esta en la lista: el
     * {@code 4773} es el comercio al por menor de productos farmaceuticos y cotiza por el
     * {@code 47} como cualquier otro. La excepcion del combustible es el {@code 473} —tres
     * digitos— y es la que se prueba aqui. Rojo antes de la {@code V152}, con 1,65.
     */
    @Test
    void anExceptionNamedByTheTariffHasItsOwnRowAndWins() {
        fixtures.setCompanyCnae(RULE_SYSTEM, EMPRESA_UNA, "4730");

        Long recibo = reciboDe(empleadoDe(EMPRESA_UNA));

        assertEquals(0, tipoDe(recibo, "727").compareTo(new BigDecimal("1.85")),
                () -> "el 4730 cae bajo la excepcion 473 (1,00 de IT + 0,85 de IMS). Si sale 1,65"
                        + " es que ha resuelto a la fila del 47, que dice expresamente que no le"
                        + " cubre");
    }

    /**
     * Y las cuatro que nombra estan, en los dos ejercicios que la tabla declara.
     *
     * <p>La mitad estructural: el test de arriba prueba una, y ésta prueba que no falta ninguna.
     * Una excepcion sin fila no falla —resuelve a la division y cobra de menos o de mas—, asi que
     * nadie se entera hasta que alguien mira un recibo concreto de esa actividad.
     */
    @Test
    void everyExceptionTheTariffNamesHasARowInBothPeriods() {
        for (String cnae : new String[] {"473", "4781", "4782", "4783"}) {
            Integer filas = jdbc.queryForObject(
                    "select count(*) from payroll_engine.ss_tarifa_primas_at"
                            + " where rule_system_code = ? and cnae_code = ?",
                    Integer.class, RULE_SYSTEM, cnae);
            assertEquals(2, filas,
                    () -> "la excepcion " + cnae + " que la fila del 47 nombra tiene que tener su"
                            + " fila en los dos ejercicios de la tabla, y tiene " + filas);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String empleadoDe(String companyCode) {
        String emp = "AT" + (System.nanoTime() % 1_000_000_000L);
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, companyCode, JANUARY_1, null);
        fixtures.insertLaborClassification(empId, JANUARY_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), JANUARY_1, null);
        fixtures.insertExtraPaymentRegime(empId, false, JANUARY_1, null);
        return emp;
    }

    private CalculationRun lanzar(String employeeNumber) {
        return launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employeeNumber),
                        null),
                null));
    }

    private Long reciboDe(String employeeNumber) {
        CalculationRun run = lanzar(employeeNumber);
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

    private BigDecimal baseDe(Long payrollId, String conceptCode) {
        return jdbc.queryForObject(
                "select coalesce(sum(quantity), 0) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, conceptCode);
    }

    private BigDecimal tipoDe(Long payrollId, String conceptCode) {
        return jdbc.queryForObject(
                "select max(rate) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, conceptCode);
    }
}
