package com.b4rrhh.payroll.scenario;

import com.b4rrhh.employee.payroll_input.application.usecase.CreateEmployeePayrollInputCommand;
import com.b4rrhh.employee.payroll_input.application.usecase.CreateEmployeePayrollInputUseCase;
import com.b4rrhh.employee.payroll_input.application.usecase.UpdateEmployeePayrollInputCommand;
import com.b4rrhh.employee.payroll_input.application.usecase.UpdateEmployeePayrollInputUseCase;
import com.b4rrhh.payroll.application.usecase.BulkFinalizePayrollCommand;
import com.b4rrhh.payroll.application.usecase.BulkFinalizePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.application.usecase.PayrollRetroRequest;
import com.b4rrhh.payroll.retro.application.usecase.ExplainPayrollArrearsCommand;
import com.b4rrhh.payroll.retro.application.usecase.ExplainPayrollArrearsUseCase;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El recibo <b>cuenta</b> el atraso: el literal lleva su origen y la explicación da tres números
 * ({@code backend#134}).
 *
 * <h2>Lo que este test defiende</h2>
 *
 * <p>La condición de este camino, aplicada al paso difícil: <b>el paso no está hecho hasta que el recibo
 * lo cuenta</b>. Un motor que calcula un atraso correcto y lo imprime como una línea más —sin decir de
 * qué mes es y sin poder decir de dónde sale— ha perdido lo único que este producto tiene.
 *
 * <p>Y un atraso no se cuenta con una travesía del grafo, porque <b>no viene de ningún paso</b> de este
 * cálculo. Se cuenta con tres importes: lo que aquel mes vale hoy, lo que por aquel mes se había pagado,
 * y la diferencia.
 */
@TestWebSobreEsquemaReal
class TheReceiptTellsTheArrearWithItsOriginAndItsThreeNumbersTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PAYROLL_TYPE  = "NORMAL";
    private static final String HORAS         = "H01";
    private static final String IMPORTE_HORAS = "102";

    private static final String AGOSTO     = "202508";
    private static final String SEPTIEMBRE = "202509";
    private static final String OCTUBRE    = "202510";

    private static final LocalDate ENERO_1 = LocalDate.of(2025, 1, 1);

    @Autowired private LaunchPayrollCalculationUseCase launch;
    @Autowired private BulkFinalizePayrollUseCase cerrarEnMasa;
    @Autowired private CreateEmployeePayrollInputUseCase createInput;
    @Autowired private UpdateEmployeePayrollInputUseCase updateInput;
    @Autowired private ExplainPayrollArrearsUseCase explicar;
    @Autowired private JdbcTemplate jdbc;

    @PersistenceContext private EntityManager entityManager;

    private PayrollScenarioFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new PayrollScenarioFixtures(jdbc);
    }

    /**
     * El literal es <b>el del concepto, tal cual</b>, y el mes va <b>sólo</b> en la columna de período
     * ({@code backend#138}).
     *
     * <p>El {@code #134} congeló el mes también en el literal —«Horas extraordinarias (atraso
     * 08/2025)»—. Con una columna de período que hace su trabajo era decir lo mismo dos veces, y el
     * literal congelado dejaba de ser el del concepto. El PDF imprime el mes en esa columna sólo en las
     * líneas de otro mes, que es lo que hace que salte a la vista.
     */
    @Test
    void elLiteralDeLaLineaDeAtrasoEsElDelConceptoYElMesVaEnSuColumna() {
        String emp = numeroUnico();
        altaBasica(emp);
        calcularYCerrar(emp, AGOSTO, PayrollRetroRequest.none());

        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, 202508, new BigDecimal("10")));
        calcular(emp, SEPTIEMBRE, new PayrollRetroRequest(null, AGOSTO));

        List<Map<String, Object>> atrasos = lineasDeAtraso(emp, SEPTIEMBRE);
        assertFalse(atrasos.isEmpty(), "septiembre lleva líneas de atraso");

        for (Map<String, Object> linea : atrasos) {
            String literal = (String) linea.get("concept_label");
            assertFalse(literal.contains("atraso"),
                    "el literal de una línea de atraso es el del concepto, sin el mes: " + literal);
            assertEquals(AGOSTO, linea.get("origin_period_code"),
                    "el mes va en su columna, que es su sitio");
        }
        Map<String, Object> laDeLasHorasDeAgosto = atrasos.stream()
                .filter(l -> IMPORTE_HORAS.equals(l.get("concept_code")))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "el atraso de las horas extra tiene que estar: " + atrasos));
        assertEquals("Horas extraordinarias", laDeLasHorasDeAgosto.get("concept_label"),
                "el literal es el del concepto tal cual, el mismo que en un recibo propio");

        // Y no hay dos conceptos: el sufijo va sobre el nombre del concepto de siempre.
        Map<String, Object> laDeLasHoras = atrasos.stream()
                .filter(l -> IMPORTE_HORAS.equals(l.get("concept_code")))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "el atraso de las horas extra tiene que estar: " + atrasos));
        assertEquals(IMPORTE_HORAS, laDeLasHoras.get("concept_code"),
                "el código del concepto es el de siempre: un atraso NO es un concepto nuevo por mes,"
                        + " porque un catálogo con «Salario base de agosto» y «Salario base de"
                        + " septiembre» como conceptos distintos se llenaría de conceptos que no son"
                        + " conceptos, y el grafo tendría una rama por mes de origen");
        assertEquals(0, ((Number) laDeLasHoras.get("merged_step_count")).intValue(),
                "y no viene de ningún paso de este cálculo: el cero es lo que le dice a la pestaña"
                        + " «Cálculo» que no los busque");
    }

    /**
     * La explicación: <b>los tres números y de dónde sale cada uno</b>.
     *
     * <p>Se comprueba sobre el segundo atraso del mismo mes, que es donde los tres números dicen algo que
     * no es obvio: agosto vale hoy el doble, ya se pagó la mitad, y esta línea es la otra mitad.
     */
    @Test
    void laExplicacionDaLosTresNumerosYElDesgloseDeLoPagado() {
        String emp = numeroUnico();
        altaBasica(emp);
        calcularYCerrar(emp, AGOSTO, PayrollRetroRequest.none());

        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, 202508, new BigDecimal("10")));
        calcularYCerrar(emp, SEPTIEMBRE, new PayrollRetroRequest(null, AGOSTO));

        updateInput.update(new UpdateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, 202508, new BigDecimal("20")));
        calcular(emp, OCTUBRE, new PayrollRetroRequest(null, AGOSTO));

        List<ExplainPayrollArrearsUseCase.ArrearExplanation> explicaciones =
                explicar.explain(new ExplainPayrollArrearsCommand(
                        RULE_SYSTEM, EMPLOYEE_TYPE, emp, OCTUBRE, PAYROLL_TYPE, 1));

        assertFalse(explicaciones.isEmpty(), "octubre lleva atrasos que explicar");

        ExplainPayrollArrearsUseCase.ArrearExplanation lasHoras = explicaciones.stream()
                .filter(e -> IMPORTE_HORAS.equals(e.conceptCode()) && AGOSTO.equals(e.originPeriodCode()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "el atraso de las horas de agosto tiene que estar: " + resumen(explicaciones)));

        // Los tres números, y que cuadran con lo que la línea dice.
        assertEquals(0, lasHoras.difference().compareTo(lasHoras.lineAmount()),
                "la diferencia es el importe de la línea: vale hoy " + lasHoras.currentValue()
                        + ", se había pagado " + lasHoras.alreadyPaid()
                        + ", la línea dice " + lasHoras.lineAmount());
        assertTrue(lasHoras.addsUp(), "y la explicación lo dice con un booleano, para que la pantalla"
                + " no tenga que restar");
        assertTrue(lasHoras.currentValue().compareTo(lasHoras.alreadyPaid()) > 0,
                "agosto vale hoy más de lo que se le había pagado, que es por lo que hay atraso");
        assertNotNull(lasHoras.currentValueCalculatedAt(),
                "y se dice cuándo se calculó ese vigente: sin eso, «vale hoy» no tiene fecha");

        // El desglose, que es la mitad que lo hace útil.
        //
        // Y sale UN renglón y no dos, que es lo que este test aprendió: el recibo de agosto no tiene
        // línea de este concepto, porque agosto se cerró con cero horas y un cero no se imprime
        // (backend#104). Así que «el recibo de agosto pagó 0» no aparece como renglón: no hay renglón.
        // El desglose lista los recibos que pagaron algo, y la ausencia de agosto es la respuesta.
        assertEquals(List.of(SEPTIEMBRE),
                lasHoras.paidIn().stream()
                        .map(ExplainPayrollArrearsUseCase.PaidIn::payrollPeriodCode).sorted().toList(),
                "lo pagado se desglosa por el recibo que pagó, y aquí el único que pagó algo de este"
                        + " concepto es el de septiembre. «Por agosto se han pagado 59,40» no explica"
                        + " nada; «los 59,40 los pagó el recibo de septiembre» sí."
                        + " Desglose: " + lasHoras.paidIn());
        assertEquals(0, lasHoras.alreadyPaid().compareTo(
                        lasHoras.paidIn().stream()
                                .map(ExplainPayrollArrearsUseCase.PaidIn::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)),
                "y el desglose suma lo pagado: si no sumara, serían dos números en vez de uno con su"
                        + " detalle");
    }

    /** Un recibo sin atrasos no tiene nada que explicar, y lo dice con la lista vacía. */
    @Test
    void unReciboSinAtrasosDevuelveLaListaVacia() {
        String emp = numeroUnico();
        altaBasica(emp);
        calcular(emp, SEPTIEMBRE, PayrollRetroRequest.none());

        assertEquals(List.of(), explicar.explain(new ExplainPayrollArrearsCommand(
                        RULE_SYSTEM, EMPLOYEE_TYPE, emp, SEPTIEMBRE, PAYROLL_TYPE, 1)),
                "y no una lista con las líneas del propio mes: una línea del propio mes no es un atraso,"
                        + " aunque lleve su período en la misma columna");
    }

    /**
     * El aviso del {@code #132} sale en el recibo <b>con palabras</b>.
     *
     * <p>Como el de la base reguladora, y por el mismo sitio: {@code payroll_warning}, que es lo que la
     * API del recibo sirve y la pantalla pinta. El PDF no imprime avisos —ninguno, tampoco el de la base
     * reguladora— y eso no se cambia aquí: el papel es el modelo oficial, y el modelo oficial no tiene
     * un sitio para un aviso de gestión.
     */
    @Test
    void elAvisoDeLaMarcaFueraDeLimiteSaleEnElReciboConPalabras() {
        String emp = numeroUnico();
        altaBasica(emp);
        calcularYCerrar(emp, "202503", PayrollRetroRequest.none());

        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, 202503, new BigDecimal("10")));
        calcular(emp, SEPTIEMBRE, new PayrollRetroRequest(null, "202506"));

        Map<String, String> avisos = avisos(emp, SEPTIEMBRE);
        assertTrue(avisos.containsKey("RETRO_MARK_OUTSIDE_LIMIT"),
                "el recibo lo dice; avisos: " + avisos.keySet());
        String mensaje = avisos.get("RETRO_MARK_OUTSIDE_LIMIT");
        assertTrue(mensaje.contains("202503"),
                "y nombra el mes de la corrección, que es lo que hay que ir a mirar: " + mensaje);
        assertTrue(mensaje.toLowerCase().contains("no se ha pagado"),
                "con palabras y no con un código: " + mensaje);
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

    private void calcularYCerrar(String emp, String periodo, PayrollRetroRequest retro) {
        calcular(emp, periodo, retro);
        cerrarEnMasa.finalizeBulk(new BulkFinalizePayrollCommand(
                RULE_SYSTEM, periodo, PAYROLL_TYPE,
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, emp),
                        null)));
        entityManager.flush();
        entityManager.clear();
    }

    /** Las líneas que pertenecen a otro mes, que son las de atraso. */
    private List<Map<String, Object>> lineasDeAtraso(String emp, String periodo) {
        return jdbc.queryForList(
                "select c.concept_code, c.concept_label, c.origin_period_code, c.amount,"
                        + " c.merged_step_count, c.payslip_section_code"
                        + " from payroll.payroll_concept c"
                        + " join payroll.payroll p on p.id = c.payroll_id"
                        + " where p.rule_system_code = ? and p.employee_type_code = ?"
                        + "   and p.employee_number = ? and p.payroll_period_code = ?"
                        + "   and c.origin_period_code <> p.payroll_period_code"
                        + " order by c.line_number",
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, periodo);
    }

    private Map<String, String> avisos(String emp, String periodo) {
        return jdbc.queryForList(
                        "select w.warning_code, w.message from payroll.payroll_warning w"
                                + " join payroll.payroll p on p.id = w.payroll_id"
                                + " where p.rule_system_code = ? and p.employee_type_code = ?"
                                + "   and p.employee_number = ? and p.payroll_period_code = ?"
                                + "   and w.warning_code <> 'ELIGIBLE_REAL_EXECUTION'",
                        RULE_SYSTEM, EMPLOYEE_TYPE, emp, periodo)
                .stream().collect(java.util.stream.Collectors.toMap(
                        f -> (String) f.get("warning_code"), f -> (String) f.get("message")));
    }

    private static String resumen(List<ExplainPayrollArrearsUseCase.ArrearExplanation> es) {
        return es.stream()
                .map(e -> e.conceptCode() + "@" + e.originPeriodCode() + "=" + e.lineAmount())
                .toList().toString();
    }

    private String numeroUnico() {
        return "EX" + (System.nanoTime() % 1_000_000_000L);
    }
}
