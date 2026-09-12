package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El barrido de arranque, sobre el esquema de verdad.
 *
 * <p>Con mocks no se prueba lo que importa aqui: que Postgres admite cerrar una ejecucion
 * que nunca arranco —la V55 prohibe una fecha de fin sin fecha de inicio— y que el borrado
 * en bloque de reservas se ejecuta de verdad. Las filas se montan como las deja un backend
 * que muere a mitad: la ejecucion en RUNNING y su reserva puesta (#75).
 */
@TestWebSobreEsquemaReal
class RecoverAbandonedPayrollCalculationRunsIntegrationTest {

    @Autowired
    private RecoverAbandonedPayrollCalculationRunsUseCase recover;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * Hace falta para leer con SQL lo que escribio JPA: el test corre en una transaccion y
     * el UPDATE no llega a Postgres hasta que se vacia la sesion. Y vaciarla es justo lo
     * que pone a prueba la restriccion del esquema, que es el motivo de este test.
     */
    @Autowired
    private EntityManager entityManager;

    @Test
    void aRunLeftCalculatingIsClosedAsFailedAndItsClaimReleased() {
        Long runId = insertRun("RUNNING", true);
        insertClaim(runId, "RC1");

        RecoveredPayrollLaunchState recovered = recover.recover();
        entityManager.flush();

        assertTrue(recovered.closedRuns() >= 1);
        assertEquals("FAILED", status(runId));
        assertNotNull(finishedAt(runId));
        assertEquals(0, claimCount(runId));
        assertEquals(1, messageCount(runId, "RUN_ABANDONED_ON_RESTART"));
    }

    @Test
    void aRunLeftWaitingInTheQueueIsClosedToo() {
        Long runId = insertRun("REQUESTED", false);

        recover.recover();
        entityManager.flush();

        assertEquals("FAILED", status(runId));
        // Cerrada aunque no tuviera inicio: el barrido le pone el instante en que se pidio,
        // porque el esquema no admite fin sin inicio.
        assertNotNull(jdbc.queryForObject(
                "select started_at from payroll.calculation_run where id = ?", java.sql.Timestamp.class, runId));
        assertNotNull(finishedAt(runId));
    }

    @Test
    void aRunThatFinishedIsLeftAlone() {
        Long runId = insertRun("COMPLETED", true);
        jdbc.update("update payroll.calculation_run set finished_at = now() where id = ?", runId);

        recover.recover();
        entityManager.flush();

        assertEquals("COMPLETED", status(runId));
        assertEquals(0, messageCount(runId, "RUN_ABANDONED_ON_RESTART"));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Long insertRun(String status, boolean started) {
        return jdbc.queryForObject(
                """
                insert into payroll.calculation_run (
                    rule_system_code, payroll_period_code, payroll_type_code,
                    calculation_engine_code, calculation_engine_version,
                    requested_at, requested_by, status, target_selection_json, started_at
                ) values (?, ?, ?, ?, ?, now(), ?, ?, ?::json, case when ? then now() else null end)
                returning id
                """,
                Long.class,
                "TST", "202501", "NORMAL", "ENGINE", "1.0",
                "hr.manager@b4rrhh", status,
                "{\"selectionType\":\"ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD\"}",
                started
        );
    }

    private void insertClaim(Long runId, String employeeNumber) {
        jdbc.update(
                """
                insert into payroll.calculation_claim (
                    run_id, rule_system_code, employee_type_code, employee_number,
                    payroll_period_code, payroll_type_code, presence_number, claimed_at
                ) values (?, 'TST', 'INTERNAL', ?, '202501', 'NORMAL', 1, now())
                """,
                runId, employeeNumber
        );
    }

    private String status(Long runId) {
        return jdbc.queryForObject("select status from payroll.calculation_run where id = ?", String.class, runId);
    }

    private java.sql.Timestamp finishedAt(Long runId) {
        return jdbc.queryForObject(
                "select finished_at from payroll.calculation_run where id = ?", java.sql.Timestamp.class, runId);
    }

    private int claimCount(Long runId) {
        return jdbc.queryForObject(
                "select count(*) from payroll.calculation_claim where run_id = ?", Integer.class, runId);
    }

    private int messageCount(Long runId, String messageCode) {
        return jdbc.queryForObject(
                "select count(*) from payroll.calculation_run_message where run_id = ? and message_code = ?",
                Integer.class, runId, messageCode);
    }
}
