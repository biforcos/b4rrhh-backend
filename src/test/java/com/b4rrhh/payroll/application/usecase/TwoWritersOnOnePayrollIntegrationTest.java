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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La medida del criterio 1 del {@code backend#101}: si hoy dos caminos pueden escribir el mismo
 * recibo a la vez, y que queda si lo hacen.
 *
 * <p>Los dos caminos son los del issue: {@code POST /payrolls/…/recalculate}, que <b>no</b> toma
 * reserva en {@code calculation_claim}, y el lanzamiento, que si la toma. O sea que nada les
 * impide coincidir sobre la misma unidad, y este test los hace coincidir a proposito: dos hilos,
 * una barrera, el mismo recibo, y varias rondas.
 *
 * <p><b>Lo que se mide no es quien gana.</b> Es lo que el issue llama el caso peor: que los dos
 * escriban y el ultimo gane en silencio, dejando un recibo con los pasos de un calculo y las
 * lineas de otro. El invariante que lo detecta es
 * {@link #assertPayrollIsInternallyCoherent(String)}: las lineas del recibo son la proyeccion de
 * sus pasos, asi que la suma por concepto de los pasos que llegaron al folio tiene que ser
 * exactamente la suma por concepto de las lineas. Si un recibo se quedara con los pasos de un
 * calculo y las lineas de otro, esas dos sumas no cuadrarian.
 *
 * <p>Y el segundo invariante, {@link #assertOutcomeIsBusinessLevel(String, List)}: el que pierde
 * la carrera pierde <b>diciendolo</b>, con una excepcion de negocio que tiene nombre y respuesta,
 * no con un accidente de persistencia. Los dos invariantes se han visto caer: el primero alterando
 * el importe de la proyeccion a linea, el segundo quitando el {@code for update} de
 * {@code PayrollEmployeePresenceLookupAdapter}, que convierte al perdedor en un
 * {@code ObjectOptimisticLockingFailureException}.
 *
 * <p>Se comprueba tambien que los dos hilos se solaparon de verdad —sus intervalos de reloj se
 * cruzan—, porque un test de concurrencia que se ejecuta en serie no mide nada y no lo dice.
 *
 * <p>No va en transaccion ({@code NOT_SUPPORTED}): dos hilos solo se pisan si lo que escriben se
 * confirma. Por eso usa su propia reglamentacion, {@code CNC}, y no la {@code TST} que comparten
 * los demas tests de este contexto: lo que aqui se confirma se queda en el clon.
 */
@TestWebSobreEsquemaReal
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TwoWritersOnOnePayrollIntegrationTest {

    private static final String RULE_SYSTEM = "CNC";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD = "202501";
    private static final String PAYROLL_TYPE = "NORMAL";
    private static final LocalDate PERIOD_START = LocalDate.of(2025, 1, 1);
    private static final int ROUNDS = 6;

    @Autowired
    private LaunchPayrollCalculationUseCase launchPayrollCalculationUseCase;

    @Autowired
    private RecalculatePayrollUseCase recalculatePayrollUseCase;

    @Autowired
    private InvalidatePayrollUseCase invalidatePayrollUseCase;

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
        }
    }

    /**
     * El instrumento antes que la medida: en un recibo calculado por un solo camino, las dos sumas
     * cuadran. Si esto no fuera cierto, el invariante de abajo no probaria nada.
     */
    @Test
    void aReceiptWrittenByOnePathHasLinesThatAreTheProjectionOfItsSteps() {
        String employee = hire();
        launch(employee);

        assertPayrollIsInternallyCoherent(employee);
    }

    /**
     * La medida. Un recalculo y una corrida masiva sobre la misma unidad, a la vez, varias veces.
     */
    @Test
    void aRecalculationAndALaunchRacingForTheSameReceiptNeverLeaveItMixed() {
        String employee = hire();
        launch(employee);

        List<String> bitacora = new ArrayList<>();

        for (int round = 1; round <= ROUNDS; round++) {
            invalidate(employee);

            Carrera carrera = new Carrera(employee);
            carrera.run();

            bitacora.add("ronda %d | solape=%s | recalculo=%s | lanzamiento=%s"
                    .formatted(round, carrera.overlapped(), carrera.recalculationOutcome, carrera.launchOutcome));

            assertPayrollIsInternallyCoherent(employee);
            assertEquals(1, payrollRowCount(employee),
                    "la clave de negocio admite un recibo y solo uno");
            assertOutcomeIsBusinessLevel(carrera.recalculationOutcome, bitacora);
            assertOutcomeIsBusinessLevel(carrera.launchUnitFailure, bitacora);
        }

        System.out.println("[backend#101] " + String.join("\n[backend#101] ", bitacora));

        assertTrue(bitacora.stream().anyMatch(l -> l.contains("solape=true")),
                "ninguna ronda llego a solaparse: la medida no ha medido nada\n"
                        + String.join("\n", bitacora));
    }

    /**
     * El tercer camino es el unico que no pasa por el cerrojo de la presencia
     * —{@code InvalidatePayrollService} no lo toma—, y aun asi no llega a pelearse con el
     * recalculo. La razon no es un cerrojo: es la maquina de estados.
     *
     * <p>Invalidar exige {@code CALCULATED} y recalcular exige {@code NOT_VALID}, o sea que los dos
     * escritores necesitan estados opuestos y en cualquier instante solo uno de ellos es admisible.
     * Se mide saliendo de los dos estados: el que no toca se va con su excepcion de negocio con
     * nombre, y el recibo queda coherente y en un estado que se puede contar.
     */
    @Test
    void anInvalidationAndARecalculationCanNeverBothBeAdmissible() {
        String employee = hire();
        launch(employee);

        List<String> bitacora = new ArrayList<>();

        for (int round = 1; round <= ROUNDS; round++) {
            if (round % 2 == 1) {
                ensureCalculated(employee);
            } else {
                ensureNotValid(employee);
            }
            String estadoDePartida = currentStatus(employee);

            CarreraConInvalidacion carrera = new CarreraConInvalidacion(employee);
            carrera.run();

            bitacora.add("ronda %d | parte de %s | solape=%s | recalculo=%s | invalidacion=%s | queda %s"
                    .formatted(round, estadoDePartida, carrera.overlapped(), carrera.recalculationOutcome,
                            carrera.invalidationOutcome, currentStatus(employee)));

            assertPayrollIsInternallyCoherent(employee);
            assertEquals(1, payrollRowCount(employee),
                    "la clave de negocio admite un recibo y solo uno");
            assertOutcomeIsBusinessLevel(carrera.recalculationOutcome, bitacora);
            assertOutcomeIsBusinessLevel(carrera.invalidationOutcome, bitacora);
        }

        System.out.println("[backend#101 invalidar] "
                + String.join(System.lineSeparator() + "[backend#101 invalidar] ", bitacora));
    }

    /** Dos hilos, una barrera, el mismo recibo. */
    private final class Carrera {

        private final String employee;
        private final CyclicBarrier salida = new CyclicBarrier(2);

        private String recalculationOutcome;
        private String launchOutcome;
        /** Con que se fue la unidad del lanzamiento, si se fue mal. */
        private String launchUnitFailure = "OK";
        private long recalculationStart;
        private long recalculationEnd;
        private long launchStart;
        private long launchEnd;

        private Carrera(String employee) {
            this.employee = employee;
        }

        private boolean overlapped() {
            return recalculationStart < launchEnd && launchStart < recalculationEnd;
        }

        private void run() {
            Thread recalculo = new Thread(() -> {
                esperar(salida);
                recalculationStart = System.nanoTime();
                try {
                    recalculatePayrollUseCase.recalculate(new RecalculatePayrollCommand(
                            RULE_SYSTEM, EMPLOYEE_TYPE, employee, PERIOD, PAYROLL_TYPE, 1, "carrera"));
                    recalculationOutcome = "OK";
                } catch (RuntimeException ex) {
                    recalculationOutcome = ex.getClass().getSimpleName();
                } finally {
                    recalculationEnd = System.nanoTime();
                }
            }, "recalculo");

            Thread lanzamiento = new Thread(() -> {
                esperar(salida);
                launchStart = System.nanoTime();
                try {
                    var run = launchPayrollCalculationUseCase.launch(new LaunchPayrollCalculationCommand(
                            RULE_SYSTEM, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                            new PayrollLaunchTargetSelection(
                                    PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                                    new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employee),
                                    null),
                            "carrera"));
                    launchUnitFailure = unitFailureOf(run.id());
                    launchOutcome = "%s calculados=%d errores=%d reservados=%d yaReservados=%d | %s"
                            .formatted(run.status(), run.totalCalculated(), run.totalErrors(),
                                    run.totalClaimed(), run.totalSkippedAlreadyClaimed(),
                                    runMessages(run.id()));
                } catch (RuntimeException ex) {
                    launchOutcome = ex.getClass().getSimpleName();
                } finally {
                    launchEnd = System.nanoTime();
                }
            }, "lanzamiento");

            recalculo.start();
            lanzamiento.start();
            unir(recalculo);
            unir(lanzamiento);
        }

    }

    /** Recalculo contra invalidacion: el camino que no toca el cerrojo de la presencia. */
    private final class CarreraConInvalidacion {

        private final String employee;
        private final CyclicBarrier salida = new CyclicBarrier(2);

        private String recalculationOutcome;
        private String invalidationOutcome;
        private long recalculationStart;
        private long recalculationEnd;
        private long invalidationStart;
        private long invalidationEnd;

        private CarreraConInvalidacion(String employee) {
            this.employee = employee;
        }

        private boolean overlapped() {
            return recalculationStart < invalidationEnd && invalidationStart < recalculationEnd;
        }

        private void run() {
            Thread recalculo = new Thread(() -> {
                esperar(salida);
                recalculationStart = System.nanoTime();
                try {
                    recalculatePayrollUseCase.recalculate(new RecalculatePayrollCommand(
                            RULE_SYSTEM, EMPLOYEE_TYPE, employee, PERIOD, PAYROLL_TYPE, 1, "carrera"));
                    recalculationOutcome = "OK";
                } catch (RuntimeException ex) {
                    recalculationOutcome = ex.getClass().getSimpleName();
                } finally {
                    recalculationEnd = System.nanoTime();
                }
            }, "recalculo");

            Thread invalidacion = new Thread(() -> {
                esperar(salida);
                invalidationStart = System.nanoTime();
                try {
                    invalidate(employee);
                    invalidationOutcome = "OK";
                } catch (RuntimeException ex) {
                    invalidationOutcome = ex.getClass().getSimpleName();
                } finally {
                    invalidationEnd = System.nanoTime();
                }
            }, "invalidacion");

            recalculo.start();
            invalidacion.start();
            unir(recalculo);
            unir(invalidacion);
        }
    }

    private static void esperar(CyclicBarrier salida) {
        try {
            salida.await();
        } catch (Exception ex) {
            throw new IllegalStateException("la barrera de salida no dejo salir a los dos hilos", ex);
        }
    }

    private static void unir(Thread hilo) {
        try {
            hilo.join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("el hilo " + hilo.getName() + " no termino", ex);
        }
    }

    /**
     * El invariante: las lineas del recibo son la proyeccion de sus pasos.
     *
     * <p>Un paso llega al folio si tiene {@code payslip_order_code}; el colapso de segmentos suma
     * los pasos de un mismo concepto en una linea, asi que agrupando por concepto las dos sumas
     * tienen que ser la misma. Un recibo con los pasos de un calculo y las lineas de otro rompe
     * esta igualdad en cuanto los dos calculos den numeros distintos, y la rompe en el recuento en
     * cuanto den conceptos distintos.
     */
    private void assertPayrollIsInternallyCoherent(String employee) {
        Long payrollId = jdbcTemplate.queryForObject(
                "select id from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = ?",
                Long.class, RULE_SYSTEM, EMPLOYEE_TYPE, employee, PERIOD, PAYROLL_TYPE, 1);

        List<Map<String, Object>> descuadres = jdbcTemplate.queryForList("""
                select coalesce(p.concept_code, l.concept_code) as concept_code,
                       p.total as pasos,
                       l.total as lineas
                  from (select concept_code, sum(amount) as total
                          from payroll.payroll_calculation_step
                         where payroll_id = ? and payslip_order_code is not null
                         group by concept_code) p
                  full outer join (select concept_code, sum(amount) as total
                                     from payroll.payroll_concept
                                    where payroll_id = ?
                                    group by concept_code) l
                    on l.concept_code = p.concept_code
                 where p.total is distinct from l.total
                """, payrollId, payrollId);

        assertTrue(descuadres.isEmpty(),
                "el recibo " + payrollId + " tiene lineas que no son la proyeccion de sus pasos: " + descuadres);

        Integer steps = jdbcTemplate.queryForObject(
                "select count(*) from payroll.payroll_calculation_step where payroll_id = ?",
                Integer.class, payrollId);
        assertTrue(steps > 0, "el recibo " + payrollId + " se quedo sin pasos");
    }

    /**
     * El otro invariante, y el que el issue llama «ganar en silencio»: el que pierde la carrera se
     * va con una excepcion de negocio con nombre, no con un accidente de persistencia.
     *
     * <p>La diferencia importa porque son dos cosas distintas de cara a quien mira. Un
     * {@code PayrollRecalculationNotAllowedException} dice lo que pasa —el recibo ya esta
     * calculado— y tiene su respuesta en el manejador. Un
     * {@code ObjectOptimisticLockingFailureException} dice «Row was updated or deleted by another
     * transaction … PayrollConceptEntity#21», que no se le puede ensenar a nadie y encima llega
     * disfrazado de fallo del motor.
     */
    private void assertOutcomeIsBusinessLevel(String outcome, List<String> bitacora) {
        assertTrue(outcome != null && (outcome.equals("OK") || outcome.startsWith("Payroll")),
                "el que pierde tiene que perder con una excepcion de negocio, y perdio con "
                        + outcome + "\n" + String.join("\n", bitacora));
    }

    /** El {@code exceptionType} con el que se fue la unidad de una ejecucion, o {@code OK}. */
    private String unitFailureOf(Long runId) {
        List<String> tipos = jdbcTemplate.queryForList(
                "select details_json ->> 'exceptionType' from payroll.calculation_run_message"
                        + " where run_id = ? and message_code = 'UNIT_CALCULATION_ERROR'",
                String.class, runId);
        return tipos.isEmpty() ? "OK" : tipos.get(0);
    }

    private int payrollRowCount(String employee) {
        return jdbcTemplate.queryForObject(
                "select count(*) from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = ?",
                Integer.class, RULE_SYSTEM, EMPLOYEE_TYPE, employee, PERIOD, PAYROLL_TYPE, 1);
    }

    private String runMessages(Long runId) {
        return String.join(",", jdbcTemplate.queryForList(
                "select message_code || ' ' || coalesce(details_json::text, '') || ' ' || message"
                        + "   from payroll.calculation_run_message where run_id = ?",
                String.class, runId));
    }

    private String hire() {
        String employeeNumber = "CC" + (System.nanoTime() % 1_000_000_000L);
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
        assertEquals(1, run.totalCalculated(), "el escenario necesita un recibo de partida");
    }

    private String currentStatus(String employee) {
        return jdbcTemplate.queryForObject(
                "select status from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = ?",
                String.class, RULE_SYSTEM, EMPLOYEE_TYPE, employee, PERIOD, PAYROLL_TYPE, 1);
    }

    /** Deja el recibo calculado, gane quien gane la ronda anterior. */
    private void ensureCalculated(String employee) {
        if (!"CALCULATED".equals(currentStatus(employee))) {
            recalculatePayrollUseCase.recalculate(new RecalculatePayrollCommand(
                    RULE_SYSTEM, EMPLOYEE_TYPE, employee, PERIOD, PAYROLL_TYPE, 1, "reposicion"));
        }
    }

    private void ensureNotValid(String employee) {
        ensureCalculated(employee);
        invalidate(employee);
    }

    private void invalidate(String employee) {
        invalidatePayrollUseCase.invalidate(new InvalidatePayrollCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, employee, PERIOD, PAYROLL_TYPE, 1, "TEST"));
    }
}
