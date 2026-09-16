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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La cadena del {@code EMPLOYEE_INPUT}, recorrida entera (workforce-loader#5).
 *
 * <p>El issue se quedó abierto por una sola cosa: {@code employee.employee_payroll_input} está a
 * cero y sembrarla exigiría un concepto que la consuma, porque de los 35 conceptos del motor
 * ninguno declara {@code EMPLOYEE_INPUT}. La conclusión de entonces fue que lo que falta es
 * «declarar un concepto, no diseñar un mecanismo». Este test recorre la cadena con un concepto
 * declarado a propósito, y mide las dos cosas que decidían si eso basta.
 *
 * <p><b>1. El motor lo calcula.</b> Con una fila de entrada, el concepto sale con
 * {@code cantidad × precio}. La cadena está entera y funciona; lo que faltaba era la fila del
 * catálogo, tal cual estaba dicho.
 *
 * <p><b>2. Y sin fila de entrada, imprime una línea a cero.</b> Ésta es la que no estaba medida.
 * {@code isPayslipLine()} es {@code payslipOrderCode != null} y nada mira el importe, así que un
 * concepto ocasional sale en el recibo de todo el mundo aunque valga cero. Y no se puede dar sólo a
 * quien tenga horas: {@code payroll_engine.concept_assignment} acota por sociedad, convenio y tipo
 * de empleado — <b>no por empleado</b>.
 *
 * <p>Lo que eso significa para el issue está en su comentario: sobre la semilla de hoy, 12.227
 * líneas de recibo y <b>ninguna</b> a cero; declarar el concepto pondría la primera en unos 873
 * recibos. Hace falta una decisión más —una regla de «a cero no imprime», que por el ADR-062 §1
 * vive en la proyección del recibo— antes de sembrar nada.
 *
 * <p><b>Este test documenta el hueco, no lo bendice.</b> El día que esa regla exista, el segundo
 * test se pondrá rojo: eso es lo que se busca. Entonces se cambia la afirmación por la contraria y
 * se borra este párrafo.
 */
@TestWebSobreEsquemaReal
class AnEmployeeInputConceptCalculatesAndAlsoPrintsAZeroLineTest {

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
     * Y el hueco: sin fila de entrada, el mismo concepto sale igual, valiendo cero.
     *
     * <p>Si este test se pone rojo porque la línea ya no aparece, <b>es una buena noticia</b>: quiere
     * decir que existe la regla de «a cero no imprime» y que este test hay que darle la vuelta.
     */
    @Test
    void withoutAnInputRowItStillPrintsALineWorthZero() {
        String employee = hire();

        launch(employee);

        Map<String, BigDecimal> lineas = payslipLines(employee);
        assertTrue(lineas.containsKey(EARNING_CONCEPT),
                "si esta linea ha dejado de salir, lee el javadoc: el hueco esta arreglado y este"
                        + " test hay que darle la vuelta. Lineas: " + lineas);
        assertEquals(0, BigDecimal.ZERO.compareTo(lineas.get(EARNING_CONCEPT)),
                "la linea sale, y sale a cero: " + lineas);
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
