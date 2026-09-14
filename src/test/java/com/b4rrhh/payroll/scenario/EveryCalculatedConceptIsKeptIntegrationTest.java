package com.b4rrhh.payroll.scenario;

import com.b4rrhh.payroll.application.usecase.BulkInvalidatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.BulkInvalidatePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.application.usecase.RecalculatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.RecalculatePayrollUseCase;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El recibo se queda con los 14 conceptos que tienen sitio en el folio. Los otros 21 —5 BASE y 16
 * TECHNICAL— se calculaban, alimentaban a los demas y se tiraban, y son justo los que explican de
 * donde sale el numero. Desde el backend#93 se guardan todos en payroll.payroll_calculation_step.
 *
 * <p>Sobre ESP y no sobre TST, a proposito: los numeros de este issue —35 conceptos, 4 de ambito
 * SEGMENT y 31 de ambito PERIOD— son los de la reglamentacion que siembran las migraciones, y un
 * fixture con quince conceptos de mentira no probaria el recuento que hay que probar.
 *
 * <p>Y el mes partido no es un adorno del test: con un solo tramo, cualquier identidad parece
 * correcta. Es el caso en el que SALARIO_BASE sale dos veces, con dos precios, y en el que una
 * clave que no distinguiera los dos tramos se comeria una fila en silencio.
 */
@TestWebSobreEsquemaReal
class EveryCalculatedConceptIsKeptIntegrationTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final LocalDate APRIL_15 = LocalDate.of(2025, 4, 15);
    private static final LocalDate APRIL_16 = LocalDate.of(2025, 4, 16);
    private static final LocalDate JANUARY_1 = LocalDate.of(2025, 1, 1);

    /** Los que hay en el catalogo ESP: 4 de ambito SEGMENT y 31 de ambito PERIOD. */
    private static final int CONCEPTS_IN_THE_ENGINE = 35;
    private static final int SEGMENT_SCOPED_CONCEPTS = 4;

    /**
     * Y los 35 entran en algun plan, que es lo que cambio en el backend#96.
     *
     * <p>Hasta la V130 eran 36 en el catalogo y 35 alcanzables: {@code P_SS} (TIPO_SS) se quedo
     * huerfano en la V91, cuando el porcentaje del 700 paso de leerlo a el a leer
     * {@code P_SS_CC}, y desde entonces estaba en el catalogo sin que ningun plan lo pidiera. La
     * V130 lo retira, asi que las dos cuentas vuelven a ser la misma.
     *
     * <p>El recuento de pasos no se movio: 35 en un mes entero y 39 en uno del mes partido, igual
     * que antes. Retirar un concepto que nadie ejecutaba no puede anadir un paso.
     */
    private static final int CONCEPTS_IN_A_PLAN = 35;

    @Autowired
    private LaunchPayrollCalculationUseCase launch;

    @Autowired
    private RecalculatePayrollUseCase recalculate;

    @Autowired
    private BulkInvalidatePayrollUseCase invalidate;

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
     * Vacia la sesion antes de contar filas por JDBC.
     *
     * <p>Hace falta aqui y no lo hacia ningun test anterior porque
     * {@code payroll_calculation_step} es la unica entidad de {@code payroll} con clave asignada:
     * su {@code insert} queda diferido hasta el vaciado, mientras que las lineas del recibo llevan
     * {@code identity} y se insertan al persistir para sacar el id. Y estos tests van en una
     * transaccion que se deshace al terminar, asi que no hay commit que vacie por ellos.
     *
     * <p>No es un arreglo de produccion disfrazado: sin esto, la aplicacion escribe los pasos
     * igual —comprobado contra la aplicacion arrancada, por el lanzamiento y por el recalculo
     * puntual—. Lo que falta aqui es el commit, no el {@code flush} (ADR-062).
     */
    private void vaciarLaSesion() {
        entityManager.flush();
    }

    @Test
    void theEngineCensusIsTheOneThisTestCountsOn() {
        // Si la siembra cambia, los numeros de abajo dejan de significar lo que dicen. Que se
        // entere aqui y no en un recuento que ya no prueba nada.
        assertEquals(CONCEPTS_IN_THE_ENGINE, countConcepts(null), "conceptos del motor en ESP");
        assertEquals(SEGMENT_SCOPED_CONCEPTS, countConcepts("SEGMENT"), "conceptos de ambito SEGMENT");
        assertEquals(14, jdbc.queryForObject(
                "select count(*) from payroll_engine.payroll_concept c"
                        + " join payroll_engine.payroll_object o on o.id = c.object_id"
                        + " where o.rule_system_code = ? and c.payslip_order_code is not null",
                Integer.class, RULE_SYSTEM), "conceptos con sitio en el folio");

        // Y ya no sobra ninguno: desde la V130 (backend#96) todo lo que esta en el catalogo lo
        // alcanza algun plan. La lista se queda viva y vacia, que es lo que hace que el dia que
        // aparezca otro huerfano se entere alguien; borrar el test seria quitar el aviso.
        assertEquals(List.of(), conceptsInNoPlan(),
                "conceptos que ni estan asignados, ni alimentan, ni son operando de nadie");
        assertEquals(CONCEPTS_IN_THE_ENGINE, CONCEPTS_IN_A_PLAN);
    }

    @Test
    void aWholeMonthKeepsEveryStep_andOnly14OfThemReachThePayslip() {
        String emp = hireWholeMonth();
        assertEquals("COMPLETED", launchSingleEmployee(emp).status());
        vaciarLaSesion();
        Long pid = payrollId(emp);

        // Un solo tramo: un paso por concepto ejecutado.
        assertEquals(CONCEPTS_IN_A_PLAN, countSteps(pid), "pasos guardados");

        // Y los 21 que antes se tiraban estan, con nombre y apellido.
        assertEquals(5, countStepsWithNature(pid, "BASE"), "conceptos BASE");
        assertEquals(16, countStepsWithNature(pid, "TECHNICAL"),
                "conceptos TECHNICAL: los 16 del catalogo, desde que la V130 retiro el P_SS");

        // El recibo no cambia: sigue siendo las lineas de siempre, y son las que llevan orden.
        int payslipLines = jdbc.queryForObject(
                "select count(*) from payroll.payroll_concept where payroll_id = ?", Integer.class, pid);
        assertEquals(14, payslipLines, "lineas de recibo");
        assertEquals(payslipLines, jdbc.queryForObject(
                "select count(*) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and payslip_order_code is not null",
                Integer.class, pid), "los pasos con orden de recibo son las lineas del recibo");

        // El orden de ejecucion es una serie completa de 1 a 35, sin huecos ni repetidos: el
        // recuento, el minimo y el maximo solo cuadran a la vez si estan los 35 y una sola vez.
        assertEquals(1, (int) jdbc.queryForObject(
                "select min(execution_order) from payroll.payroll_calculation_step where payroll_id = ?",
                Integer.class, pid), "el primer paso es el 1");
        assertEquals(CONCEPTS_IN_A_PLAN, (int) jdbc.queryForObject(
                "select max(execution_order) from payroll.payroll_calculation_step where payroll_id = ?",
                Integer.class, pid), "el ultimo paso es el 35");
    }

    @Test
    void aSplitMonthKeepsFourStepsMore_withSalarioBaseTwiceAndNeitherOneLost() {
        String emp = hireWithSplitWorkingTime(new BigDecimal("100.00"), new BigDecimal("50.00"));
        assertEquals("COMPLETED", launchSingleEmployee(emp).status());
        vaciarLaSesion();
        Long pid = payrollId(emp);

        // Los 4 conceptos SEGMENT se evaluan una vez por tramo: 31 + 4 x 2 = 39.
        assertEquals(CONCEPTS_IN_A_PLAN + SEGMENT_SCOPED_CONCEPTS, countSteps(pid), "pasos guardados");

        // Y el 101 sale dos veces, con dos precios distintos. Es el caso que se comia cualquier
        // identidad que no distinguiera los tramos.
        List<Map<String, Object>> salarioBase = jdbc.queryForList(
                "select execution_order, amount, quantity, rate, segment_start_date, segment_end_date"
                        + " from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = '101' order by execution_order",
                pid);
        assertEquals(2, salarioBase.size(), "SALARIO_BASE, una vez por tramo");
        assertNotEquals(salarioBase.get(0).get("rate"), salarioBase.get(1).get("rate"),
                "dos precios distintos, no la misma fila dos veces");
        assertEquals(APRIL_15, ((java.sql.Date) salarioBase.get(0).get("segment_end_date")).toLocalDate());
        assertEquals(APRIL_16, ((java.sql.Date) salarioBase.get(1).get("segment_start_date")).toLocalDate());

        // El ambito va explicito, y las fechas dicen lo mismo que el: un paso PERIOD cubre el
        // periodo entero y no tiene segmento; uno de segmento siempre lo tiene.
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from payroll.payroll_calculation_step where payroll_id = ?"
                        + " and ((execution_scope = 'PERIOD') <> (segment_start_date is null))",
                Integer.class, pid), "las fechas son nulas si y solo si el ambito es PERIOD");
    }

    @Test
    void recalculating_replacesBothTablesAtOnce_andLeavesNoStepsBehind() {
        String emp = hireWholeMonth();
        assertEquals("COMPLETED", launchSingleEmployee(emp).status());
        vaciarLaSesion();

        Long firstId = payrollId(emp);
        int stepsBefore = countSteps(firstId);
        int linesBefore = countLines(firstId);
        assertTrue(stepsBefore > 0 && linesBefore > 0);

        // Un recibo CALCULATED no se recalcula: primero se invalida, que es el camino de verdad.
        assertEquals(1, invalidate.invalidateBulk(new BulkInvalidatePayrollCommand(
                RULE_SYSTEM, PERIOD, PAYROLL_TYPE, "TEST",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, emp),
                        null))).totalInvalidated());
        assertEquals(stepsBefore, countSteps(firstId),
                "invalidar no toca los pasos: el recibo sigue siendo el mismo");

        recalculate.recalculate(new RecalculatePayrollCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, PERIOD, PAYROLL_TYPE, 1));
        vaciarLaSesion();

        Long secondId = payrollId(emp);
        assertNotEquals(firstId, secondId, "un recalculo hace un recibo nuevo, no edita el viejo");

        // Gratis no es lo mismo que comprobado: payroll_concept se borra por orphanRemoval desde
        // el agregado y los pasos por la clave ajena. Son dos mecanismos para el mismo ciclo de
        // vida, y lo que importa es que los dos hayan corrido.
        assertEquals(0, countSteps(firstId), "no quedan pasos del recibo anterior");
        assertEquals(0, countLines(firstId), "no quedan lineas del recibo anterior");
        assertEquals(stepsBefore, countSteps(secondId), "el recibo nuevo tiene sus pasos");
        assertEquals(linesBefore, countLines(secondId), "y sus lineas");

        // Y en toda la tabla no hay una sola fila huerfana de ningun recibo.
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from payroll.payroll_calculation_step s"
                        + " where not exists (select 1 from payroll.payroll p where p.id = s.payroll_id)",
                Integer.class), "pasos sin recibo");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String hireWholeMonth() {
        String emp = uniqueEmployeeNumber();
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, JANUARY_1, null);
        fixtures.insertLaborClassification(empId, JANUARY_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), JANUARY_1, null);
        return emp;
    }

    private String hireWithSplitWorkingTime(BigDecimal untilApril15, BigDecimal fromApril16) {
        String emp = uniqueEmployeeNumber();
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, JANUARY_1, null);
        fixtures.insertLaborClassification(empId, JANUARY_1);
        fixtures.insertWorkingTime(empId, untilApril15, JANUARY_1, APRIL_15);
        fixtures.insertWorkingTime(empId, fromApril16, APRIL_16, null);
        return emp;
    }

    private String uniqueEmployeeNumber() {
        return "CS" + (System.nanoTime() % 1_000_000_000L);
    }

    private CalculationRun launchSingleEmployee(String employeeNumber) {
        return launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employeeNumber),
                        null),
                null));
    }

    private Long payrollId(String employeeNumber) {
        Long id = jdbc.queryForObject(
                "select id from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = 1"
                        + "   and status = ?",
                Long.class,
                RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE, "CALCULATED");
        assertNotNull(id);
        return id;
    }

    private int countConcepts(String executionScope) {
        return jdbc.queryForObject(
                "select count(*) from payroll_engine.payroll_concept c"
                        + " join payroll_engine.payroll_object o on o.id = c.object_id"
                        + " where o.rule_system_code = ? and (?::varchar is null or c.execution_scope = ?)",
                Integer.class, RULE_SYSTEM, executionScope, executionScope);
    }

    private int countSteps(Long payrollId) {
        return jdbc.queryForObject(
                "select count(*) from payroll.payroll_calculation_step where payroll_id = ?",
                Integer.class, payrollId);
    }

    private int countLines(Long payrollId) {
        return jdbc.queryForObject(
                "select count(*) from payroll.payroll_concept where payroll_id = ?",
                Integer.class, payrollId);
    }

/** Los conceptos que no puede alcanzar ningun plan: ni asignados, ni alimentando, ni operando. */
    private List<String> conceptsInNoPlan() {
        return jdbc.queryForList(
                "select o.object_code from payroll_engine.payroll_concept c"
                        + " join payroll_engine.payroll_object o on o.id = c.object_id"
                        + " where o.rule_system_code = ?"
                        + "   and not exists (select 1 from payroll_engine.concept_assignment a"
                        + "                    where a.rule_system_code = o.rule_system_code"
                        + "                      and a.concept_code = o.object_code)"
                        + "   and not exists (select 1 from payroll_engine.payroll_concept_feed_relation r"
                        + "                    where r.source_object_id = o.id)"
                        + "   and not exists (select 1 from payroll_engine.payroll_concept_operand op"
                        + "                    where op.source_object_id = o.id)"
                        + " order by o.object_code",
                String.class, RULE_SYSTEM);
    }

    private int countStepsWithNature(Long payrollId, String nature) {
        return jdbc.queryForObject(
                "select count(*) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and functional_nature = ?",
                Integer.class, payrollId, nature);
    }
}
