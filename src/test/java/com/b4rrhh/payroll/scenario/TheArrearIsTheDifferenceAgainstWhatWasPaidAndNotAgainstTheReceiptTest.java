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
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.retro.application.usecase.ListEmployeeRetroMarksCommand;
import com.b4rrhh.payroll.retro.application.usecase.ListEmployeeRetroMarksUseCase;
import com.b4rrhh.payroll.retro.domain.model.RetroMark;
import com.b4rrhh.payroll.retro.domain.model.RetroMarkStatus;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>Un atraso es la diferencia contra lo que se ha pagado, no contra el recibo</b>
 * ({@code backend#133}).
 *
 * <h2>El caso que decide el paso</h2>
 *
 * <table>
 *   <tr><th></th><th>qué pasa</th><th>qué se cobra</th></tr>
 *   <tr><td>ago</td><td>cerrado con 0 horas</td><td>recibo A, congelado, entregado</td></tr>
 *   <tr><td>sep</td><td>«agosto tenía 10»</td><td>recibo B + atraso {@code origen=ago} por <b>10</b></td></tr>
 *   <tr><td>oct</td><td>«no, eran 20»</td><td>recibo C + atraso {@code origen=ago} por <b>+10</b>, no por 20</td></tr>
 * </table>
 *
 * <p>Y la razón por la que este test existe: <b>los dos diseños posibles dan lo mismo en septiembre</b>.
 * Calcular el atraso contra el recibo cerrado de agosto y calcularlo contra lo que se ha pagado por
 * agosto dan los dos diez. La diferencia sale en octubre: contra el recibo daría veinte —y el empleado
 * cobraría treinta por unas horas que valen veinte— y contra lo pagado da diez.
 *
 * <p>Un caso donde dos cosas coinciden no distingue si se han confundido. Éste es el caso donde
 * dejan de coincidir.
 */
@TestWebSobreEsquemaReal
class TheArrearIsTheDifferenceAgainstWhatWasPaidAndNotAgainstTheReceiptTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PAYROLL_TYPE  = "NORMAL";

    /** El único concepto de entrada del catálogo (V133): la cantidad de horas extra. */
    private static final String HORAS = "H01";
    /** Y el devengo que sale de ellas. */
    private static final String IMPORTE_HORAS = "102";

    private static final String AGOSTO      = "202508";
    private static final String SEPTIEMBRE  = "202509";
    private static final String OCTUBRE     = "202510";

    private static final LocalDate ENERO_1 = LocalDate.of(2025, 1, 1);

    @Autowired private LaunchPayrollCalculationUseCase launch;
    @Autowired private BulkFinalizePayrollUseCase cerrarEnMasa;
    @Autowired private CreateEmployeePayrollInputUseCase createInput;
    @Autowired private UpdateEmployeePayrollInputUseCase updateInput;
    @Autowired private ListEmployeeRetroMarksUseCase listMarks;
    @Autowired private JdbcTemplate jdbc;

    @PersistenceContext private EntityManager entityManager;

    private PayrollScenarioFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new PayrollScenarioFixtures(jdbc);
    }

    @Test
    void elSegundoAtrasoDelMismoMesPagaLaDiferenciaYNoElTotalOtraVez() {
        String emp = numeroUnico();
        altaBasica(emp);

        // ── agosto: cerrado con cero horas ───────────────────────────────────
        calcular(emp, AGOSTO, PayrollRetroRequest.none());
        cerrar(emp, AGOSTO);
        Long agostoId = reciboId(emp, AGOSTO);
        BigDecimal agostoOriginal = importe(agostoId, IMPORTE_HORAS);
        assertEquals(0, agostoOriginal.compareTo(BigDecimal.ZERO),
                "agosto se cerró sin horas extra");
        String huellaDeAgosto = huella(agostoId);

        // ── septiembre: «agosto tenía 10» ────────────────────────────────────
        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, 202508, new BigDecimal("10")));
        assertEquals(1, marcas(emp).size(), "la corrección a agosto ha dejado su marca");

        CalculationRun runSep = calcular(emp, SEPTIEMBRE, new PayrollRetroRequest(null, AGOSTO));
        assertEquals(1, (int) runSep.totalRetroUnits(), "un mes de tramo: agosto");
        assertEquals(1, (int) runSep.totalRetroRecalculated());

        Long septiembreId = reciboId(emp, SEPTIEMBRE);
        BigDecimal atrasoEnSeptiembre = importeConOrigen(septiembreId, IMPORTE_HORAS, AGOSTO);
        assertTrue(atrasoEnSeptiembre.compareTo(BigDecimal.ZERO) > 0,
                "septiembre paga las diez horas de agosto como atraso; líneas de atraso: "
                        + atrasos(septiembreId));
        BigDecimal valorDeDiezHoras = atrasoEnSeptiembre;

        // Y el recibo de agosto no se ha tocado.
        assertEquals(huellaDeAgosto, huella(agostoId),
                "el recibo de agosto sigue siendo el documento que se entregó");
        assertEquals(0, importe(agostoId, IMPORTE_HORAS).compareTo(BigDecimal.ZERO));

        // Los totales de septiembre incluyen el atraso: es dinero de este mes.
        assertTrue(importe(septiembreId, "970").compareTo(atrasoEnSeptiembre) > 0,
                "el total de devengos de septiembre incluye el atraso: " + importe(septiembreId, "970")
                        + " frente a un atraso de " + atrasoEnSeptiembre);
        assertEquals(0,
                importe(septiembreId, "990").compareTo(
                        importe(septiembreId, "970").subtract(importe(septiembreId, "980"))),
                "y el líquido sigue siendo devengos menos deducciones, con el atraso dentro");

        // No hay línea 800 con origen: el IRPF no viaja (ADR-070 §4).
        assertEquals(0, importeConOrigen(septiembreId, "800", AGOSTO).compareTo(BigDecimal.ZERO),
                "el IRPF no viaja: la retención es sobre lo que se paga cuando se paga, y el 800 de"
                        + " septiembre absorbe los devengos atrasados");

        cerrar(emp, SEPTIEMBRE);
        assertEquals(RetroMarkStatus.CONSUMED, marcas(emp).get(0).getStatus(),
                "cerrar septiembre consume la marca, y la fila dice qué recibo la pagó");
        assertEquals(SEPTIEMBRE, marcas(emp).get(0).getConsumedPeriodCode());

        // ── octubre: «no, eran 20» ───────────────────────────────────────────
        updateInput.update(new UpdateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, 202508, new BigDecimal("20")));
        List<RetroMark> trasLaSegunda = marcas(emp);
        assertEquals(2, trasLaSegunda.size(),
                "la segunda corrección es otra marca, no la primera cambiada: " + resumen(trasLaSegunda));

        CalculationRun runOct = calcular(emp, OCTUBRE, new PayrollRetroRequest(null, AGOSTO));
        assertEquals(2, (int) runOct.totalRetroUnits(),
                "el tramo va desde agosto hasta septiembre: dos meses");

        Long octubreId = reciboId(emp, OCTUBRE);
        BigDecimal atrasoEnOctubre = importeConOrigen(octubreId, IMPORTE_HORAS, AGOSTO);

        assertEquals(0, atrasoEnOctubre.compareTo(valorDeDiezHoras),
                "ESTE es el caso que decide el paso: octubre paga OTRAS diez horas, no veinte."
                        + " El atraso se calcula contra lo que ya se ha pagado por agosto -su recibo"
                        + " (cero) más el atraso de septiembre (diez)- y no contra su recibo cerrado."
                        + " Contra el recibo saldrían veinte y el empleado cobraría treinta por unas"
                        + " horas que valen veinte. Pagado en octubre: " + atrasoEnOctubre
                        + ", valor de diez horas: " + valorDeDiezHoras
                        + ", líneas de atraso de octubre: " + atrasos(octubreId));

        // Y los dos recibos anteriores siguen intactos.
        assertEquals(huellaDeAgosto, huella(agostoId), "agosto sigue sin tocarse, dos meses después");
    }

    /**
     * La invariante, que es lo que hace esto seguro.
     *
     * <blockquote>
     * <b>Lo cobrado por un período es igual a su último cálculo.</b><br>
     * {@code recibo(M) + Σ atrasos(M) == vigente(M)}, por concepto, <b>excluidos los conceptos del mes
     * que paga</b>.
     * </blockquote>
     *
     * <h2>Qué queda fuera, y por qué son ocho y no uno</h2>
     *
     * <p>El issue decía «excluido el 800», y montando la invariante salieron siete más — por exactamente
     * la misma razón, que es lo que hace que la lista sea una y no dos:
     *
     * <ul>
     *   <li><b>{@code 800}</b>, la retención de IRPF: es sobre lo que se paga cuando se paga (ADR-070
     *       §4).</li>
     *   <li><b>{@code 970}, {@code 980}, {@code 990}, {@code 725}</b>, los totales: son sumas del mes que
     *       paga. El vigente de agosto dice que agosto vale 1.543,80 de bruto, y por agosto se han pagado
     *       1.543,80 — pero repartidos entre el {@code 970} de agosto (1.425) y el {@code 970} de
     *       septiembre (que lleva los 118,80 del atraso). El dinero cuadra; el <i>concepto total</i> no
     *       puede cuadrar, porque no se atribuye a un mes.</li>
     *   <li><b>{@code A_DEV}, {@code A_DED}, {@code A_EMP}</b>, los tres técnicos que meten los atrasos
     *       en esos totales: un atraso de un atraso no existe.</li>
     * </ul>
     *
     * <p>Y es <b>la misma lista</b> que {@code RetroDeltaCalculator.NO_VIAJAN}, lo cual no es una
     * coincidencia y es la propiedad que hace segura la invariante: <b>lo que no viaja es exactamente lo
     * que la invariante no puede comparar</b>. Si alguien añade un concepto a una de las dos listas y no
     * a la otra, este test se pone rojo.
     *
     * <p>No es un párrafo: es una consulta, y se puede pegar contra cualquier base tal cual está abajo.
     * Aquí se ejecuta sobre el caso de arriba, que es el que la puede romper.
     */
    @Test
    void loCobradoPorUnPeriodoEsIgualASuUltimoCalculo() {
        String emp = numeroUnico();
        altaBasica(emp);

        calcular(emp, AGOSTO, PayrollRetroRequest.none());
        cerrar(emp, AGOSTO);

        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, 202508, new BigDecimal("10")));
        calcular(emp, SEPTIEMBRE, new PayrollRetroRequest(null, AGOSTO));
        cerrar(emp, SEPTIEMBRE);

        updateInput.update(new UpdateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, 202508, new BigDecimal("20")));
        calcular(emp, OCTUBRE, new PayrollRetroRequest(null, AGOSTO));
        cerrar(emp, OCTUBRE);

        List<Map<String, Object>> descuadres = jdbc.queryForList(INVARIANTE, emp);
        assertEquals(List.of(), descuadres,
                "Lo cobrado por un período tiene que ser igual a su último cálculo, por concepto."
                        + " Cada fila de aquí es un concepto de un mes por el que se ha pagado algo"
                        + " distinto de lo que ese mes vale hoy, y eso es un atraso mal calculado o uno"
                        + " que falta. Descuadres: " + descuadres);
    }

    /**
     * Invalidar el período <b>no devuelve</b> las marcas, porque nunca se consumieron.
     *
     * <p>Se consumen al <b>cerrar</b> y no al calcular, y eso es lo que hace que una nómina de prueba no
     * se lleve por delante un atraso: si se consumieran al calcular, el recibo definitivo saldría sin él
     * y nada lo diría.
     */
    @Test
    void unaNominaDePruebaNoConsumeNingunaMarca() {
        String emp = numeroUnico();
        altaBasica(emp);
        calcular(emp, AGOSTO, PayrollRetroRequest.none());
        cerrar(emp, AGOSTO);

        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, HORAS, 202508, new BigDecimal("10")));

        // Se calcula septiembre y NO se cierra.
        calcular(emp, SEPTIEMBRE, new PayrollRetroRequest(null, AGOSTO));

        assertEquals(RetroMarkStatus.ACTIVE, marcas(emp).get(0).getStatus(),
                "calcular no paga nada: un recibo CALCULATED se vuelve a calcular entero y no ha salido"
                        + " del sistema, así que la marca sigue pendiente");
    }

    /**
     * La consulta de la invariante, tal y como se pega contra una base.
     *
     * <p>Se compara <b>por empleado y por mes</b>, sumando las presencias de los dos lados: «lo cobrado
     * por agosto» es dinero del empleado, y que la línea de atraso acabe en el recibo de una presencia o
     * de otra es un detalle documental (ADR-074 §3).
     *
     * <p>Y sólo de los meses que <b>tienen vigente</b>: un mes que nunca se recalculó no tiene con qué
     * compararse, y su recibo es la verdad.
     */
    private static final String INVARIANTE = """
            with vigente as (
                select v.employee_number, v.payroll_period_code, c.concept_code,
                       sum(c.amount) as importe
                  from payroll.current_calculation v
                  join payroll.current_calculation_concept c
                    on c.current_calculation_id = v.id
                 group by 1, 2, 3
            ),
            pagado as (
                select p.employee_number, c.origin_period_code as payroll_period_code, c.concept_code,
                       sum(c.amount) as importe
                  from payroll.payroll p
                  join payroll.payroll_concept c on c.payroll_id = p.id
                 where p.status = 'DEFINITIVE'
                 group by 1, 2, 3
            ),
            meses_con_vigente as (
                select distinct employee_number, payroll_period_code from vigente
            )
            select m.employee_number, m.payroll_period_code,
                   coalesce(v.concept_code, g.concept_code) as concept_code,
                   coalesce(v.importe, 0) as vale_hoy,
                   coalesce(g.importe, 0) as se_ha_pagado
              from meses_con_vigente m
              left join vigente v
                on v.employee_number = m.employee_number
               and v.payroll_period_code = m.payroll_period_code
              full outer join pagado g
                on g.employee_number = m.employee_number
               and g.payroll_period_code = m.payroll_period_code
               and g.concept_code = v.concept_code
             where m.employee_number = ?
               -- Los conceptos del mes que PAGA, que no se atribuyen a un mes y por eso no viajan:
               -- el IRPF, los cuatro totales y los tres tecnicos de los atrasos. Es la misma lista
               -- que RetroDeltaCalculator.NO_VIAJAN, y que sea la misma es la propiedad.
               and coalesce(v.concept_code, g.concept_code) not in
                   ('800', '970', '980', '990', '725', 'A_DEV', 'A_DED', 'A_EMP')
               and coalesce(v.importe, 0) <> coalesce(g.importe, 0)
             order by 2, 3
            """;

    // ── helpers ──────────────────────────────────────────────────────────────

    private void altaBasica(String emp) {
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, ENERO_1, null);
        fixtures.insertLaborClassification(empId, ENERO_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), ENERO_1, null);
    }

    private CalculationRun calcular(String emp, String periodo, PayrollRetroRequest retro) {
        CalculationRun run = launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, periodo, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, emp),
                        null),
                null,
                retro));
        assertTrue("COMPLETED".equals(run.status()),
                "la corrida de " + periodo + " termina bien: " + run.status()
                        // Los mensajes de la corrida en el propio fallo: sin ellos, un
                        // COMPLETED_WITH_ERRORS obliga a ir a buscar la causa a la base, y el test
                        // ya la tiene delante.
                        + " | mensajes: " + jdbc.queryForList(
                                "select message_code, severity_code, message, details_json"
                                        + " from payroll.calculation_run_message where run_id = ?",
                                run.id()));
        entityManager.flush();
        entityManager.clear();
        return run;
    }

    /** Se cierra por el caso de uso de cierre en masa, que es el que consume las marcas. */
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

    private Long reciboId(String emp, String periodo) {
        return jdbc.queryForObject(
                "select id from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = 1",
                Long.class, RULE_SYSTEM, EMPLOYEE_TYPE, emp, periodo, PAYROLL_TYPE);
    }

    /** El importe de un concepto del propio mes del recibo. */
    private BigDecimal importe(Long payrollId, String conceptCode) {
        return jdbc.queryForObject(
                "select coalesce(sum(c.amount), 0) from payroll.payroll_concept c"
                        + " join payroll.payroll p on p.id = c.payroll_id"
                        + " where c.payroll_id = ? and c.concept_code = ?"
                        + "   and c.origin_period_code = p.payroll_period_code",
                BigDecimal.class, payrollId, conceptCode);
    }

    /** El importe de una línea de atraso: mismo concepto, otro origen. */
    private BigDecimal importeConOrigen(Long payrollId, String conceptCode, String origen) {
        return jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from payroll.payroll_concept"
                        + " where payroll_id = ? and concept_code = ? and origin_period_code = ?",
                BigDecimal.class, payrollId, conceptCode, origen);
    }

    private String atrasos(Long payrollId) {
        return jdbc.queryForList(
                        "select c.concept_code, c.origin_period_code, c.amount"
                                + " from payroll.payroll_concept c"
                                + " join payroll.payroll p on p.id = c.payroll_id"
                                + " where c.payroll_id = ? and c.origin_period_code <> p.payroll_period_code"
                                + " order by c.origin_period_code, c.concept_code",
                        payrollId)
                .toString();
    }

    private String huella(Long payrollId) {
        return jdbc.queryForObject(
                "select status || '|' || calculated_at || '|' ||"
                        + " (select coalesce(sum(amount), 0) from payroll.payroll_concept where payroll_id = p.id)"
                        + " from payroll.payroll p where id = ?",
                String.class, payrollId);
    }

    private List<RetroMark> marcas(String emp) {
        return listMarks.list(new ListEmployeeRetroMarksCommand(RULE_SYSTEM, EMPLOYEE_TYPE, emp));
    }

    private static String resumen(List<RetroMark> marcas) {
        return marcas.stream()
                .map(m -> m.getSource().verticalCode() + "@" + m.getFromPeriodCode() + ":" + m.getStatus())
                .toList().toString();
    }

    private String numeroUnico() {
        return "AT" + (System.nanoTime() % 1_000_000_000L);
    }
}
