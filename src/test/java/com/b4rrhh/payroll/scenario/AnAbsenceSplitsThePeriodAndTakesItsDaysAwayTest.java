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
 * La ausencia es la cuarta causa de corte del periodo, y en su tramo no se devengan dias
 * ({@code backend#127}, paso 5 de {@code b4rrhh/workspace#9}).
 *
 * <h2>Lo que habia antes de esto</h2>
 *
 * <p>{@code employee.employee_absence} existia desde el principio y <b>no llegaba al motor</b>: un
 * empleado de vacaciones del 15 al 25 cobraba 30 dias, y uno con un dia de baja por enfermedad
 * comun, tambien. Ochenta y cinco bajas tocaban septiembre de la semilla y ninguna movia un
 * centimo.
 *
 * <h2>Las dos mitades de la regla, y por que las dos hacen falta</h2>
 *
 * <p><b>No todas las ausencias parten.</b> Parten las que quitan dias —{@code IT_COMMON} y
 * {@code UNPAID_LEAVE}— porque en su tramo el salario vale otra cosa. Las vacaciones y los permisos
 * retribuidos se cobran enteros, asi que un tramo para ellas seria un paso de calculo mas que no
 * cambia ningun numero.
 *
 * <p>Sin el caso de las vacaciones, un test que solo mirara a la baja pasaria igual con una
 * implementacion que partiera por cualquier ausencia. Es la misma pareja que el
 * {@code AChangeOfExtraPaymentRegimeSplitsThePeriodTest}: el caso que no se mueve es el que hace
 * que el que se mueve diga algo.
 *
 * <h2>Lo que este issue deja mal a proposito</h2>
 *
 * <p>La base de cotizacion del mes <b>baja</b> con los dias de baja, y eso esta mal: durante la baja
 * se sigue cotizando. Lo arregla el {@code backend#129} con la base durante la baja, y los tres
 * tercios van en la misma resiembra. Aqui no se afirma nada sobre {@code B01} para no escribir como
 * bueno un estado que no lo es.
 */
@TestWebSobreEsquemaReal
class AnAbsenceSplitsThePeriodAndTakesItsDaysAwayTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final LocalDate ENERO_1   = LocalDate.of(2025, 1, 1);
    private static final LocalDate MARZO_10  = LocalDate.of(2025, 3, 10);
    private static final LocalDate ABRIL_1   = LocalDate.of(2025, 4, 1);
    private static final LocalDate ABRIL_10  = LocalDate.of(2025, 4, 10);
    private static final LocalDate ABRIL_12  = LocalDate.of(2025, 4, 12);
    private static final LocalDate ABRIL_13  = LocalDate.of(2025, 4, 13);
    private static final LocalDate ABRIL_16  = LocalDate.of(2025, 4, 16);
    private static final LocalDate ABRIL_17  = LocalDate.of(2025, 4, 17);
    private static final LocalDate ABRIL_30  = LocalDate.of(2025, 4, 30);
    private static final LocalDate MAYO_20   = LocalDate.of(2025, 5, 20);

    /** Lo que cuesta un dia en el convenio de los escenarios. */
    private static final BigDecimal PRECIO_DIA = new BigDecimal("47.50");

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
     * Un dia de baja el 16: tres tramos —antes, el dia, y despues— y 29 dias devengados.
     *
     * <p>Tres y no dos porque la baja se abre y se cierra dentro del mes: aporta el corte del 16 y
     * el del 17, que es la mitad que se olvida (ADR-068 §1).
     */
    @Test
    void unDiaDeBajaParteElMesEnTresYQuitaUnDia() {
        String emp = numeroUnico();
        long empId = altaBasica(emp);
        fixtures.insertAbsence(empId, "IT_COMMON", ABRIL_16, ABRIL_16);

        Long pid = calcular(emp);

        assertEquals(List.of(ABRIL_1, ABRIL_16, ABRIL_17), iniciosDeTramo(pid),
                "la baja aporta dos cortes: el dia que empieza y el dia siguiente al que acaba");
        assertEquals(29L, diasDevengados(pid), "29 dias: el de la baja no se devenga");
        assertImporte(PRECIO_DIA.multiply(new BigDecimal("29")), salarioBase(pid),
                "el salario base son los dias que quedan al precio del dia");
        assertImporte(BigDecimal.ZERO, salarioBaseDelTramo(pid, ABRIL_16),
                "el tramo de baja se evalua y da cero: el paso existe, y ese es el punto");
    }

    /**
     * Una baja que viene de marzo y sigue en mayo: un tramo, ningun corte dentro del mes, cero dias.
     *
     * <p>Es el caso que obliga a recortar contra el periodo y no contra las fechas de la ausencia: si
     * los cortes se metieran tal cual, el 10 de marzo y el 21 de mayo partirian un abril donde no
     * cambia nada.
     */
    @Test
    void unaBajaQueOcupaElMesEnteroDejaUnTramoSinDiasYSalarioCero() {
        String emp = numeroUnico();
        long empId = altaBasica(emp);
        fixtures.insertAbsence(empId, "IT_COMMON", MARZO_10, MAYO_20);

        Long pid = calcular(emp);

        assertEquals(List.of(ABRIL_1), iniciosDeTramo(pid),
                "una baja que pasa de largo no parte nada: los cortes caen fuera del periodo");
        assertEquals(0L, diasDevengados(pid), "ningun dia se devenga");
        assertImporte(BigDecimal.ZERO, salarioBase(pid), "y el salario base es cero");
    }

    /** Una excedencia de tres dias en medio del mes: tres tramos y 27 dias. */
    @Test
    void unaExcedenciaDeTresDiasDejaTresTramosY27Dias() {
        String emp = numeroUnico();
        long empId = altaBasica(emp);
        fixtures.insertAbsence(empId, "UNPAID_LEAVE", ABRIL_10, ABRIL_12);

        Long pid = calcular(emp);

        assertEquals(List.of(ABRIL_1, ABRIL_10, ABRIL_13), iniciosDeTramo(pid));
        assertEquals(27L, diasDevengados(pid),
                "el permiso no retribuido es el caso mas simple del mismo mecanismo: dias que no se"
                        + " pagan y punto");
    }

    /**
     * Unas vacaciones no parten nada y se cobran enteras.
     *
     * <p>Es la mitad que hace que las de arriba digan algo: sin ella, partir por cualquier ausencia
     * pasaria los tres tests anteriores.
     */
    @Test
    void unasVacacionesNoPartenNadaYSeCobranEnteras() {
        String emp = numeroUnico();
        long empId = altaBasica(emp);
        fixtures.insertAbsence(empId, "VACATION", ABRIL_10, ABRIL_12);

        Long pid = calcular(emp);

        assertEquals(List.of(ABRIL_1), iniciosDeTramo(pid),
                "no hay motivo para un tramo que no cambia nada");
        assertEquals(30L, diasDevengados(pid), "las vacaciones se cobran");
        assertImporte(PRECIO_DIA.multiply(new BigDecimal("30")), salarioBase(pid),
                "y se cobran al precio del dia, los treinta");
    }

    /**
     * Y el accidente de trabajo sigue cobrando entero, que es la v1 y no un olvido (ADR-073).
     */
    @Test
    void elAccidenteDeTrabajoSigueCobrandoEnteroEnEstaVersion() {
        String emp = numeroUnico();
        long empId = altaBasica(emp);
        fixtures.insertAbsence(empId, "IT_WORK_ACCIDENT", ABRIL_10, ABRIL_12);

        Long pid = calcular(emp);

        assertEquals(List.of(ABRIL_1), iniciosDeTramo(pid));
        assertEquals(30L, diasDevengados(pid),
                "IT_WORK_ACCIDENT no esta en el alcance de la v1: otra base reguladora y el 75 %"
                        + " desde el dia siguiente. Cuando entre, este test cambia y se ve.");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long altaBasica(String emp) {
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, ENERO_1, null);
        fixtures.insertLaborClassification(empId, ENERO_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), ENERO_1, null);
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
                "la corrida tiene que terminar bien: una ausencia no es un motivo para no calcular");
        entityManager.flush();
        return jdbc.queryForObject(
                "select id from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = 1",
                Long.class, RULE_SYSTEM, EMPLOYEE_TYPE, emp, PERIOD, PAYROLL_TYPE);
    }

    private List<LocalDate> iniciosDeTramo(Long payrollId) {
        return jdbc.queryForList(
                        "select segment_start from payroll.payroll_segment"
                                + " where payroll_id = ? order by segment_start",
                        java.sql.Date.class, payrollId)
                .stream().map(java.sql.Date::toLocalDate).toList();
    }

    /** Los dias de devengo del mes: la suma de los pasos de D01, que es de ambito SEGMENT. */
    private long diasDevengados(Long payrollId) {
        BigDecimal total = jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = 'D01'",
                BigDecimal.class, payrollId);
        assertTrue(total != null, "D01 tiene que haberse calculado");
        return total.longValueExact();
    }

    /**
     * Compara importes por valor y no por escala: {@code 0} y {@code 0.00} son el mismo dinero, y
     * {@code assertEquals} sobre {@code BigDecimal} dice que no.
     */
    private static void assertImporte(BigDecimal esperado, BigDecimal real, String porque) {
        assertEquals(0, esperado.compareTo(real), porque + " (esperado " + esperado + ", fue " + real + ")");
    }

    private BigDecimal salarioBase(Long payrollId) {
        return jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = '101'",
                BigDecimal.class, payrollId);
    }

    private BigDecimal salarioBaseDelTramo(Long payrollId, LocalDate inicioDeTramo) {
        return jdbc.queryForObject(
                "select amount from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = '101' and segment_start_date = ?",
                BigDecimal.class, payrollId, inicioDeTramo);
    }

    private String numeroUnico() {
        return "AB" + (System.nanoTime() % 1_000_000_000L);
    }
}
