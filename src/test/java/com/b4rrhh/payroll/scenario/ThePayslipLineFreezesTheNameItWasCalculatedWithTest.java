package com.b4rrhh.payroll.scenario;

import com.b4rrhh.payroll.application.usecase.InvalidatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.InvalidatePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.application.usecase.RecalculatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.RecalculatePayrollUseCase;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * El literal de una linea se congela al calcular, y el mnemonico se queda ({@code backend#109}).
 *
 * <h2>Que distingue un documento de una vista</h2>
 *
 * <p>La salida facil habria sido resolver el literal al leer: la linea guarda el codigo y la
 * pantalla pregunta al catalogo como se llama. Parece mas limpio y rompe la promesa del recibo. En
 * cuanto exista el PDF, un recibo tiene un gemelo fisico fuera del sistema: el papel que tiene el
 * empleado dice «Salario base». Si alguien renombra el concepto y la pantalla cambia, la pantalla
 * esta mintiendo sobre lo que se entrego.
 *
 * <p>Por eso el test que importa no es «el recibo ensena Salario base»: es el de los <b>dos
 * lados</b>. Se cambia el nombre en el catalogo y se mira un recibo ya calculado —no se mueve— y
 * uno recalculado —si coge el nuevo—. <b>Si las dos salidas se parecieran, esto no estaria
 * hecho</b>: seria igual de verde con el literal resuelto al leer, que es justo la solucion que se
 * descarto.
 */
@TestWebSobreEsquemaReal
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ThePayslipLineFreezesTheNameItWasCalculatedWithTest {

    private static final String RULE_SYSTEM = "LBL";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD = "202501";
    private static final String PAYROLL_TYPE = "NORMAL";
    private static final LocalDate PERIOD_START = LocalDate.of(2025, 1, 1);

    @Autowired
    private LaunchPayrollCalculationUseCase launchPayrollCalculationUseCase;

    @Autowired
    private InvalidatePayrollUseCase invalidatePayrollUseCase;

    @Autowired
    private RecalculatePayrollUseCase recalculatePayrollUseCase;

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
        // Cada test parte del catalogo con el nombre puesto.
        fixtures.setConceptLabel(RULE_SYSTEM, "101", "Salario base");
    }

    /**
     * Criterio 1: donde decia {@code SALARIO_BASE} dice {@code Salario base}, y el mnemonico sigue
     * en la linea.
     */
    @Test
    void thePayslipShowsTheNameAndKeepsTheMnemonic() {
        String employee = hire();
        launch(employee);

        assertEquals("Salario base", label(employee, "101"));
        assertEquals("SALARIO_BASE", mnemonic(employee, "101"));
    }

    /**
     * Criterios 2 y 3, provocado por los dos lados.
     *
     * <p>El mismo cambio de catalogo, mirado en dos recibos: el que ya estaba calculado y el que
     * se recalcula despues. La ultima afirmacion es la que da valor a las dos primeras.
     */
    @Test
    void renamingTheConceptLeavesTheCalculatedPayslipAloneAndReachesTheRecalculatedOne() {
        String yaCalculado = hire();
        launch(yaCalculado);
        String seRecalcula = hire();
        launch(seRecalcula);

        assertEquals("Salario base", label(yaCalculado, "101"));

        // El catalogo cambia. Nada mas.
        fixtures.setConceptLabel(RULE_SYSTEM, "101", "Sueldo base de convenio");

        // Lado uno: el recibo ya calculado no se mueve. Es un documento y dice lo que dijo.
        assertEquals("Salario base", label(yaCalculado, "101"),
                "un recibo ya calculado no puede cambiar porque alguien toque el catalogo");

        // Lado dos: el que se recalcula si coge el nuevo, porque todavia no se ha entregado nada.
        recalculate(seRecalcula);
        assertEquals("Sueldo base de convenio", label(seRecalcula, "101"),
                "un recibo recalculado coge el literal de hoy");

        // Y lo que hace que los dos anteriores signifiquen algo: son distintos. Con el literal
        // resuelto al leer, los dos dirian «Sueldo base de convenio» y este test seguiria verde.
        assertNotEquals(label(yaCalculado, "101"), label(seRecalcula, "101"),
                "si las dos salidas se parecen, el literal no se esta congelando");

        // Y el mnemonico no se ha movido en ninguno de los dos: es el identificador, y renombrar
        // no es reidentificar.
        assertEquals("SALARIO_BASE", mnemonic(yaCalculado, "101"));
        assertEquals("SALARIO_BASE", mnemonic(seRecalcula, "101"));
    }

    /**
     * Criterio 4: el concepto al que se le olvido el nombre.
     *
     * <p>Ni rompe el calculo ni deja un hueco en el folio: la linea ensena el mnemonico. Es el caso
     * que va a existir el dia que alguien anada un concepto, y una ausencia visible es mejor que
     * una invisible.
     */
    @Test
    void aConceptWithNoNameShowsItsMnemonicAndBreaksNothing() {
        fixtures.removeConceptLabel(RULE_SYSTEM, "101");

        String employee = hire();
        launch(employee);

        assertEquals("SALARIO_BASE", label(employee, "101"),
                "sin literal, la linea ensena la clave y se nota que falta");
        assertEquals("SALARIO_BASE", mnemonic(employee, "101"));

        // Y el resto del recibo esta entero: faltar un nombre no es faltar un calculo.
        assertEquals(7, jdbcTemplate.queryForObject("""
                select count(*)
                  from payroll.payroll_concept c
                  join payroll.payroll p on p.id = c.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ?
                """, Integer.class, RULE_SYSTEM, employee));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String label(String employee, String conceptCode) {
        return jdbcTemplate.queryForObject("""
                select c.concept_label
                  from payroll.payroll_concept c
                  join payroll.payroll p on p.id = c.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ? and c.concept_code = ?
                """, String.class, RULE_SYSTEM, employee, conceptCode);
    }

    private String mnemonic(String employee, String conceptCode) {
        return jdbcTemplate.queryForObject("""
                select c.concept_mnemonic
                  from payroll.payroll_concept c
                  join payroll.payroll p on p.id = c.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ? and c.concept_code = ?
                """, String.class, RULE_SYSTEM, employee, conceptCode);
    }

    private String hire() {
        String employeeNumber = "LB" + (System.nanoTime() % 1_000_000_000L);
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
        assertEquals(1, run.totalCalculated(), "el escenario necesita un recibo calculado");
    }

    private void recalculate(String employee) {
        invalidatePayrollUseCase.invalidate(new InvalidatePayrollCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, employee, PERIOD, PAYROLL_TYPE, 1, "TEST"));
        recalculatePayrollUseCase.recalculate(new RecalculatePayrollCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, employee, PERIOD, PAYROLL_TYPE, 1, "test"));
    }
}
