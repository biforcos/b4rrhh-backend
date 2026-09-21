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

/**
 * Cambiar de regimen de pagas extras a mitad de mes parte el periodo ({@code backend#118}).
 *
 * <h2>Por que esto tiene que ser cierto antes de que nadie lo lea</h2>
 *
 * <p>Ningun concepto lee todavia esta vertical: quien la lee es el {@code backend#119}. Y aun asi
 * el periodo tiene que partirse ya, por la misma razon por la que lo parte el contrato desde el
 * {@code backend#47}: <b>la union de los cortes incluye esta vertical o no es la union</b>. El dia
 * que la prorrata tenga dos puertas, los dos tramos tienen que existir para poder poner una en
 * cada uno; si el periodo no se partiera, no habria donde.
 *
 * <p>Lo que se afirma es sobre {@code payroll.payroll_segment}, que es donde queda escrito como se
 * partio el mes, y sobre los pasos de un concepto {@code SEGMENT}, que es donde se ve que el motor
 * evaluo dos veces.
 *
 * <h2>El caso, y el que lo hace valer algo</h2>
 *
 * <p>Dos empleados iguales en todo salvo en esto: uno cambia de regimen el 16 y el otro no cambia
 * de nada. El primero tiene dos tramos y el segundo uno. Sin el segundo, un test que solo mirara
 * al primero pasaria igual con un periodo que se partiera siempre.
 */
@TestWebSobreEsquemaReal
class AChangeOfExtraPaymentRegimeSplitsThePeriodTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final LocalDate JANUARY_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate APRIL_1   = LocalDate.of(2025, 4, 1);
    private static final LocalDate APRIL_15  = LocalDate.of(2025, 4, 15);
    private static final LocalDate APRIL_16  = LocalDate.of(2025, 4, 16);
    private static final LocalDate APRIL_30  = LocalDate.of(2025, 4, 30);

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

    @Test
    void aRegimeThatChangesMidMonthLeavesTwoSegments() {
        String emp = empleadoQueCambiaDeRegimenElDia16();
        assertEquals("COMPLETED", lanzar(emp).status());
        entityManager.flush();
        Long pid = payrollId(emp);

        // payroll_segment ancla cada tramo por su fecha de inicio: el fin es implicito, el dia
        // anterior al siguiente inicio o el fin del periodo (V76).
        List<LocalDate> inicios = jdbc.queryForList(
                "select segment_start from payroll.payroll_segment"
                        + " where payroll_id = ? order by segment_start", java.sql.Date.class, pid)
                .stream().map(java.sql.Date::toLocalDate).toList();

        assertEquals(List.of(APRIL_1, APRIL_16), inicios,
                "el cambio de regimen del dia 16 parte el mes en dos: la union de los cortes de las"
                        + " verticales incluye esta");

        // Y el motor evaluo de verdad dos veces lo que es del tramo, con las dos fechas de fin.
        // Con solo la tabla de tramos, un periodo partido ahi y calculado de una sola pasada
        // pasaria la comprobacion de arriba.
        List<Map<String, Object>> salarioBase = jdbc.queryForList(
                "select segment_start_date, segment_end_date from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = '101' order by execution_order",
                pid);
        assertEquals(2, salarioBase.size(),
                "SALARIO_BASE es de ambito SEGMENT: una evaluacion por tramo");
        assertEquals(APRIL_1, fecha(salarioBase.get(0), "segment_start_date"));
        assertEquals(APRIL_15, fecha(salarioBase.get(0), "segment_end_date"));
        assertEquals(APRIL_16, fecha(salarioBase.get(1), "segment_start_date"));
        assertEquals(APRIL_30, fecha(salarioBase.get(1), "segment_end_date"));
    }

    /**
     * El mismo empleado sin el cambio: un tramo. Es la mitad que hace que la de arriba diga algo.
     */
    @Test
    void anUnchangedRegimeLeavesOne() {
        String emp = empleadoConUnSoloRegimen();
        assertEquals("COMPLETED", lanzar(emp).status());
        entityManager.flush();
        Long pid = payrollId(emp);

        assertEquals(1, (int) jdbc.queryForObject(
                "select count(*) from payroll.payroll_segment where payroll_id = ?",
                Integer.class, pid),
                "sin cambios no hay cortes, y un mes entero es un solo tramo");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String empleadoQueCambiaDeRegimenElDia16() {
        String emp = numeroUnico();
        long empId = altaBasica(emp);
        fixtures.insertExtraPaymentRegime(empId, false, JANUARY_1, APRIL_15);
        fixtures.insertExtraPaymentRegime(empId, true, APRIL_16, null);
        return emp;
    }

    private String empleadoConUnSoloRegimen() {
        String emp = numeroUnico();
        long empId = altaBasica(emp);
        fixtures.insertExtraPaymentRegime(empId, false, JANUARY_1, null);
        return emp;
    }

    private long altaBasica(String emp) {
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, JANUARY_1, null);
        fixtures.insertLaborClassification(empId, JANUARY_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), JANUARY_1, null);
        return empId;
    }

    private static LocalDate fecha(Map<String, Object> fila, String columna) {
        return ((java.sql.Date) fila.get(columna)).toLocalDate();
    }

    private String numeroUnico() {
        return "PR" + (System.nanoTime() % 1_000_000_000L);
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

    private Long payrollId(String employeeNumber) {
        return jdbc.queryForObject(
                "select id from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = 1"
                        + "   and status = 'CALCULATED'",
                Long.class,
                RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE);
    }
}
