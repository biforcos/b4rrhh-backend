package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.scenario.PayrollScenarioFixtures;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La regla del cero, y la excepcion que se pierde si no se fija ({@code backend#104}).
 *
 * <blockquote><b>Una linea de concepto a cero no se imprime. Un total a cero, si.</b></blockquote>
 *
 * <p>Un concepto que no aplica no sale en la nomina: nadie cobra una linea de cero euros. Sin esta
 * regla, declarar un concepto ocasional estrena una primera linea de {@code 0,00} en el recibo de
 * todo el que no lo tenga, porque {@code concept_assignment} acota por sociedad, convenio y tipo de
 * empleado y <b>no por empleado</b>.
 *
 * <p>Y la otra mitad, que es la que impide que esto sea una mentira: <b>el paso sigue existiendo</b>.
 * La regla vive en la proyeccion y no en el motor — un paso a cero es un calculo que ocurrio y dio
 * cero, y borrarlo del motor seria mentir en la explicacion para arreglar el documento (ADR-062 §1).
 */
@TestWebSobreEsquemaReal
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConceptLinesWorthZeroDoNotReachThePayslipTest {

    private static final String RULE_SYSTEM = "CER";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD = "202501";
    private static final String PAYROLL_TYPE = "NORMAL";
    private static final LocalDate PERIOD_START = LocalDate.of(2025, 1, 1);
    private static final String AGREEMENT_CODE = "99002405011982";
    private static final String INPUT_CONCEPT = "H01";
    private static final String EARNING_CONCEPT = "102";

    @Autowired
    private LaunchPayrollCalculationUseCase launchPayrollCalculationUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PayrollScenarioFixtures fixtures;

    @BeforeEach
    void setUpData() {
        fixtures = new PayrollScenarioFixtures(jdbcTemplate);
        Integer alreadySeeded = jdbcTemplate.queryForObject(
                "select count(*) from rulesystem.rule_system where code = ?", Integer.class, RULE_SYSTEM);
        if (alreadySeeded == 0) {
            fixtures.seedConceptGraph(RULE_SYSTEM);
            declararHorasExtra();
        }
    }

    /**
     * El caso que hace falta la regla: alguien sin horas extra.
     *
     * <p>El concepto esta asignado a su convenio, asi que el motor lo evalua y da cero. Lo que este
     * test afirma es que ese cero <b>no llega al folio</b> y que el paso <b>si</b> queda guardado.
     */
    @Test
    void aConceptWorthZeroIsCalculatedAndKeptButNotPrinted() {
        String employee = hire();
        launch(employee);

        assertFalse(payslipConcepts(employee).contains(EARNING_CONCEPT),
                "el concepto a cero no puede salir en el folio: " + payslipLines(employee));

        Map<String, Object> paso = step(employee, EARNING_CONCEPT);
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) paso.get("amount")),
                "y el paso tiene que seguir ahi, con su cero: " + paso);
        assertTrue(paso.get("payslip_order_code") != null,
                "el paso conserva su orden de folio: lo que cambia es lo que el folio pinta");
        assertTrue(paso.get("payslip_line_number") == null,
                "pero no tiene linea, porque su linea no se imprimio: " + paso);
    }

    /**
     * <b>La excepcion, que es la que se pierde.</b> Un total a cero si se imprime: un liquido de
     * {@code 0,00} dice que ese mes no se cobro, y eso es un dato. Alguien limpiando ceros lo
     * borraria sin este test.
     *
     * <p>Y no afirma nada mas. Tuvo una linea de mas —«y el concepto a cero de este mismo recibo no
     * esta»— que lo ponia rojo tambien al <b>retirar</b> la regla entera, que es un sabotaje que no
     * toca la excepcion. Un test que se pone rojo con dos sabotajes distintos no dice cual de los
     * dos ha ocurrido, y esta excepcion necesita un test que solo se rompa cuando se rompe ella. La
     * otra mitad ya la sujeta {@link #aConceptWorthZeroIsCalculatedAndKeptButNotPrinted}.
     */
    @Test
    void aTotalWorthZeroIsStillPrinted() {
        String employee = hireWithNoPay();
        launch(employee);

        List<String> conceptos = payslipConcepts(employee);
        assertTrue(conceptos.containsAll(List.of("970", "980", "990")),
                "los tres totales se imprimen aunque valgan cero: " + payslipLines(employee));

        BigDecimal liquido = (BigDecimal) payslipLines(employee).stream()
                .filter(l -> "990".equals(l.get("concept_code")))
                .findFirst().orElseThrow().get("amount");
        assertEquals(0, BigDecimal.ZERO.compareTo(liquido),
                "y el liquido es cero, que es justo el dato: " + payslipLines(employee));

    }

    /** Con horas declaradas, el concepto calcula cantidad x precio y si llega al folio. */
    @Test
    void withInputRowsTheConceptIsCalculatedAndPrinted() {
        String employee = hire();
        insertPayrollInput(employee, new BigDecimal("10.0000"));
        launch(employee);

        List<Map<String, Object>> lineas = payslipLines(employee);
        Map<String, Object> linea = lineas.stream()
                .filter(l -> EARNING_CONCEPT.equals(l.get("concept_code")))
                .findFirst().orElseThrow(() -> new AssertionError("no salio la linea: " + lineas));

        BigDecimal precio = jdbcTemplate.queryForObject(
                "select daily_value from payroll.payroll_table_row"
                        + " where rule_system_code = ? and table_code = ?",
                BigDecimal.class, RULE_SYSTEM, "P03_" + AGREEMENT_CODE);
        assertEquals(0, precio.multiply(new BigDecimal("10")).compareTo((BigDecimal) linea.get("amount")),
                "cantidad x precio: " + lineas);
    }

    /**
     * Criterio 5: la pestana «Calculo» distingue un concepto que dio cero de uno que no participo.
     *
     * <p>El primero deja paso con su cero; el segundo no deja nada. Es la diferencia entre «se
     * evaluo y no aplica» y «ni siquiera estaba en el plan», y la pestana la puede contar porque las
     * dos cosas se ven distintas en los datos.
     */
    @Test
    void theCalculationTrailTellsZeroApartFromNotParticipating() {
        String employee = hire();
        launch(employee);

        assertTrue(step(employee, EARNING_CONCEPT) != null,
                "el que dio cero deja paso");
        assertTrue(steps(employee).stream().noneMatch(s -> "999".equals(s.get("concept_code"))),
                "y uno que no esta en el plan no deja ninguno");
    }

    private void declararHorasExtra() {
        for (String code : new String[]{INPUT_CONCEPT, "P03", EARNING_CONCEPT}) {
            jdbcTemplate.update(
                    "insert into payroll_engine.payroll_object"
                            + " (rule_system_code, object_type_code, object_code, created_at, updated_at)"
                            + " values (?, 'CONCEPT', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    RULE_SYSTEM, code);
        }
        jdbcTemplate.update(
                "insert into payroll_engine.payroll_object"
                        + " (rule_system_code, object_type_code, object_code, created_at, updated_at)"
                        + " values (?, 'TABLE', 'P03_HOURLY_OVERTIME_TABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                RULE_SYSTEM);

        String concepto = "insert into payroll_engine.payroll_concept"
                + " (object_id, concept_mnemonic, calculation_type, functional_nature,"
                + "  payslip_order_code, execution_scope, created_at, updated_at)"
                + " values (?, ?, ?, ?, ?, 'PERIOD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        jdbcTemplate.update(concepto, objectId("CONCEPT", INPUT_CONCEPT), "HORAS_EXTRA",
                "EMPLOYEE_INPUT", "INFORMATIONAL", null);
        jdbcTemplate.update(concepto, objectId("CONCEPT", "P03"), "PRECIO_HORA_EXTRA",
                "DIRECT_AMOUNT", "BASE", null);
        jdbcTemplate.update(concepto, objectId("CONCEPT", EARNING_CONCEPT), "IMPORTE_HORAS_EXTRA",
                "RATE_BY_QUANTITY", "EARNING", EARNING_CONCEPT);

        String operando = "insert into payroll_engine.payroll_concept_operand"
                + " (target_object_id, operand_role, source_object_id, created_at, updated_at)"
                + " values (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        jdbcTemplate.update(operando, objectId("CONCEPT", EARNING_CONCEPT), "QUANTITY",
                objectId("CONCEPT", INPUT_CONCEPT));
        jdbcTemplate.update(operando, objectId("CONCEPT", EARNING_CONCEPT), "RATE",
                objectId("CONCEPT", "P03"));

        String feed = "insert into payroll_engine.payroll_concept_feed_relation"
                + " (source_object_id, target_object_id, feed_mode, feed_value, invert_sign,"
                + "  effective_from, effective_to, created_at, updated_at)"
                + " values (?, ?, 'FEED_BY_SOURCE', null, false, DATE '2025-01-01', null,"
                + "         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        jdbcTemplate.update(feed, objectId("TABLE", "P03_HOURLY_OVERTIME_TABLE"), objectId("CONCEPT", "P03"));
        jdbcTemplate.update(feed, objectId("CONCEPT", EARNING_CONCEPT), objectId("CONCEPT", "970"));

        jdbcTemplate.update(
                "insert into payroll.payroll_object_binding"
                        + " (rule_system_code, owner_type_code, owner_code, binding_role_code,"
                        + "  bound_object_type_code, bound_object_code, active)"
                        + " values (?, 'AGREEMENT', ?, 'P03_HOURLY_OVERTIME_TABLE', 'TABLE', ?, true)",
                RULE_SYSTEM, AGREEMENT_CODE, "P03_" + AGREEMENT_CODE);
        jdbcTemplate.update(
                "insert into payroll.payroll_table_row"
                        + " (rule_system_code, table_code, search_code, start_date, end_date, daily_value, active)"
                        + " values (?, ?, ?, DATE '2025-01-01', null, ?, true)",
                RULE_SYSTEM, "P03_" + AGREEMENT_CODE, "99002405-G2", new BigDecimal("5.94"));

        jdbcTemplate.update(
                "insert into payroll_engine.concept_assignment"
                        + " (rule_system_code, concept_code, company_code, agreement_code, employee_type_code,"
                        + "  valid_from, valid_to, priority, created_at, updated_at)"
                        + " values (?, ?, null, ?, null, DATE '2025-01-01', null, 102,"
                        + "         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                RULE_SYSTEM, EARNING_CONCEPT, AGREEMENT_CODE);
    }

    private Long objectId(String tipo, String codigo) {
        return jdbcTemplate.queryForObject(
                "select id from payroll_engine.payroll_object"
                        + " where rule_system_code = ? and object_type_code = ? and object_code = ?",
                Long.class, RULE_SYSTEM, tipo, codigo);
    }

    private void insertPayrollInput(String employee, BigDecimal quantity) {
        jdbcTemplate.update(
                "insert into employee.employee_payroll_input"
                        + " (rule_system_code, employee_type_code, employee_number, concept_code, period, quantity)"
                        + " values (?, ?, ?, ?, ?, ?)",
                RULE_SYSTEM, EMPLOYEE_TYPE, employee, INPUT_CONCEPT, Integer.parseInt(PERIOD), quantity);
    }

    private List<Map<String, Object>> payslipLines(String employee) {
        return jdbcTemplate.queryForList("""
                select c.line_number, c.concept_code, c.amount
                  from payroll.payroll_concept c
                  join payroll.payroll p on p.id = c.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ?
                 order by c.line_number
                """, RULE_SYSTEM, employee);
    }

    private List<String> payslipConcepts(String employee) {
        return payslipLines(employee).stream().map(l -> (String) l.get("concept_code")).toList();
    }

    private List<Map<String, Object>> steps(String employee) {
        return jdbcTemplate.queryForList("""
                select s.concept_code, s.amount, s.payslip_order_code, s.payslip_line_number
                  from payroll.payroll_calculation_step s
                  join payroll.payroll p on p.id = s.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ?
                 order by s.execution_order
                """, RULE_SYSTEM, employee);
    }

    private Map<String, Object> step(String employee, String conceptCode) {
        return steps(employee).stream()
                .filter(s -> conceptCode.equals(s.get("concept_code")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no hay paso de " + conceptCode));
    }

    private String hire() {
        String employeeNumber = "CE" + (System.nanoTime() % 1_000_000_000L);
        long employeeId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber);
        fixtures.insertPresence(employeeId, 1, PERIOD_START, null);
        fixtures.insertLaborClassification(employeeId, PERIOD_START);
        fixtures.insertWorkingTime(employeeId, new BigDecimal("100.00"), PERIOD_START, null);
        return employeeNumber;
    }

    /** Sin jornada: todo vale cero, que es el escenario donde la excepcion de los totales importa. */
    private String hireWithNoPay() {
        String employeeNumber = "CE" + (System.nanoTime() % 1_000_000_000L);
        long employeeId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber);
        fixtures.insertPresence(employeeId, 1, PERIOD_START, null);
        fixtures.insertLaborClassification(employeeId, PERIOD_START);
        fixtures.insertWorkingTime(employeeId, new BigDecimal("0.00"), PERIOD_START, null);
        return employeeNumber;
    }

    private void launch(String employee) {
        var run = launchPayrollCalculationUseCase.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employee),
                        null),
                null));
        assertEquals(1, run.totalCalculated(), "el escenario necesita un recibo calculado");
    }
}
