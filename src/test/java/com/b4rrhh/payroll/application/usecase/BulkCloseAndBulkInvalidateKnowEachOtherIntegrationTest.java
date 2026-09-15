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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los criterios 3 y 4 del {@code backend#102}: un periodo con recibos en varios estados, cerrado en
 * masa, y despues el invalidador masivo del mismo periodo contando esos recibos en <b>Protegidas</b>.
 *
 * <p>El criterio 4 es el que cierra el circulo, y por eso los dos verbos se prueban en el mismo
 * test y no en dos: {@code totalSkippedProtected} lleva meses valiendo cero —no habia forma de
 * cerrar en masa— y dejara de valerlo en cuanto exista esto. Un contador que nunca se ha visto
 * distinto de cero no se sabe si cuenta.
 *
 * <p>Fuera de transaccion ({@code NOT_SUPPORTED}) a proposito. Lo que hay que afirmar es lo que
 * <b>quedo</b> en la base, no lo que la sesion tenia encolado: un cierre masivo son
 * {@code update}s sobre filas que ya existen, y dentro de una transaccion sin confirmar un
 * {@code JdbcTemplate} no los ve. Es la cuarta regla del {@code b4rrhh/workspace#3}, y el criterio
 * pide expresamente que las cuentas cuadren con lo que hay en la base.
 *
 * <p>Por eso cada test usa su propia reglamentacion, y no la {@code TST} que comparten los demas
 * tests: lo que aqui se confirma se queda en el clon y no se lo encuentra nadie mas.
 */
@TestWebSobreEsquemaReal
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BulkCloseAndBulkInvalidateKnowEachOtherIntegrationTest {

    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD = "202501";
    private static final String PAYROLL_TYPE = "NORMAL";
    private static final LocalDate PERIOD_START = LocalDate.of(2025, 1, 1);

    @Autowired
    private LaunchPayrollCalculationUseCase launchPayrollCalculationUseCase;

    @Autowired
    private BulkFinalizePayrollUseCase bulkFinalizePayrollUseCase;

    @Autowired
    private BulkInvalidatePayrollUseCase bulkInvalidatePayrollUseCase;

    @Autowired
    private ValidatePayrollUseCase validatePayrollUseCase;

    @Autowired
    private InvalidatePayrollUseCase invalidatePayrollUseCase;

    @Autowired
    private FinalizePayrollUseCase finalizePayrollUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PayrollScenarioFixtures fixtures;

    /**
     * Cada test tiene la suya, y no es manía: los dos apuntan a <b>todo el periodo</b>, que es el
     * gesto de verdad, y aquí no hay rollback que los separe. Compartiéndola, el segundo vería los
     * empleados del primero y la cuenta dejaría de ser la del escenario que monta.
     */
    private String ruleSystem;

    @BeforeEach
    void setUpData() {
        fixtures = new PayrollScenarioFixtures(jdbcTemplate);
    }

    private void usar(String ruleSystemCode) {
        ruleSystem = ruleSystemCode;
        Integer alreadySeeded = jdbcTemplate.queryForObject(
                "select count(*) from rulesystem.rule_system where code = ?", Integer.class, ruleSystem);
        if (alreadySeeded == 0) {
            fixtures.seedConceptGraph(ruleSystem);
        }
    }

    @Test
    void closesThePeriodAndTheInvalidatorThenSeesThoseReceiptsAsProtected() {
        usar("CLB");

        String calculada = hireAndCalculate();
        String validada = hireAndCalculate();
        String invalida = hireAndCalculate();
        String yaCerrada = hireAndCalculate();

        validate(validada);
        invalidate(invalida);
        finalizeOne(yaCerrada);

        assertEquals(
                Map.of("CALCULATED", 1L, "EXPLICIT_VALIDATED", 1L, "NOT_VALID", 1L, "DEFINITIVE", 1L),
                statusCounts(),
                "el escenario necesita un recibo en cada estado");

        // --- Criterio 3: cerrar el periodo entero ---
        BulkFinalizePayrollResult cierre = bulkFinalizePayrollUseCase.finalizeBulk(
                new BulkFinalizePayrollCommand(ruleSystem, PERIOD, PAYROLL_TYPE, todoElPeriodo()));

        assertEquals(4, cierre.totalCandidates());
        assertEquals(4, cierre.totalFound());
        assertEquals(2, cierre.totalFinalized(), "se cierran la calculada y la validada");
        assertEquals(1, cierre.totalSkippedAlreadyDefinitive(), "la que ya estaba cerrada");
        assertEquals(1, cierre.totalSkippedNotEligibleByStatus(), "y la invalida, que no es un fallo");
        assertEquals(0, cierre.totalSkippedNotFound());

        // Y las cuentas cuadran con lo que hay en la base, que es lo que el criterio pide.
        assertEquals(Map.of("DEFINITIVE", 3L, "NOT_VALID", 1L), statusCounts());
        assertEquals("DEFINITIVE", statusOf(calculada));
        assertEquals("DEFINITIVE", statusOf(validada));
        assertEquals("DEFINITIVE", statusOf(yaCerrada));
        assertEquals("NOT_VALID", statusOf(invalida),
                "el recibo invalido no se toca: NOT_VALID -> DEFINITIVE no existe");

        // --- Criterio 4: el invalidador masivo del mismo periodo ---
        BulkInvalidatePayrollResult invalidacion = bulkInvalidatePayrollUseCase.invalidateBulk(
                new BulkInvalidatePayrollCommand(
                        ruleSystem, PERIOD, PAYROLL_TYPE, "BULK_RESET", todoElPeriodo()));

        assertEquals(4, invalidacion.totalCandidates());
        assertEquals(0, invalidacion.totalInvalidated(), "no queda nada invalidable");
        assertEquals(3, invalidacion.totalSkippedProtected(),
                "los tres recibos cerrados salen en Protegidas: es donde los dos verbos se conocen");
        assertEquals(1, invalidacion.totalSkippedAlreadyNotValid());

        // Y el cierre aguanta: invalidar en masa no deshace un cierre.
        assertEquals(Map.of("DEFINITIVE", 3L, "NOT_VALID", 1L), statusCounts());
    }

    /**
     * Cerrar no prohibe calcular en el periodo. Un alta posterior produce un recibo que nunca se
     * cerro, asi que no viola nada — y es la unica de las tres cosas que el issue dice que el
     * cierre NO hace que se puede comprobar ejecutando, porque las otras dos son ausencias.
     */
    @Test
    void closingThePeriodDoesNotForbidCalculatingInIt() {
        usar("CLC");

        String primero = hireAndCalculate();
        bulkFinalizePayrollUseCase.finalizeBulk(
                new BulkFinalizePayrollCommand(ruleSystem, PERIOD, PAYROLL_TYPE, todoElPeriodo()));
        assertEquals("DEFINITIVE", statusOf(primero));

        String tardio = hireAndCalculate();

        assertEquals("CALCULATED", statusOf(tardio),
                "un recibo que no existia no estaba cerrado");
        assertEquals("DEFINITIVE", statusOf(primero),
                "y el que si estaba cerrado sigue estandolo");
    }

    private PayrollLaunchTargetSelection todoElPeriodo() {
        return new PayrollLaunchTargetSelection(
                PayrollLaunchTargetSelectionType.ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD, null, null);
    }

    private String hireAndCalculate() {
        String employeeNumber = "CL" + (System.nanoTime() % 1_000_000_000L);
        long employeeId = fixtures.insertEmployee(ruleSystem, EMPLOYEE_TYPE, employeeNumber);
        fixtures.insertPresence(employeeId, 1, PERIOD_START, null);
        fixtures.insertLaborClassification(employeeId, PERIOD_START);
        fixtures.insertWorkingTime(employeeId, new BigDecimal("100.00"), PERIOD_START, null);

        var run = launchPayrollCalculationUseCase.launch(new LaunchPayrollCalculationCommand(
                ruleSystem, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employeeNumber),
                        null),
                null));
        assertEquals(1, run.totalCalculated(), "el escenario necesita un recibo de partida");
        return employeeNumber;
    }

    private void validate(String employee) {
        validatePayrollUseCase.validate(new ValidatePayrollCommand(
                ruleSystem, EMPLOYEE_TYPE, employee, PERIOD, PAYROLL_TYPE, 1));
    }

    private void invalidate(String employee) {
        invalidatePayrollUseCase.invalidate(new InvalidatePayrollCommand(
                ruleSystem, EMPLOYEE_TYPE, employee, PERIOD, PAYROLL_TYPE, 1, "TEST"));
    }

    private void finalizeOne(String employee) {
        finalizePayrollUseCase.finalizePayroll(new FinalizePayrollCommand(
                ruleSystem, EMPLOYEE_TYPE, employee, PERIOD, PAYROLL_TYPE, 1));
    }

    /** Cuantos recibos del periodo hay en cada estado, leidos de la base y no de la sesion. */
    private Map<String, Long> statusCounts() {
        List<Map<String, Object>> filas = jdbcTemplate.queryForList("""
                select status, count(*) as total
                  from payroll.payroll
                 where rule_system_code = ? and payroll_period_code = ? and payroll_type_code = ?
                 group by status
                """, ruleSystem, PERIOD, PAYROLL_TYPE);
        assertTrue(!filas.isEmpty(), "no hay ningun recibo del periodo: el escenario no se monto");
        return filas.stream().collect(java.util.stream.Collectors.toMap(
                f -> (String) f.get("status"),
                f -> ((Number) f.get("total")).longValue()));
    }

    private String statusOf(String employee) {
        return jdbcTemplate.queryForObject(
                "select status from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = ?",
                String.class, ruleSystem, EMPLOYEE_TYPE, employee, PERIOD, PAYROLL_TYPE, 1);
    }
}
