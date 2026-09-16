package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.scenario.PayrollScenarioFixtures;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La cadena del {@code EMPLOYEE_INPUT}, recorrida entera (workforce-loader#5, {@code backend#104}).
 *
 * <p>El {@code workforce-loader#5} se quedó abierto por una sola cosa:
 * {@code employee.employee_payroll_input} está a cero y sembrarla exigiría un concepto que la
 * consuma, porque de los 35 conceptos del motor ninguno declaraba {@code EMPLOYEE_INPUT}. La
 * conclusión de entonces fue que lo que falta es «declarar un concepto, no diseñar un mecanismo».
 * Este test recorre la cadena con un concepto declarado a propósito, y mide las dos cosas que
 * decidían si eso basta.
 *
 * <p><b>1. El motor lo calcula.</b> Con una fila de entrada, el concepto sale con
 * {@code cantidad × precio}. La cadena está entera y funciona; lo que faltaba era la fila del
 * catálogo, tal cual estaba dicho.
 *
 * <p><b>2. Y sin fila de entrada, el cero no se imprime.</b> Ésta es la que no estaba medida, y
 * durante un tiempo este test afirmó <b>lo contrario</b>: que la línea salía igual, valiendo cero.
 * Era verdad y era el hueco — {@code isPayslipLine()} miraba {@code payslipOrderCode != null} y
 * nada miraba el importe, así que un concepto ocasional salía en el recibo de todo el mundo. Y no
 * se puede dar sólo a quien tenga horas: {@code payroll_engine.concept_assignment} acota por
 * sociedad, convenio y tipo de empleado — <b>no por empleado</b>.
 *
 * <p>Lo cerró la regla del cero del {@code backend#104}: una línea de concepto a cero no se
 * imprime, y eso vive en la proyección del recibo y no en el motor (ADR-062 §1). Este test está
 * dado la vuelta a propósito, y el párrafo que decía «el día que esa regla exista, este test se
 * pondrá rojo» se ha borrado porque ese día fue éste.
 *
 * <p><b>Lo que NO cambió es el paso.</b> El concepto se sigue calculando, sigue dando cero y sigue
 * guardado en {@code payroll_calculation_step} con su orden de recibo. Lo que falta es su línea. Es
 * la diferencia entre «no se calculó» y «se calculó y dio cero», y el tercer test la mide.
 */
@TestWebSobreEsquemaReal
class AnEmployeeInputConceptCalculatesAndItsZeroLineIsNotPrintedTest {

    private static final String RULE_SYSTEM = "EIN";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD = "202501";
    private static final String PAYROLL_TYPE = "NORMAL";
    private static final LocalDate PERIOD_START = LocalDate.of(2025, 1, 1);
    private static final String AGREEMENT_CODE = "99002405011982";

    /** El concepto de entrada: lo que la persona declara, sin precio. */
    private static final String INPUT_CONCEPT = "H01";
    /** Lo que se cobra por ello, y lo único que sale en el folio. */
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
            declararConceptoDeEntrada();
        }
    }

    @Test
    void withAnInputRowTheEngineCalculatesIt() {
        String employee = hire();
        insertPayrollInput(employee, new BigDecimal("10.0000"));

        launch(employee);

        // El precio por dia de la reglamentacion de prueba es el que usa el salario base, y aqui se
        // reutiliza como precio de la hora: lo que se mide es la cadena, no la tarifa.
        BigDecimal precio = jdbcTemplate.queryForObject(
                "select daily_value from payroll.payroll_table_row where rule_system_code = ?",
                BigDecimal.class, RULE_SYSTEM);

        Map<String, BigDecimal> lineas = payslipLines(employee);
        assertTrue(lineas.containsKey(EARNING_CONCEPT),
                "el concepto de entrada no llego al recibo: " + lineas);
        assertEquals(0, precio.multiply(new BigDecimal("10")).compareTo(lineas.get(EARNING_CONCEPT)),
                "cantidad x precio: " + lineas);
    }

    /**
     * Y sin fila de entrada, el concepto no estrena una línea de {@code 0,00} en el recibo.
     *
     * <p>Es la regla del cero del {@code backend#104}. Sin ella, declarar un concepto que le aplica
     * a unos pocos le pone una línea vacía a todos los demás, que es exactamente lo que impedía
     * sembrar las horas extra.
     */
    @Test
    void withoutAnInputRowNoZeroLineIsPrinted() {
        String employee = hire();

        launch(employee);

        Map<String, BigDecimal> lineas = payslipLines(employee);
        assertFalse(lineas.containsKey(EARNING_CONCEPT),
                "un concepto que vale cero no se imprime, y este vale cero: " + lineas);
    }

    /**
     * Pero el paso está, y dice cero. Es la mitad que la regla del cero no puede tocar.
     *
     * <p>«No se imprimió» y «no se calculó» son dos cosas distintas, y quien mire la pestaña
     * «Cálculo» para entender un recibo necesita poder distinguirlas. La regla vive en la
     * proyección: borrar el paso del motor sería mentir en la explicación para arreglar el
     * documento.
     */
    @Test
    void butTheStepIsThere_andItSaysZero() {
        String employee = hire();

        launch(employee);

        Map<String, Object> paso = jdbcTemplate.queryForMap("""
                select s.amount, s.payslip_order_code, s.payslip_line_number
                  from payroll.payroll_calculation_step s
                  join payroll.payroll p on p.id = s.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ?
                   and p.payroll_period_code = ? and p.payroll_type_code = ?
                   and s.concept_code = ?
                """, RULE_SYSTEM, employee, PERIOD, PAYROLL_TYPE, EARNING_CONCEPT);

        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) paso.get("amount")),
                "el calculo ocurrio y dio cero: " + paso);
        assertEquals(EARNING_CONCEPT, paso.get("payslip_order_code"),
                "y el paso sigue diciendo que tiene sitio en el folio: " + paso);
        assertNull(paso.get("payslip_line_number"),
                "lo que no tiene es linea, porque no se imprimio: " + paso);
    }

    /**
     * Un concepto {@code EMPLOYEE_INPUT} y el devengo que lo consume, declarados como los declararia
     * una migracion: objeto, concepto, operandos, alimentacion al total y fila de ambito.
     */
    private void declararConceptoDeEntrada() {
        for (String code : new String[]{INPUT_CONCEPT, EARNING_CONCEPT}) {
            jdbcTemplate.update(
                    "insert into payroll_engine.payroll_object"
                            + " (rule_system_code, object_type_code, object_code, created_at, updated_at)"
                            + " values (?, 'CONCEPT', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    RULE_SYSTEM, code);
        }

        Long idEntrada = objectId(INPUT_CONCEPT);
        Long idDevengo = objectId(EARNING_CONCEPT);
        Long idPrecio = objectId("P02");
        Long idTotalDevengos = objectId("970");

        String concepto = "insert into payroll_engine.payroll_concept"
                + " (object_id, concept_mnemonic, calculation_type, functional_nature,"
                + "  payslip_order_code, execution_scope, created_at, updated_at)"
                + " values (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        // La cantidad que declara la persona: no lleva orden de folio, porque no es una linea.
        jdbcTemplate.update(concepto, idEntrada, "HORAS_EXTRA", "EMPLOYEE_INPUT", "INFORMATIONAL", null, "PERIOD");
        // Y lo que se cobra por ella, que si lo es.
        jdbcTemplate.update(concepto, idDevengo, "IMPORTE_HORAS_EXTRA", "RATE_BY_QUANTITY", "EARNING",
                EARNING_CONCEPT, "PERIOD");

        String operando = "insert into payroll_engine.payroll_concept_operand"
                + " (target_object_id, operand_role, source_object_id, created_at, updated_at)"
                + " values (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        jdbcTemplate.update(operando, idDevengo, "QUANTITY", idEntrada);
        jdbcTemplate.update(operando, idDevengo, "RATE", idPrecio);

        jdbcTemplate.update(
                "insert into payroll_engine.payroll_concept_feed_relation"
                        + " (source_object_id, target_object_id, feed_mode, feed_value, invert_sign,"
                        + "  effective_from, effective_to, created_at, updated_at)"
                        + " values (?, ?, 'FEED_BY_SOURCE', null, false, DATE '2025-01-01', null,"
                        + "         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                idDevengo, idTotalDevengos);

        jdbcTemplate.update(
                "insert into payroll_engine.concept_assignment"
                        + " (rule_system_code, concept_code, company_code, agreement_code, employee_type_code,"
                        + "  valid_from, valid_to, priority, created_at, updated_at)"
                        + " values (?, ?, null, ?, null, DATE '2025-01-01', null, 102,"
                        + "         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                RULE_SYSTEM, EARNING_CONCEPT, AGREEMENT_CODE);
    }

    private Long objectId(String conceptCode) {
        return jdbcTemplate.queryForObject(
                "select id from payroll_engine.payroll_object"
                        + " where rule_system_code = ? and object_type_code = 'CONCEPT' and object_code = ?",
                Long.class, RULE_SYSTEM, conceptCode);
    }

    private void insertPayrollInput(String employee, BigDecimal quantity) {
        jdbcTemplate.update(
                "insert into employee.employee_payroll_input"
                        + " (rule_system_code, employee_type_code, employee_number, concept_code, period, quantity)"
                        + " values (?, ?, ?, ?, ?, ?)",
                RULE_SYSTEM, EMPLOYEE_TYPE, employee, INPUT_CONCEPT, Integer.parseInt(PERIOD), quantity);
    }

    private Map<String, BigDecimal> payslipLines(String employee) {
        List<Map<String, Object>> filas = jdbcTemplate.queryForList("""
                select c.concept_code, c.amount
                  from payroll.payroll_concept c
                  join payroll.payroll p on p.id = c.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ?
                   and p.payroll_period_code = ? and p.payroll_type_code = ?
                """, RULE_SYSTEM, employee, PERIOD, PAYROLL_TYPE);
        assertTrue(!filas.isEmpty(), "el recibo no tiene ni una linea");
        return filas.stream().collect(java.util.stream.Collectors.toMap(
                f -> (String) f.get("concept_code"),
                f -> (BigDecimal) f.get("amount")));
    }

    private String hire() {
        String employeeNumber = "EI" + (System.nanoTime() % 1_000_000_000L);
        long employeeId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber);
        fixtures.insertPresence(employeeId, 1, PERIOD_START, null);
        fixtures.insertLaborClassification(employeeId, PERIOD_START);
        fixtures.insertWorkingTime(employeeId, new BigDecimal("100.00"), PERIOD_START, null);
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
        assertEquals(1, run.totalCalculated(), "el escenario necesita un recibo calculado: "
                + runMessages(run.id()));
    }

    private String runMessages(Long runId) {
        return String.join(",", jdbcTemplate.queryForList(
                "select message_code || ' ' || message from payroll.calculation_run_message where run_id = ?",
                String.class, runId));
    }
}
