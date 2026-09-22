package com.b4rrhh.payroll.scenario;

import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El endpoint que sirve los pasos, de extremo a extremo y sobre el esquema real
 * ({@code backend#97}).
 *
 * <p>Sobre ESP y no sobre TST, por lo mismo que el
 * {@code EveryCalculatedConceptIsKeptIntegrationTest}: los recuentos que afirma —67 pasos y 82—
 * son los de la reglamentacion que siembran las migraciones, y un fixture con quince conceptos de
 * mentira no probaria el numero que hay que probar.
 *
 * <p>Lo que este test sujeta y no sujeta ningun otro:
 *
 * <ul>
 *   <li><b>El orden que sale por HTTP es el de ejecucion.</b> No el del folio, no el de la
 *       naturaleza, no el del codigo. Es lo unico que este endpoint aporta sobre el recibo, que ya
 *       existe y ya esta ordenado para imprimir.
 *   <li><b>El mismo concepto puede salir dos veces.</b> Se prueba contra el mes partido, que es el
 *       unico caso donde se ve: con un solo tramo, cualquier implementacion que indexe por
 *       concepto parece correcta y acierta en 868 recibos de 873.
 *   <li><b>Los dos vacios son distintos.</b> Recibo sin pasos, {@code 200} y lista vacia;
 *       direccion sin recibo, {@code 404}.
 * </ul>
 */
@TestWebSobreEsquemaReal
class CalculationStepsAreServedInExecutionOrderIntegrationTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final LocalDate APRIL_15 = LocalDate.of(2025, 4, 15);
    private static final LocalDate APRIL_16 = LocalDate.of(2025, 4, 16);
    private static final LocalDate JANUARY_1 = LocalDate.of(2025, 1, 1);

    /** Los pasos de un empleado de mes entero: uno por cada concepto del catalogo ESP. */
    private static final int STEPS_IN_A_WHOLE_MONTH = 67;

    /**
     * Y los de uno del mes partido: los conceptos de ambito SEGMENT se evaluan una vez por tramo.
     *
     * <p>Eran 42 y fueron 43 desde el {@code backend#47}: la {@code V135} paso {@code P02} a
     * {@code SEGMENT}, asi que los conceptos por tramo son cinco. El importe no se mueve —con una
     * sola categoria los dos tramos leen la misma fila— y lo que se anade es el paso que lo dice.
     *
     * <p>Y son 44 desde el {@code backend#114}: el {@code 725} es {@code PERIOD}, asi que anade
     * un paso y solo uno, tenga el recibo los tramos que tenga.
     *
     * <p>Y 43 y 52 desde el {@code backend#117}: la {@code V144} declara las cuatro pagas
     * extraordinarias del convenio, las cuatro {@code SEGMENT}, asi que anaden cuatro pasos por
     * tramo. Son pasos y no lineas: no llevan orden de folio, y el recibo seguia teniendo 17.
     *
     * <p>Y 50 y 65 desde el {@code backend#119}: siete conceptos mas, seis de ellos {@code SEGMENT}.
     * De esos siete, dos SI son linea —las dos puertas de la prorrata— y el recibo pasa a tener 18,
     * porque de las dos solo se imprime la que no vale cero.
     *
     * <p>Y 65 y 80 desde el {@code backend#121}: las tres bases del modelo oficial son quince
     * conceptos mas y los quince son {@code PERIOD}, asi que anaden quince pasos al mes entero y
     * los mismos quince al partido. La diferencia entre los dos numeros no se mueve: sigue siendo
     * los quince {@code SEGMENT} evaluados una vez de mas.
     *
     * <p>Y 67 y 82 desde el {@code backend#122}: la cuota de accidentes de trabajo y su tipo, los
     * dos {@code PERIOD}.
     */
    private static final int STEPS_IN_A_SPLIT_MONTH = 82;

    /**
     * Los pasos que llevan orden de recibo: los que PUEDEN ser linea.
     *
     * <p>Eran 14 y fueron 15 desde el {@code backend#104}, que declaro las horas extra: el
     * {@code 102} lleva orden de recibo como cualquier devengo. Fueron 17 desde el
     * {@code backend#111}, que imprimio el recuadro de bases: {@code B_CC} y {@code B01} se
     * calculaban desde siempre y ahora ademas llevan orden. Y son 18 desde el
     * {@code backend#114}, que le dio total propio al recuadro de aportacion empresarial. Y son
     * 29 desde el {@code backend#121}: el recuadro de bases pasa de tres lineas a las diez del
     * modelo oficial, y las horas extraordinarias estrenan sus dos cuotas de cotizacion
     * adicional, la del trabajador y la de la empresa. Y son 30 desde el {@code backend#122},
     * que anade la de accidentes de trabajo.
     */
    private static final int STEPS_WITH_A_PAYSLIP_ORDER = 30;

    /**
     * Y las lineas que el folio acaba imprimiendo, que ya no son las mismas.
     *
     * <p>Hasta el {@code backend#104} estos dos numeros eran uno solo, y eso era una casualidad
     * de la siembra: todos los conceptos con orden de recibo valian algo en todos los recibos. La
     * regla del cero los separa —una linea de concepto a cero no se imprime— y este empleado no
     * tiene horas extra, asi que el {@code 102} se calcula, se guarda como paso y no llega al
     * papel. <b>Que estos dos numeros se hayan separado no es un fallo: es el issue.</b>
     *
     * <p>Eran 14 y fueron 16 desde el {@code backend#111}: las dos bases del recuadro de
     * cotizacion valen algo en cualquier recibo con presencia, asi que suben los dos recuentos a
     * la vez y la diferencia entre ellos sigue siendo la misma, el {@code 102} a cero. Son 17
     * desde el {@code backend#114}, y por lo mismo.
     *
     * <p>Y 18 desde el {@code backend#119}. De las dos puertas de la prorrata se imprime siempre
     * UNA —la otra vale cero—, asi que el catalogo sube de dos en dos y el recibo de uno en uno.
     * Este empleado no tiene vertical de regimen, o sea que no prorratea, y lo que sale es el
     * {@code B02} en el recuadro de bases.
     *
     * <p>Y 23 desde el {@code backend#121}: el recuadro pasa de tres lineas a diez, pero este
     * empleado no tiene horas extra, asi que las cinco que cuelgan de ellas valen cero y no se
     * imprimen —el {@code 102}, su base {@code B08}, la linea {@code B06} con la que entra en la
     * base profesional y las dos cuotas {@code 704} y {@code 726}—. El {@code B02} tampoco sale
     * ya por su cuenta: entra sumado en el {@code B04}.
     *
     * <p>Y 24 desde el {@code backend#122}: la cuota de accidentes de trabajo vale algo en
     * cualquier recibo con presencia, asi que sube los dos recuentos a la vez.
     */
    private static final int PAYSLIP_LINES = 24;

    private static final String STEPS_URL =
            "/payrolls/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}"
                    + "/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}/steps";

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

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
    @WithMockUser(roles = "ADMIN")
    void aWholeMonthServesItsStepsInExecutionOrder_andSaysWhichOnesReachedThePayslip() throws Exception {
        String emp = hireWholeMonth();
        calculate(emp);

        List<Map<String, Object>> steps = getSteps(emp);
        assertEquals(STEPS_IN_A_WHOLE_MONTH, steps.size(), "pasos de un empleado de mes entero");

        // El orden de ejecucion sale como una serie completa, en ese orden y sin huecos. Un
        // "order by" que se cayera, o un assembler que agrupara, se ve aqui y solo aqui.
        assertEquals(
                IntStream.rangeClosed(1, STEPS_IN_A_WHOLE_MONTH).boxed().toList(),
                column(steps, "executionOrder"),
                "los pasos salen en orden de ejecucion, del 1 al " + STEPS_IN_A_WHOLE_MONTH);

        // Y ese orden NO es el del folio. Si lo fuera, este endpoint no aportaria nada que el
        // recibo no tenga ya: la lista de importes ordenada para imprimir es el recibo.
        List<Object> ordenDeFolio = column(steps, "payslipOrderCode").stream()
                .filter(Objects::nonNull).toList();
        assertEquals(STEPS_WITH_A_PAYSLIP_ORDER, ordenDeFolio.size(),
                "los pasos que llevan orden de recibo");
        assertNotEquals(
                ordenDeFolio.stream().map(String::valueOf).sorted().toList(),
                ordenDeFolio.stream().map(String::valueOf).toList(),
                "el orden de ejecucion no coincide con el del folio, que es el motivo de que este "
                        + "endpoint exista");

        // Y el recibo tiene una linea MENOS que pasos con orden: el 102 vale cero y no se imprime.
        // Es la regla del cero del backend#104, y es lo que separa estos dos recuentos.
        assertEquals(PAYSLIP_LINES, (int) jdbc.queryForObject(
                "select count(*) from payroll.payroll_concept where payroll_id = ?",
                Integer.class, payrollId(emp)), "lineas de recibo");

        // Y los que no llegan al folio estan servidos, que es la explicacion entera.
        List<Object> naturalezas = column(steps, "functionalNature");
        assertTrue(naturalezas.contains("BASE"), "los BASE estan: son de donde sale el numero");
        assertTrue(naturalezas.contains("TECHNICAL"), "y los TECHNICAL tambien");

        // El ambito va explicito y las fechas dicen lo mismo que el, tambien al salir por HTTP.
        for (Map<String, Object> step : steps) {
            if ("PERIOD".equals(step.get("executionScope"))) {
                assertNull(step.get("segmentStartDate"), "un paso PERIOD no lleva segmento");
                assertNull(step.get("segmentEndDate"), "un paso PERIOD no lleva segmento");
            } else {
                assertNotNull(step.get("segmentStartDate"), "un paso de segmento siempre lo lleva");
                assertNotNull(step.get("segmentEndDate"), "un paso de segmento siempre lo lleva");
            }
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aSplitMonthServesConcept101Twice_withItsTwoSegmentsAndItsTwoRates() throws Exception {
        String emp = hireWithSplitWorkingTime(new BigDecimal("100.00"), new BigDecimal("50.00"));
        calculate(emp);

        List<Map<String, Object>> steps = getSteps(emp);
        assertEquals(STEPS_IN_A_SPLIT_MONTH, steps.size(), "pasos de un empleado del mes partido");

        // El caso que rompe cualquier implementacion que indexe o agrupe por concepto: el 101 sale
        // dos veces, y las dos filas son distintas en todo menos en el codigo.
        List<Map<String, Object>> salarioBase = steps.stream()
                .filter(step -> "101".equals(step.get("conceptCode")))
                .toList();

        assertEquals(2, salarioBase.size(), "SALARIO_BASE, una vez por tramo");
        assertNotEquals(salarioBase.get(0).get("executionOrder"), salarioBase.get(1).get("executionOrder"),
                "dos pasos distintos: la clave de fila es executionOrder, no conceptCode");
        assertNotEquals(salarioBase.get(0).get("rate"), salarioBase.get(1).get("rate"),
                "dos precios distintos, no la misma fila dos veces");
        assertEquals("2025-04-15", salarioBase.get(0).get("segmentEndDate"), "el primer tramo cierra el 15");
        assertEquals("2025-04-16", salarioBase.get(1).get("segmentStartDate"), "y el segundo abre el 16");

        // Y los dos llegan al folio con el mismo orden de recibo, que es justo por lo que la clave
        // de fila no puede ser esa.
        assertEquals(salarioBase.get(0).get("payslipOrderCode"), salarioBase.get(1).get("payslipOrderCode"));
    }

    /**
     * Un recibo calculado antes de la {@code V129} no tiene pasos, y eso no es un {@code 404}.
     *
     * <p>El escenario se fabrica borrando los pasos de un recibo recien calculado, que es
     * exactamente el estado en que esta hoy la semilla entera: 873 recibos calculados antes de que
     * la tabla existiera. La lista vacia no puede leerse como «este recibo no tiene conceptos», y
     * no se rellena derivandola de {@code payroll_concept}: sus 24 lineas no son 67 pasos.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void aPayrollCalculatedBeforeTheStepsExistedAnswersTwoHundredWithAnEmptyList() throws Exception {
        String emp = hireWholeMonth();
        calculate(emp);
        Long pid = payrollId(emp);

        assertEquals(STEPS_IN_A_WHOLE_MONTH, jdbc.update(
                "delete from payroll.payroll_calculation_step where payroll_id = ?", pid));
        assertTrue(jdbc.queryForObject(
                "select count(*) from payroll.payroll_concept where payroll_id = ?",
                Integer.class, pid) > 0, "y el recibo sigue teniendo sus lineas");

        assertEquals(List.of(), getSteps(emp),
                "el recibo existe y no tiene pasos: lista vacia, no 404 y no las lineas del folio");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anAddressWithNoPayrollBehindItIsAFourOhFour() throws Exception {
        mockMvc.perform(get(STEPS_URL, RULE_SYSTEM, EMPLOYEE_TYPE, "NOSUCHEMP", PERIOD, PAYROLL_TYPE, 1))
                .andExpect(status().isNotFound());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<Map<String, Object>> getSteps(String employeeNumber) throws Exception {
        String body = mockMvc
                .perform(get(STEPS_URL, RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE, 1))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JSON.readValue(body, new TypeReference<List<Map<String, Object>>>() { });
    }

    private static List<Object> column(List<Map<String, Object>> steps, String field) {
        // Con los nulos dentro y en su sitio: la posicion importa para poder cruzar columnas.
        return steps.stream().map(step -> step.get(field)).toList();
    }

    private void calculate(String employeeNumber) {
        assertEquals("COMPLETED", launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employeeNumber),
                        null),
                null)).status());
        // Los pasos llevan clave asignada y su insert queda diferido hasta el vaciado de la
        // sesion; estos tests van en transaccion y no hay commit que vacie por ellos (ADR-062).
        entityManager.flush();
    }

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
        return "ST" + (System.nanoTime() % 1_000_000_000L);
    }

    private Long payrollId(String employeeNumber) {
        Long id = jdbc.queryForObject(
                "select id from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = 1"
                        + "   and status = 'CALCULATED'",
                Long.class, RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE);
        assertNotNull(id);
        return id;
    }
}
