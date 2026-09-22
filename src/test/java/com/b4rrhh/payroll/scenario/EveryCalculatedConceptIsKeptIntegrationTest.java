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
 * El recibo se queda con los conceptos que tienen sitio en el folio. Los otros —los BASE, los
 * TECHNICAL y los INFORMATIONAL sin orden— se calculaban, alimentaban a los demas y se tiraban, y
 * son justo los que explican de donde sale el numero. Desde el backend#93 se guardan todos en
 * payroll.payroll_calculation_step.
 *
 * <p>Sobre ESP y no sobre TST, a proposito: los numeros de este issue —38 conceptos, 4 de ambito
 * SEGMENT y 34 de ambito PERIOD— eran los de la reglamentacion que siembran las migraciones, y un
 * fixture con quince conceptos de mentira no probaria el recuento que hay que probar.
 *
 * <p>Eran 35 hasta el backend#104, que declaro la cadena de las horas extra: {@code H01} (la
 * cantidad que entra desde fuera), {@code P03} (el precio de la hora) y {@code 102} (lo que se
 * cobra), los tres de ambito PERIOD. Y con ellos se rompio una identidad que este test daba por
 * hecha sin decirlo: <b>los pasos con orden de recibo ya no son las lineas del recibo</b>, porque
 * la regla del cero no imprime un {@code 102} que vale cero.
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

    /**
     * Los que hay en el catalogo ESP: 5 de ambito SEGMENT y 34 de ambito PERIOD.
     *
     * <p>Eran 4 y 34 hasta el {@code backend#47}: la {@code V135} paso {@code P02} a
     * {@code SEGMENT} porque el precio del dia sale de una fila que se busca por categoria, y un
     * empleado que cambia de categoria a mitad de mes tiene dos precios en el mismo mes.
     *
     * <p>Y eran 38 hasta el {@code backend#114}: la {@code V141} declara el {@code 725}, el total
     * de la aportacion empresarial, que hasta entonces sumaba la plantilla del PDF. Es
     * {@code PERIOD}, asi que anade un paso por recibo y no uno por tramo.
     *
     * <p>Y eran 39 y 5 hasta el {@code backend#117}: la {@code V144} declara las cuatro pagas
     * extraordinarias del convenio ({@code PE_1} a {@code PE_4}), las cuatro {@code SEGMENT}
     * porque se componen del {@code 101}, que lo es. Son cuatro conceptos que se calculan y no
     * salen en ningun recibo: no llevan orden de folio y todavia no alimentan a nadie —eso es el
     * {@code backend#119}—, asi que lo unico que crecio fue el rastro de calculo.
     *
     * <p>Y eran 43 y 9 hasta el {@code backend#119}: la {@code V146} declara los siete
     * conceptos de la prorrata de pagas extras. Seis son {@code SEGMENT} —el total de las
     * pagas, la prorrata, los dos coeficientes de regimen y las dos puertas— porque el
     * regimen puede cambiar a mitad de mes; el septimo, los meses del ano, es
     * {@code PERIOD}: doce son doce en un mes partido tambien.
     *
     * <p>Y eran 50 y 15 hasta el {@code backend#121}: las tres bases del modelo oficial son
     * quince conceptos mas, los quince {@code PERIOD}. Y 65 hasta el {@code backend#122}, que
     * anade la cuota de accidentes de trabajo y el tipo del que sale.
     */
    private static final int CONCEPTS_IN_THE_ENGINE = 67;
    private static final int SEGMENT_SCOPED_CONCEPTS = 15;

    /**
     * Y los 38 entran en algun plan, que es lo que cambio en el backend#96.
     *
     * <p>Hasta la V130 eran 36 en el catalogo y 35 alcanzables: {@code P_SS} (TIPO_SS) se quedo
     * huerfano en la V91, cuando el porcentaje del 700 paso de leerlo a el a leer
     * {@code P_SS_CC}, y desde entonces estaba en el catalogo sin que ningun plan lo pidiera. La
     * V130 lo retira, asi que las dos cuentas vuelven a ser la misma.
     *
     * <p>El recuento de pasos no se movio con aquello: 35 en un mes entero y 39 en uno del mes
     * partido, igual que antes. Retirar un concepto que nadie ejecutaba no puede anadir un paso.
     * Declarar tres si: desde el backend#104 son 38 y 42, desde el backend#47 son 38 y 43, y desde
     * el backend#114 son 39 y 44 — cambiar un ambito no anade conceptos, anade evaluaciones.
     */
    private static final int CONCEPTS_IN_A_PLAN = 67;

    /**
     * Los conceptos con orden de recibo: los que PUEDEN ser linea.
     *
     * <p>Eran 14 y fueron 15 desde que el backend#104 declaro el {@code 102}. Fueron 17 desde el
     * backend#111, que imprimio el recuadro de bases de cotizacion: {@code B_CC} y {@code B01}
     * se calculaban desde siempre y ahora ademas salen en el papel. Y son 18 desde el
     * backend#114, que le dio total propio al recuadro de aportacion empresarial.
     */
    private static final int CONCEPTS_WITH_A_PAYSLIP_ORDER = 30;

    /**
     * Y las lineas que un empleado sin horas extra acaba teniendo en el folio, que son 16.
     *
     * <p>Este par de numeros era uno solo hasta el backend#104, y ahi estaba la trampa: coincidian
     * porque todos los conceptos con orden valian algo en todos los recibos, no porque tuvieran
     * que coincidir. La regla del cero los separa —una linea de concepto a cero no se imprime— y
     * este empleado no declara horas, asi que el {@code 102} se calcula, se guarda y no se
     * imprime.
     *
     * <p>Eran 14 hasta el backend#111 y fueron 16: las dos bases del recuadro de cotizacion valen
     * algo en cualquier recibo con presencia, asi que suman linea en los dos sitios. Son 17 desde
     * el backend#114 por lo mismo: la aportacion empresarial vale algo en cualquier recibo con
     * presencia, y su total tambien.
     */
    private static final int PAYSLIP_LINES_WITHOUT_OVERTIME = 24;

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
        assertEquals(CONCEPTS_WITH_A_PAYSLIP_ORDER, jdbc.queryForObject(
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
    void aWholeMonthKeepsEveryStep_andOnly18OfThemGetPrinted() {
        String emp = hireWholeMonth();
        assertEquals("COMPLETED", launchSingleEmployee(emp).status());
        vaciarLaSesion();
        Long pid = payrollId(emp);

        // Un solo tramo: un paso por concepto ejecutado.
        assertEquals(CONCEPTS_IN_A_PLAN, countSteps(pid), "pasos guardados");

        // Y los que antes se tiraban estan, con nombre y apellido.
        assertEquals(22, countStepsWithNature(pid, "BASE"),
                "conceptos BASE: los 13 de antes del backend#121 mas los nueve de las tres bases"
                        + " — B03, B04, B05, B06, B07, B_CP_MAX, B_CP, B08 y B09");
        assertEquals(24, countStepsWithNature(pid, "TECHNICAL"),
                "conceptos TECHNICAL: los 19 de antes, los cuatro del backend#121 —los dos"
                        + " topes de la base profesional y los dos tipos de la cotizacion"
                        + " adicional por horas extraordinarias— y el tipo de accidentes de"
                        + " trabajo del backend#122, que sale de la actividad de la empresa");

        // El recibo tiene 18 lineas: las 17 de antes mas la prorrata que cotiza, que es la puerta
        // que le toca a este empleado. Siguen sin ser TODOS los pasos con orden de recibo: hay 20,
        // y los dos que sobran son el 102 y el 103 valiendo cero.
        int payslipLines = jdbc.queryForObject(
                "select count(*) from payroll.payroll_concept where payroll_id = ?", Integer.class, pid);
        assertEquals(PAYSLIP_LINES_WITHOUT_OVERTIME, payslipLines, "lineas de recibo");
        assertEquals(CONCEPTS_WITH_A_PAYSLIP_ORDER, jdbc.queryForObject(
                "select count(*) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and payslip_order_code is not null",
                Integer.class, pid), "los pasos que llevan orden de recibo");

        // Y la diferencia tiene nombre: el 102 y el 103, que se calcularon, dieron cero y no se
        // imprimieron. El paso existe —el calculo ocurrio— y la linea no. Esa es la regla del cero
        // (backend#104), y es lo que hace que las dos puertas de la prorrata puedan estar las dos
        // asignadas a todo el mundo y salga impresa exactamente una (backend#119).
        assertEquals(List.of("102", "103", "704", "726", "B06", "B08"), jdbc.queryForList(
                "select concept_code from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and payslip_order_code is not null"
                        + "   and payslip_line_number is null order by concept_code",
                String.class, pid),
                "los pasos con orden y sin linea son los que valen cero en este recibo: el 103"
                        + " porque este empleado no tiene las pagas prorrateadas —su prorrata sale"
                        + " por la otra puerta, el B02— y los cinco que cuelgan de las horas extra"
                        + " que no ha hecho: el 102, su base (B08), la linea con la que entra en"
                        + " la base profesional (B06) y las dos cuotas de cotizacion adicional"
                        + " (704 y 726)");

        // El recuadro de bases, por los dos lados (backend#111).
        //
        // Las dos que el modelo oficial imprime salen, en su bloque y con su nombre. Y los otros
        // diez BASE siguen sin salir: el B_CC_MAX es el paso intermedio entre las dos; P01, P02 y
        // P03 son precios que llevan naturaleza BASE porque el motor los usa como operando; las
        // cuatro PE_* son las pagas del convenio; y PE_TOTAL y P_PRORRATA son su suma y su
        // duodecima parte, los dos pasos por los que se llega a la linea que SI sale (backend#119).
        //
        // Las dos mitades van juntas a proposito. Con solo la primera, darles orden de recibo a
        // los doce pasaria igual; es la segunda la que hace que «no tiene orden» siga
        // significando «no va al papel» y no «se me olvido».
        assertEquals(List.of("B03", "B04", "B01", "B_CC", "B05", "B07", "B_CP", "B09"),
                jdbc.queryForList(
                "select c.concept_code from payroll.payroll_concept c"
                        + " where c.payroll_id = ? and c.payslip_section_code = 'BASES'"
                        + " order by c.display_order",
                String.class, pid),
                "el recuadro de bases del modelo oficial, en sus cuatro bloques: comunes (B03,"
                        + " B04, B01, B_CC), profesionales (B05, B07, B_CP), y la base sujeta a"
                        + " retencion (B09). Las dos lineas de horas extra del recuadro —B06 y"
                        + " B08— valen cero en este recibo y no se imprimen (backend#121)");
        assertEquals(List.of("B02", "B_CC_MAX", "B_CP_MAX", "P01", "P02", "P03",
                        "PE_1", "PE_2", "PE_3", "PE_4", "PE_TOTAL", "P_PRORRATA"),
                jdbc.queryForList(
                "select s.concept_code from payroll.payroll_calculation_step s"
                        + " where s.payroll_id = ? and s.functional_nature = 'BASE'"
                        + "   and s.payslip_order_code is null order by s.concept_code",
                String.class, pid), "los BASE que se calculan y no van al papel");

        // El orden de ejecucion es una serie completa desde 1, sin huecos ni repetidos: el
        // recuento, el minimo y el maximo solo cuadran a la vez si estan todos y una sola vez.
        assertEquals(1, (int) jdbc.queryForObject(
                "select min(execution_order) from payroll.payroll_calculation_step where payroll_id = ?",
                Integer.class, pid), "el primer paso es el 1");
        assertEquals(CONCEPTS_IN_A_PLAN, (int) jdbc.queryForObject(
                "select max(execution_order) from payroll.payroll_calculation_step where payroll_id = ?",
                Integer.class, pid), "el ultimo paso es el " + CONCEPTS_IN_A_PLAN);
    }

    @Test
    void aSplitMonthKeepsFifteenStepsMore_withSalarioBaseTwiceAndNeitherOneLost() {
        String emp = hireWithSplitWorkingTime(new BigDecimal("100.00"), new BigDecimal("50.00"));
        assertEquals("COMPLETED", launchSingleEmployee(emp).status());
        vaciarLaSesion();
        Long pid = payrollId(emp);

        // Los 15 conceptos SEGMENT se evaluan una vez por tramo: 35 + 15 x 2 = 65.
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
        Long launchRunId = jdbc.queryForObject(
                "select run_id from payroll.payroll where id = ?", Long.class, firstId);
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
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, PERIOD, PAYROLL_TYPE, 1, null));
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

        // Y el recibo nuevo sabe de donde viene. Hasta el #99 nacia con run_id nulo y la pantalla
        // decia «sin ejecucion registrada» justo despues del gesto que remata la demo. Se lee de la
        // base y no del objeto devuelto: lo que hay que afirmar es que la columna quedo escrita.
        Long runId = jdbc.queryForObject(
                "select run_id from payroll.payroll where id = ?", Long.class, secondId);
        assertNotNull(runId, "un recalculo es una ejecucion, y el recibo tiene que decir cual");
        assertNotEquals(launchRunId, runId, "y no es la del lanzamiento, que no produjo este recibo");

        Map<String, Object> run = jdbc.queryForMap(
                "select status, total_candidates, total_calculated, target_selection_json,"
                        + " finished_at from payroll.calculation_run where id = ?", runId);
        assertEquals("COMPLETED", run.get("status"));
        assertEquals(1, run.get("total_candidates"));
        assertEquals(1, run.get("total_calculated"));
        assertNotNull(run.get("finished_at"));
        assertTrue(run.get("target_selection_json").toString().contains("SINGLE_CALCULATION_UNIT"),
                "la ejecucion dice que unidad recalculo, que SINGLE_EMPLOYEE no diria");
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
