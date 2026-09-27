package com.b4rrhh.payroll.scenario;

import com.b4rrhh.employee.payroll_input.application.usecase.CreateEmployeePayrollInputCommand;
import com.b4rrhh.employee.payroll_input.application.usecase.CreateEmployeePayrollInputUseCase;
import com.b4rrhh.payroll.application.usecase.BulkFinalizePayrollCommand;
import com.b4rrhh.payroll.application.usecase.BulkFinalizePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.application.usecase.PayrollRetroRequest;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Una línea de atraso <b>no lleva cantidad ni tarifa</b> ({@code backend#135}).
 *
 * <h2>Lo que este test defiende</h2>
 *
 * <p>En una línea de atraso el importe es {@code vigente − pagado}. Si además se copian la cantidad y la
 * tarifa del vigente, la fila dice {@code 1.680,00 × 0,10 = 0,08}: tres números correctos por separado
 * que <b>no multiplican</b>. Y un recibo se lee línea a línea, así que una fila que no cuadra miente a
 * quien la lea como a las demás — que es como se leen todas.
 *
 * <p>Lo que explica una línea de atraso son los tres números del {@code backend#134} —lo que aquel mes
 * vale hoy, lo que por aquel mes se ha pagado, y la diferencia—, y ésos ya están en la explicación.
 * Poner en la fila dos números que no multiplican es peor que no ponerlos.
 *
 * <p>La regla es una y no admite el caso bueno: aunque la diferencia sí fuera cantidad × tarifa —una
 * tarifa que no cambió y una cantidad que sí—, la fila sigue sin llevarlas. Un recibo en el que unas
 * filas de atraso multiplican y otras no obliga al que lo lee a saber cuál es cuál.
 */
@TestWebSobreEsquemaReal
class AnArrearLineCarriesNeitherQuantityNorRateTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PAYROLL_TYPE  = "NORMAL";
    private static final String HORAS         = "H01";

    private static final String AGOSTO     = "202508";
    private static final String SEPTIEMBRE = "202509";

    private static final LocalDate ENERO_1 = LocalDate.of(2025, 1, 1);

    @Autowired private LaunchPayrollCalculationUseCase launch;
    @Autowired private BulkFinalizePayrollUseCase cerrarEnMasa;
    @Autowired private CreateEmployeePayrollInputUseCase createInput;
    @Autowired private JdbcTemplate jdbc;

    @PersistenceContext private EntityManager entityManager;

    private PayrollScenarioFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new PayrollScenarioFixtures(jdbc);
    }

    /**
     * Ninguna línea con origen distinto del período del recibo lleva cantidad ni tarifa.
     *
     * <p>Se comprueba sobre <b>todas</b> las líneas de atraso del recibo y no sobre una elegida: el que
     * se veía a simple vista era un porcentaje sobre una base, pero la regla vale para las cuatro clases
     * de línea que bajan con origen (devengo, base, cuota y aportación empresarial).
     */
    @Test
    void ningunaLineaDeAtrasoLlevaCantidadNiTarifa() {
        String emp = numeroUnico();
        altaBasica(emp);
        calcularYCerrar(emp, AGOSTO);

        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, 202508, new BigDecimal("10")));
        calcular(emp, SEPTIEMBRE, new PayrollRetroRequest(null, AGOSTO));

        List<Map<String, Object>> atrasos = lineas(emp, SEPTIEMBRE, true);
        assertFalse(atrasos.isEmpty(), "septiembre tiene que llevar líneas de atraso para que este"
                + " test pruebe algo");

        for (Map<String, Object> linea : atrasos) {
            assertNull(linea.get("quantity"),
                    "una línea de atraso no lleva cantidad: su importe es vigente − pagado, y la"
                            + " cantidad del vigente no lo multiplica. Línea: " + linea);
            assertNull(linea.get("rate"),
                    "y tampoco tarifa, por lo mismo. Línea: " + linea);
        }
    }

    /**
     * Y las líneas normales del mismo recibo <b>siguen llevándolas</b>.
     *
     * <p>La mitad que hace que la regla sea una regla y no un borrado: lo que se quita es la cantidad y
     * la tarifa <b>de las filas que no multiplican</b>. Si se fueran también las de las demás, el recibo
     * dejaría de poder decir «diez horas a 12,50», que es lo que se estaba protegiendo.
     */
    @Test
    void lasLineasDelPropioMesConservanCantidadYTarifa() {
        String emp = numeroUnico();
        altaBasica(emp);
        calcularYCerrar(emp, AGOSTO);

        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, 202508, new BigDecimal("10")));
        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, 202509, new BigDecimal("10")));
        calcular(emp, SEPTIEMBRE, new PayrollRetroRequest(null, AGOSTO));

        List<Map<String, Object>> propias = lineas(emp, SEPTIEMBRE, false);
        assertTrue(propias.stream().anyMatch(l -> l.get("quantity") != null && l.get("rate") != null),
                "alguna línea del propio mes sigue diciendo cantidad y tarifa: " + propias);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void altaBasica(String emp) {
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, ENERO_1, null);
        fixtures.insertLaborClassification(empId, ENERO_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), ENERO_1, null);
    }

    private void calcular(String emp, String periodo, PayrollRetroRequest retro) {
        var run = launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, periodo, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, emp),
                        null),
                null,
                retro));
        assertTrue("COMPLETED".equals(run.status()),
                "la corrida de " + periodo + ": " + run.status() + " | mensajes: "
                        + jdbc.queryForList("select message_code, message from"
                                + " payroll.calculation_run_message where run_id = ?", run.id()));
        entityManager.flush();
        entityManager.clear();
    }

    private void calcularYCerrar(String emp, String periodo) {
        calcular(emp, periodo, PayrollRetroRequest.none());
        cerrarEnMasa.finalizeBulk(new BulkFinalizePayrollCommand(
                RULE_SYSTEM, periodo, PAYROLL_TYPE,
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, emp),
                        null)));
        entityManager.flush();
        entityManager.clear();
    }

    /** Las líneas del recibo, las de otro mes ({@code deAtraso}) o las del propio. */
    private List<Map<String, Object>> lineas(String emp, String periodo, boolean deAtraso) {
        return jdbc.queryForList(
                "select c.line_number, c.concept_code, c.concept_label, c.origin_period_code,"
                        + " c.amount, c.quantity, c.rate"
                        + " from payroll.payroll_concept c"
                        + " join payroll.payroll p on p.id = c.payroll_id"
                        + " where p.rule_system_code = ? and p.employee_type_code = ?"
                        + "   and p.employee_number = ? and p.payroll_period_code = ?"
                        + "   and c.origin_period_code " + (deAtraso ? "<>" : "=") + " p.payroll_period_code"
                        + " order by c.line_number",
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, periodo);
    }

    private String numeroUnico() {
        return "AQ" + (System.nanoTime() % 1_000_000_000L);
    }
}
