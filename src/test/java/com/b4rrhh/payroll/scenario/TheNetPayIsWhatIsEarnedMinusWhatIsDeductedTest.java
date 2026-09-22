package com.b4rrhh.payroll.scenario;

import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El liquido es lo devengado menos lo deducido, tambien con horas extra ({@code backend#120}).
 *
 * <h2>Lo que afirma</h2>
 *
 * <p>{@code 990 = 970 - 980}. Nada mas, y por eso vale: el liquido de un recibo es su total
 * devengado menos su total a deducir, y un devengo llega al liquido <b>por el {@code 970}</b> y
 * por ningun otro sitio.
 *
 * <h2>Por que con horas extra, y por que eso es la mitad del issue</h2>
 *
 * <p>Porque sin ellas la identidad se cumple <b>con el defecto puesto</b>. El {@code 990} se
 * alimentaba del {@code 102} ademas de del {@code 970}, que ya lo llevaba dentro, asi que el
 * liquido salia {@code 101 + 2 x 102 - 980}: en un recibo sin horas el termino que sobra vale
 * cero y no se nota. 245 de los 863 recibos de la demo pagaban las horas extra dos veces desde
 * la {@code V133}, y ni un test se puso rojo.
 *
 * <p>Por eso los dos casos estan aqui y en este orden. El primero es el que muerde; el segundo
 * es el que ensena por que el catalogo entero paso cinco dias con un liquido mal sin que nadie
 * lo viera: <b>es el caso normal el que no prueba nada</b>.
 *
 * <p>Sobre {@code ESP} y no sobre una reglamentacion de prueba, a proposito: el grafo de las
 * fixtures esta sano —nunca tuvo el {@code 102}— y el defecto vivia en el catalogo real, que es
 * el que calcula la demo. Un escenario montado a mano habria dado verde los dos dias.
 *
 * <p>El candado que impide que el patron vuelva es
 * {@code NoTotalAddsTheSameConceptTwiceTest}: este mide un recibo, aquel mira el catalogo.
 */
@TestWebSobreEsquemaReal
class TheNetPayIsWhatIsEarnedMinusWhatIsDeductedTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PAYROLL_TYPE  = "NORMAL";

    /** Lo que declara la persona: horas. El {@code 102} es lo que se cobra por ellas. */
    private static final String CONCEPTO_DE_ENTRADA = "H01";

    private static final LocalDate JANUARY_1 = LocalDate.of(2025, 1, 1);

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

    /**
     * El caso que muerde: un recibo con horas extra.
     */
    @Test
    void withOvertimeTheNetPayIsStillTotalEarningsMinusTotalDeductions() {
        Long recibo = reciboDe(empleadoConHorasExtra(new BigDecimal("10.0000")));

        BigDecimal horasExtra = importe(recibo, "102");
        assertTrue(horasExtra.signum() > 0,
                "el escenario tiene que traer horas extra cobradas o no prueba nada; el 102 vale "
                        + horasExtra);

        BigDecimal devengado = importe(recibo, "970");
        BigDecimal deducido  = importe(recibo, "980");
        BigDecimal liquido   = importe(recibo, "990");
        assertEquals(0, devengado.subtract(deducido).compareTo(liquido),
                () -> "990 = 970 - 980, y aqui no: liquido=" + liquido + " devengado=" + devengado
                        + " deducido=" + deducido + " -> sobran "
                        + liquido.subtract(devengado.subtract(deducido)) + ", y las horas extra"
                        + " valen " + horasExtra + ". El liquido las esta sumando dos veces: por"
                        + " el 970, que ya las lleva dentro, y otra vez directo");

        // Y el porque: las horas extra estan dentro del total devengado, que es su unico camino
        // al liquido. El 103 vale cero en este recibo —este empleado no prorratea— y por eso el
        // total devengado es exactamente el salario mas las horas.
        assertEquals(0, importe(recibo, "101").add(horasExtra).compareTo(importe(recibo, "970")),
                "el total devengado es el salario mas las horas extra");
    }

    /**
     * Y el caso normal, que es el que no probaba nada.
     *
     * <p>Se cumplia igual con el {@code 102 → 990} puesto, porque sin horas el termino que sobra
     * vale cero. Esta aqui escrito para que la proxima persona que lo lea no escriba solo este:
     * un test verde sobre el caso comodo es lo que dejo pasar el defecto.
     */
    @Test
    void withoutOvertimeItHoldsToo_whichIsExactlyWhyItProvedNothing() {
        Long recibo = reciboDe(empleadoSinHorasExtra());

        assertEquals(0, importe(recibo, "102").signum(),
                "este recibo no tiene horas extra: si las tuviera, el caso ciego no seria ciego");
        assertEquals(0, importe(recibo, "970").subtract(importe(recibo, "980"))
                        .compareTo(importe(recibo, "990")),
                "990 = 970 - 980, que era verdad tambien el dia que el liquido estaba mal");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String empleadoConHorasExtra(BigDecimal horas) {
        String emp = empleadoSinHorasExtra();
        jdbc.update("insert into employee.employee_payroll_input"
                        + " (rule_system_code, employee_type_code, employee_number, concept_code,"
                        + "  period, quantity)"
                        + " values (?, ?, ?, ?, ?, ?)",
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, CONCEPTO_DE_ENTRADA, Integer.parseInt(PERIOD), horas);
        return emp;
    }

    /**
     * Un alta corriente, sin prorrateo: asi la prorrata entra por el recuadro de bases
     * ({@code B02}) y no por los devengos, y el total devengado se lee sin ruido (ADR-070).
     */
    private String empleadoSinHorasExtra() {
        String emp = "NP" + (System.nanoTime() % 1_000_000_000L);
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, JANUARY_1, null);
        fixtures.insertLaborClassification(empId, JANUARY_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), JANUARY_1, null);
        fixtures.insertExtraPaymentRegime(empId, false, JANUARY_1, null);
        return emp;
    }

    private Long reciboDe(String employeeNumber) {
        CalculationRun run = launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employeeNumber),
                        null),
                null));
        assertEquals("COMPLETED", run.status(), () -> "la corrida no salio limpia: "
                + jdbc.queryForList(
                        "select message_code, severity_code, message from payroll.calculation_run_message"
                                + " where run_id = ?", run.id()));
        entityManager.flush();

        return jdbc.queryForObject(
                "select id from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and status = 'CALCULATED'",
                Long.class, RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE);
    }

    /** El importe de periodo de un concepto, sumando sus tramos si los tiene. */
    private BigDecimal importe(Long payrollId, String conceptCode) {
        return jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, conceptCode);
    }
}
