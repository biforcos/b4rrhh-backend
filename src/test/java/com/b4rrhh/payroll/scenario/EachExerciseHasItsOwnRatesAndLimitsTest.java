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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cada ejercicio tiene sus tipos y sus topes, y el recibo coge los de su fecha
 * ({@code backend#123}).
 *
 * <h2>Lo que estaba mal</h2>
 *
 * <p>Las dos tablas tenian <b>una sola fila por concepto</b>, abierta el 1 de enero de 2025 y sin
 * cerrar. Dentro de esa fila habia cifras de dos ejercicios distintos —las bases minimas por grupo
 * eran las de 2024 y el tope maximo el de 2025— y un MEI (0,58 + 0,11) que no es de ningun ano. La
 * demo calcula {@code 202609}: ninguna de las cifras era la del ejercicio que se calculaba, y la
 * vigencia afirmaba lo contrario de lo que era.
 *
 * <p>Que esto no se notara es lo que lo hace peligroso: <b>una fila abierta nunca falla</b>. Sirve
 * para cualquier fecha, incluso para las que nadie ha comprobado.
 *
 * <h2>Por que estas comprobaciones</h2>
 *
 * <p>Un test que afirmara «el MEI de la empresa es 0,75» seria verde tanto con tres ejercicios
 * sembrados como con una unica fila abierta que llevara ese numero. No distingue. Lo que distingue
 * es <b>calcular dos periodos de ejercicios distintos con el mismo empleado</b> y ver que las dos
 * cifras salen distintas, y mirar despues que la cobertura de las vigencias no deja ni huecos ni
 * solapes.
 *
 * <p>Y una correccion que no es de ano: {@code DESEMPLEO_EMP} valia 7,05, que es el tipo
 * <b>total</b> —5,50 de la empresa mas 1,55 de la persona trabajadora—. El {@code 721} le cobraba
 * a la empresa la suma de los dos.
 */
@TestWebSobreEsquemaReal
class EachExerciseHasItsOwnRatesAndLimitsTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final String PERIODO_2025 = "202504";
    private static final String PERIODO_2026 = "202609";

    private static final LocalDate JANUARY_1     = LocalDate.of(2025, 1, 1);
    private static final LocalDate PRIMER_DIA    = LocalDate.of(2024, 1, 1);

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
     * El mismo empleado, dos periodos de dos ejercicios: cada recibo coge el tipo y el tope de su
     * fecha.
     *
     * <p>Rojo con una fila abierta desde 2025, que daria las dos veces el mismo numero.
     */
    @Test
    void theSameEmployeeGetsTheRatesOfEachExercise() {
        String emp = contratadoDesdeEneroDe2025();

        Long abrilDe2025       = reciboDe(emp, PERIODO_2025);
        Long septiembreDe2026  = reciboDe(emp, PERIODO_2026);

        assertEquals(0, importe(abrilDe2025, "P_SS_MEI_EMP").compareTo(new BigDecimal("0.67")),
                "en 2025 el MEI de la empresa es 0,67 (Orden PJC/178/2025, art. 16)");
        assertEquals(0, importe(septiembreDe2026, "P_SS_MEI_EMP").compareTo(new BigDecimal("0.75")),
                "y en 2026 es 0,75 (Orden PJC/297/2026, art. 16): si sale 0,67 es que manda la"
                        + " ultima fila y no la vigencia");

        assertEquals(0, importe(abrilDe2025, "P_TOPE_MIN").compareTo(new BigDecimal("1381.20")),
                "la base minima del grupo 05 en 2025 (Orden PJC/178/2025, art. 3)");
        assertEquals(0, importe(septiembreDe2026, "P_TOPE_MIN").compareTo(new BigDecimal("1424.40")),
                "y la de 2026 (Orden PJC/297/2026, art. 3)");

        assertEquals(0, importe(abrilDe2025, "P_TOPE_MAX").compareTo(new BigDecimal("4909.50")),
                "el tope maximo de 2025");
        assertEquals(0, importe(septiembreDe2026, "P_TOPE_MAX").compareTo(new BigDecimal("5101.20")),
                "y el de 2026");
    }

    /**
     * El desempleo de la empresa es su parte, no el total.
     *
     * <p>7,05 es la suma de los dos: 5,50 la empresa y 1,55 la persona trabajadora (Orden
     * PJC/297/2026, art. 33.2.a).1.o, y sus equivalentes de 2024 y 2025). Cobrarle a la empresa el
     * 7,05 la hacia pagar tambien la parte de la persona trabajadora, que ya se descuenta aparte.
     */
    @Test
    void theEmployerUnemploymentRateIsItsShareAndNotTheTotal() {
        Long recibo = reciboDe(contratadoDesdeEneroDe2025(), PERIODO_2026);

        BigDecimal empresa    = importe(recibo, "P_SS_DESEMPLEO_EMP");
        BigDecimal trabajador = importe(recibo, "P_SS_DESEMPLEO");

        assertEquals(0, empresa.compareTo(new BigDecimal("5.50")),
                "la empresa paga 5,50, no los 7,05 del total");
        assertEquals(0, trabajador.compareTo(new BigDecimal("1.55")),
                "y la persona trabajadora 1,55");
        assertEquals(0, empresa.add(trabajador).compareTo(new BigDecimal("7.05")),
                "que es de donde salia el 7,05 mal puesto");
    }

    /**
     * Las vigencias cubren de 2024 a hoy sin huecos y sin solapes, en las dos tablas.
     *
     * <p>Es la mitad que los importes no ven: se pueden tener las tres cifras buenas y una fila de
     * 2024 abierta por debajo. Un hueco deja una fecha sin tipo —y la corrida se para, que al menos
     * se ve—; un solape deja dos filas vigentes a la vez, y ahi el motor elige una <b>en
     * silencio</b>.
     */
    @Test
    void theVigenciasTileTheYearsWithoutGapsOrOverlaps() {
        assertSinHuecosNiSolapes(
                "select contingency_code as clave, valid_from, valid_to"
                        + " from payroll_engine.ss_cotizacion_tipos where rule_system_code = ?"
                        + " order by clave, valid_from",
                "ss_cotizacion_tipos");

        assertSinHuecosNiSolapes(
                "select grupo_code || '/' || period_type || '/' || contingency_code as clave,"
                        + "       valid_from, valid_to"
                        + " from payroll_engine.ss_cotizacion_topes where rule_system_code = ?"
                        + " order by clave, valid_from",
                "ss_cotizacion_topes");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Comprueba, para cada clave, que la primera vigencia arranca el 1 de enero de 2024, que cada
     * una empieza justo el dia siguiente al final de la anterior, y que la ultima queda abierta.
     */
    private void assertSinHuecosNiSolapes(String consulta, String tabla) {
        List<Map<String, Object>> filas = jdbc.queryForList(consulta, RULE_SYSTEM);
        assertTrue(!filas.isEmpty(), tabla + " tiene que estar sembrada para ESP");

        String    claveAnterior = null;
        LocalDate finAnterior   = null;

        for (Map<String, Object> fila : filas) {
            String    clave = (String) fila.get("clave");
            LocalDate desde = ((java.sql.Date) fila.get("valid_from")).toLocalDate();
            java.sql.Date cierre = (java.sql.Date) fila.get("valid_to");
            LocalDate hasta = cierre == null ? null : cierre.toLocalDate();

            if (!clave.equals(claveAnterior)) {
                if (claveAnterior != null) {
                    String anterior = claveAnterior;
                    assertNull(finAnterior,
                            () -> tabla + ", clave " + anterior + ": la ultima vigencia tiene que"
                                    + " quedar abierta, o hay fechas futuras sin fila");
                }
                assertEquals(PRIMER_DIA, desde,
                        () -> tabla + ", clave " + clave + ": el catalogo arranca el 1 de enero de"
                                + " 2024 y esta clave empieza el " + desde);
            } else {
                LocalDate fin = finAnterior;
                assertEquals(fin == null ? null : fin.plusDays(1), desde,
                        () -> tabla + ", clave " + clave + ": la vigencia que empieza el " + desde
                                + " no encaja con la anterior, que acaba el " + fin
                                + " (hueco o solape)");
            }
            claveAnterior = clave;
            finAnterior   = hasta;
        }

        assertNull(finAnterior, tabla + ": la ultima vigencia tiene que quedar abierta");
    }

    private String contratadoDesdeEneroDe2025() {
        String emp = "TP" + (System.nanoTime() % 1_000_000_000L);
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, JANUARY_1, null);
        fixtures.insertLaborClassification(empId, JANUARY_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), JANUARY_1, null);
        fixtures.insertExtraPaymentRegime(empId, false, JANUARY_1, null);
        return emp;
    }

    private Long reciboDe(String employeeNumber, String period) {
        CalculationRun run = launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, period, PAYROLL_TYPE, "ENGINE", "1.0",
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
                Long.class, RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, period, PAYROLL_TYPE);
    }

    private BigDecimal importe(Long payrollId, String conceptCode) {
        return jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, conceptCode);
    }
}
