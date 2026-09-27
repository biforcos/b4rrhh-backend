package com.b4rrhh.payroll.scenario;

import com.b4rrhh.employee.payroll_input.application.usecase.CreateEmployeePayrollInputCommand;
import com.b4rrhh.employee.payroll_input.application.usecase.CreateEmployeePayrollInputUseCase;
import com.b4rrhh.payroll.application.usecase.BulkFinalizePayrollCommand;
import com.b4rrhh.payroll.application.usecase.BulkFinalizePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.InvalidatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.InvalidatePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.application.usecase.PayrollRetroRequest;
import com.b4rrhh.payroll.application.usecase.RecalculatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.RecalculatePayrollUseCase;
import com.b4rrhh.payroll.domain.model.Payroll;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Recalcular un recibo <b>paga su retro</b>, y nunca en silencio ({@code backend#136}).
 *
 * <h2>Lo que este test defiende</h2>
 *
 * <p>La operativa normal es corregir un dato de un empleado y recalcularle <b>desde su recibo</b>, no ir
 * a Operaciones. Hasta aquí el recálculo de un recibo no hacía retro: una marca activa se quedaba sin
 * pagar, el recibo salía igual y nadie lo decía. Un fallo presentado como hecho.
 *
 * <p>Ahora el recálculo planifica y paga la retro del empleado con el <b>límite por defecto</b> —doce
 * meses antes del período, el mismo que propone Operaciones y con el mismo motivo—, lo guarda en su
 * ejecución como cualquier lanzamiento, y lo que no paga lo dice en el recibo.
 */
@TestWebSobreEsquemaReal
class RecalculatingAReceiptPaysItsRetroAndNeverInSilenceTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PAYROLL_TYPE  = "NORMAL";
    private static final String HORAS         = "H01";
    private static final String IMPORTE_HORAS = "102";

    private static final LocalDate ENERO_1 = LocalDate.of(2024, 1, 1);

    @Autowired private LaunchPayrollCalculationUseCase launch;
    @Autowired private BulkFinalizePayrollUseCase cerrarEnMasa;
    @Autowired private InvalidatePayrollUseCase invalidar;
    @Autowired private RecalculatePayrollUseCase recalcular;
    @Autowired private CreateEmployeePayrollInputUseCase createInput;
    @Autowired private JdbcTemplate jdbc;

    @PersistenceContext private EntityManager entityManager;

    private PayrollScenarioFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new PayrollScenarioFixtures(jdbc);
    }

    /**
     * Una marca activa a agosto, y septiembre recalculado desde su recibo: la línea de atraso está.
     *
     * <p>El caso de la demo del 162: horas de agosto corregidas, «Recalcular» en el recibo de septiembre,
     * y el recibo salía igual.
     */
    @Test
    void recalcularDesdeElReciboPagaLaMarcaActiva() {
        String emp = numeroUnico();
        altaBasica(emp);
        calcular(emp, "202508");
        cerrar(emp, "202508");
        calcular(emp, "202509");

        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, 202508, new BigDecimal("10")));

        Payroll recalculado = invalidarYRecalcular(emp, "202509");

        BigDecimal atraso = jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from payroll.payroll_concept c"
                        + " join payroll.payroll p on p.id = c.payroll_id"
                        + " where p.employee_number = ? and p.payroll_period_code = '202509'"
                        + "   and c.concept_code = ? and c.origin_period_code = '202508'",
                BigDecimal.class, emp, IMPORTE_HORAS);
        assertTrue(atraso.compareTo(BigDecimal.ZERO) > 0,
                "el recibo recalculado paga las horas de agosto como atraso; líneas: "
                        + jdbc.queryForList("select c.concept_code, c.origin_period_code, c.amount"
                                + " from payroll.payroll_concept c join payroll.payroll p on p.id = c.payroll_id"
                                + " where p.employee_number = ? and p.payroll_period_code = '202509'", emp));

        // Y la ejecución del recálculo lleva sus parámetros y sus contadores, como un lanzamiento.
        Map<String, Object> run = jdbc.queryForMap(
                "select retro_limit_period_code, retro_floor_period_code, total_retro_units,"
                        + " total_retro_recalculated, total_retro_not_recalculated"
                        + " from payroll.calculation_run where id = ?",
                recalculado.getRunId());
        assertEquals("202409", run.get("retro_limit_period_code"),
                "el límite por defecto: doce meses antes del período, el que propone Operaciones");
        assertEquals(null, run.get("retro_floor_period_code"), "y sin suelo");
        assertEquals(1, ((Number) run.get("total_retro_units")).intValue(), "un mes: agosto");
        assertEquals(1, ((Number) run.get("total_retro_recalculated")).intValue());
        assertEquals(0, ((Number) run.get("total_retro_not_recalculated")).intValue());
    }

    /**
     * Una marca más antigua que el límite por defecto: no se paga, <b>se dice en el recibo</b>, y la
     * marca sigue activa.
     */
    @Test
    void unaMarcaFueraDelLimitePorDefectoSaleComoAvisoYSigueActiva() {
        String emp = numeroUnico();
        altaBasica(emp);
        calcular(emp, "202407");
        cerrar(emp, "202407");
        calcular(emp, "202509");

        // Julio de 2024 está a catorce meses de septiembre de 2025: el límite por defecto no llega.
        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, 202407, new BigDecimal("10")));

        invalidarYRecalcular(emp, "202509");

        List<String> avisos = jdbc.queryForList(
                "select w.message from payroll.payroll_warning w join payroll.payroll p on p.id = w.payroll_id"
                        + " where p.employee_number = ? and p.payroll_period_code = '202509'"
                        + "   and w.warning_code = 'RETRO_MARK_OUTSIDE_LIMIT'",
                String.class, emp);
        assertEquals(1, avisos.size(), "el recibo lo dice, una vez por marca");
        assertTrue(avisos.get(0).contains("202407"), "y nombra el mes: " + avisos.get(0));

        List<String> estados = jdbc.queryForList(
                "select status from payroll.retro_mark where employee_number = ?", String.class, emp);
        assertEquals(List.of("ACTIVE"), estados, "la marca sigue activa: alguien tiene que decidir");

        BigDecimal atraso = jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from payroll.payroll_concept c"
                        + " join payroll.payroll p on p.id = c.payroll_id"
                        + " where p.employee_number = ? and p.payroll_period_code = '202509'"
                        + "   and c.origin_period_code <> p.payroll_period_code",
                BigDecimal.class, emp);
        assertEquals(0, atraso.compareTo(BigDecimal.ZERO), "y no se ha pagado nada de ella");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void altaBasica(String emp) {
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, ENERO_1, null);
        fixtures.insertLaborClassification(empId, ENERO_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), ENERO_1, null);
    }

    private void calcular(String emp, String periodo) {
        var run = launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, periodo, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, emp),
                        null),
                null,
                PayrollRetroRequest.none()));
        assertTrue("COMPLETED".equals(run.status()), "la corrida de " + periodo + ": " + run.status());
        entityManager.flush();
        entityManager.clear();
    }

    private void cerrar(String emp, String periodo) {
        cerrarEnMasa.finalizeBulk(new BulkFinalizePayrollCommand(
                RULE_SYSTEM, periodo, PAYROLL_TYPE,
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, emp),
                        null)));
        entityManager.flush();
        entityManager.clear();
    }

    private Payroll invalidarYRecalcular(String emp, String periodo) {
        invalidar.invalidate(new InvalidatePayrollCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, periodo, PAYROLL_TYPE, 1, "RECALCULO"));
        entityManager.flush();
        entityManager.clear();
        Payroll recalculado = recalcular.recalculate(new RecalculatePayrollCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, periodo, PAYROLL_TYPE, 1, "test"));
        entityManager.flush();
        entityManager.clear();
        assertFalse(recalculado.getRunId() == null, "el recálculo es una ejecución y el recibo lo sabe");
        return recalculado;
    }

    private String numeroUnico() {
        return "RR" + (System.nanoTime() % 1_000_000_000L);
    }
}
