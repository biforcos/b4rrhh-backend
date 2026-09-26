package com.b4rrhh.payroll.scenario;

import com.b4rrhh.employee.payroll_input.application.usecase.CreateEmployeePayrollInputCommand;
import com.b4rrhh.employee.payroll_input.application.usecase.CreateEmployeePayrollInputUseCase;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.application.usecase.PayrollRetroRequest;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.retro.application.usecase.ListEmployeeRetroMarksCommand;
import com.b4rrhh.payroll.retro.application.usecase.ListEmployeeRetroMarksUseCase;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El lanzamiento dice hasta dónde atrás recalcula, y lo que no alcanza <b>se dice</b>
 * ({@code backend#132}).
 *
 * <h2>Lo que este test defiende</h2>
 *
 * <p>Tres cosas, y la tercera es la que este proyecto no admite que se rompa:
 *
 * <ol>
 *   <li><b>Que los dos parámetros no puedan pedir cosas contrarias.</b> Un suelo más antiguo que el
 *       límite se rechaza en la petición, no al calcular, y no gana ninguno de los dos en silencio.</li>
 *   <li><b>Que el suelo signifique «todos».</b> Un empleado sin ninguna marca recalcula desde el suelo,
 *       porque eso es lo que es una revisión de convenio.</li>
 *   <li><b>Que una corrección que no se paga no se calle.</b> Una marca más antigua que el límite deja
 *       un aviso en el recibo con los dos períodos, y <b>la marca sigue activa</b> — así que aparecerá
 *       en la checklist y alguien tendrá que decidir. Si se borrara o se acortara, el atraso quedaría
 *       sin pagar y sin nadie que pudiera saberlo.</li>
 * </ol>
 */
@TestWebSobreEsquemaReal
class TheLaunchSaysHowFarBackItRecalculatesAndTheRestIsSaidNotSilencedTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PAYROLL_TYPE  = "NORMAL";

    /** El único concepto de entrada del catálogo (V133). */
    private static final String CONCEPTO_DE_ENTRADA = "H01";

    private static final LocalDate ENERO_1_2026 = LocalDate.of(2026, 1, 1);

    @Autowired private LaunchPayrollCalculationUseCase launch;
    @Autowired private CreateEmployeePayrollInputUseCase createInput;
    @Autowired private ListEmployeeRetroMarksUseCase listMarks;
    @Autowired private JdbcTemplate jdbc;

    @PersistenceContext private EntityManager entityManager;

    private PayrollScenarioFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new PayrollScenarioFixtures(jdbc);
    }

    /** Un suelo más antiguo que el límite no se puede construir, así que no llega al motor. */
    @Test
    void unSueloMasAntiguoQueElLimiteSeRechazaEnLaPeticion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new PayrollRetroRequest("202601", "202606"));
        assertTrue(ex.getMessage().contains("no puede ser mas antiguo que el limite"), ex.getMessage());
        assertTrue(ex.getMessage().contains("202601") && ex.getMessage().contains("202606"),
                "el mensaje dice los dos periodos, que es lo que hace falta para corregirlo: "
                        + ex.getMessage());
    }

    /** Y un suelo sin límite tampoco: sin límite no hay retro, así que el suelo no llegaría a nada. */
    @Test
    void unSueloSinLimiteSeRechazaEnLaPeticion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new PayrollRetroRequest("202601", null));
        assertTrue(ex.getMessage().contains("sin limite"), ex.getMessage());
    }

    /**
     * El caso que el issue llama por su nombre: <b>marca a 202603 con límite 202606</b>.
     *
     * <p>No se paga, la marca sigue activa, no hay vigente de 202603, y el recibo de septiembre lo dice
     * con los dos períodos.
     */
    @Test
    void unaMarcaMasAntiguaQueElLimiteNoSePagaYNoSeCalla() {
        String emp = numeroUnico();
        altaBasica(emp);
        cerrar(emp, "202603");

        // La corrección llega a marzo, que está entregado: queda marca.
        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, CONCEPTO_DE_ENTRADA, 202603, new BigDecimal("10")));
        assertEquals(1, marcas(emp).size(), "la corrección a marzo ha dejado su marca");

        CalculationRun run = lanzar(emp, "202609", new PayrollRetroRequest(null, "202606"));

        assertEquals(0, (int) run.totalRetroUnits(),
                "el límite es junio y la marca es de marzo: no hay ni un mes que recalcular");
        assertEquals("202606", run.retroLimitPeriodCode(),
                "y el run guarda con qué se calculó, que es lo que el recibo y la checklist leen");

        assertEquals(0, cuantosVigentes(emp, "202603"),
                "no hay vigente de marzo: el límite no se ha saltado");

        assertEquals(RetroMarkStatus.ACTIVE, marcas(emp).get(0).getStatus(),
                "y la marca SIGUE ACTIVA: no se paga, pero tampoco se consume ni se borra, así que"
                        + " aparecerá en la checklist y alguien tendrá que decidir");

        Map<String, String> avisos = avisosDelRecibo(emp, "202609");
        assertTrue(avisos.containsKey("RETRO_MARK_OUTSIDE_LIMIT"),
                "el recibo de septiembre lo dice con palabras; avisos: " + avisos.keySet());
        String mensaje = avisos.get("RETRO_MARK_OUTSIDE_LIMIT");
        assertTrue(mensaje.contains("202603"),
                "y nombra el período de la marca, que es lo que hay que ir a mirar: " + mensaje);
    }

    /**
     * El suelo para todos: un empleado <b>sin ninguna marca</b> recalcula desde ahí.
     *
     * <p>Es lo que hace que «todos desde enero» signifique todos, y es el caso que distingue un suelo de
     * un límite: sin él, el suelo sería sólo otra forma de acotar las marcas que ya había.
     */
    @Test
    void conSueloParaTodosUnEmpleadoSinMarcasRecalculaDesdeElSuelo() {
        String emp = numeroUnico();
        altaBasica(emp);
        cerrar(emp, "202607");
        cerrar(emp, "202608");

        assertTrue(marcas(emp).isEmpty(), "este empleado no tiene ninguna marca");

        CalculationRun run = lanzar(emp, "202609", new PayrollRetroRequest("202607", "202601"));

        assertEquals(2, (int) run.totalRetroUnits(),
                "julio y agosto: el suelo manda aunque no haya ninguna marca");
        assertEquals(2, (int) run.totalRetroRecalculated(), "y los dos se han recalculado");
        assertEquals(0, (int) run.totalRetroNotRecalculated());
        assertEquals("202607", run.retroFloorPeriodCode());
        assertEquals("202601", run.retroLimitPeriodCode());

        assertEquals(1, cuantosVigentes(emp, "202607"), "hay vigente de julio");
        assertEquals(1, cuantosVigentes(emp, "202608"), "y de agosto");
        assertEquals(0, cuantosVigentes(emp, "202609"),
                "y ninguno de septiembre: el mes abierto se calcula por el camino normal y escribe su"
                        + " recibo, que es de lo que va el paso");
    }

    /**
     * Un lanzamiento <b>sin límite</b> no hace retro, y <b>lo dice</b> cuando había marcas.
     *
     * <p>Es la mitad del diseño que no estaba en el issue y que hay que defender: una corrida no puede
     * inventarse hasta dónde atrás tiene permiso para recalcular, pero tampoco puede dejar sin pagar un
     * atraso sin que nadie lo sepa.
     */
    @Test
    void sinLimiteNoHayRetroYLaCorridaLoDice() {
        String emp = numeroUnico();
        altaBasica(emp);
        cerrar(emp, "202608");
        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, CONCEPTO_DE_ENTRADA, 202608, new BigDecimal("10")));
        assertEquals(1, marcas(emp).size());

        CalculationRun run = lanzar(emp, "202609", PayrollRetroRequest.none());

        assertEquals(0, (int) run.totalRetroUnits(), "sin límite no se recalcula nada");
        assertEquals(0, cuantosVigentes(emp, "202608"));
        assertEquals(RetroMarkStatus.ACTIVE, marcas(emp).get(0).getStatus(),
                "la marca sigue viva: nadie la ha pagado y nadie la ha descartado");

        List<Map<String, Object>> mensajes = jdbc.queryForList(
                "select message_code, severity_code, message from payroll.calculation_run_message"
                        + " where run_id = ? and message_code = 'RETRO_SKIPPED_NO_LIMIT'",
                run.id());
        assertEquals(1, mensajes.size(),
                "la corrida lo dice: un atraso que no se paga en silencio es lo único que no se admite");
        assertTrue(((String) mensajes.get(0).get("message")).contains("1 empleado"),
                "y dice cuántos eran: " + mensajes.get(0).get("message"));
    }

    /**
     * Y el contador cuenta <b>empleado × mes</b>, que es lo que la pantalla tiene que enseñar.
     *
     * <p>Dos empleados con dos meses de tramo cada uno son cuatro unidades de retro y dos candidatos.
     * Una pantalla que contara candidatos diría «2» mientras el motor hace seis cosas.
     */
    @Test
    void laEjecucionCuentaPorEmpleadoYMesYNoPorEmpleado() {
        String uno = numeroUnico();
        String dos = numeroUnico();
        altaBasica(uno);
        altaBasica(dos);
        for (String emp : List.of(uno, dos)) {
            cerrar(emp, "202607");
            cerrar(emp, "202608");
        }

        CalculationRun run = lanzarTodos("202609", new PayrollRetroRequest("202607", "202601"), uno, dos);

        assertEquals(2, (int) run.totalCandidates(),
                "dos candidatos: los recibos de septiembre que hay que calcular");
        assertEquals(4, (int) run.totalRetroUnits(),
                "y cuatro unidades de retro: dos empleados por dos meses. Es lo que la pantalla enseña"
                        + " como empleado x mes, y sin eso un lanzamiento con suelo para todos parece"
                        + " colgado");
        assertEquals(4, (int) run.totalRetroRecalculated());
        assertEquals((int) run.totalRetroUnits(),
                run.totalRetroRecalculated() + run.totalRetroNotRecalculated(),
                "la partición de la retro cuadra, y es suya: no se suma a la de los recibos porque una"
                        + " unidad de retro no acaba en ninguno de esos cajones");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long altaBasica(String emp) {
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, ENERO_1_2026, null);
        fixtures.insertLaborClassification(empId, ENERO_1_2026);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), ENERO_1_2026, null);
        return empId;
    }

    /** Un recibo entregado de ese mes, que es lo que convierte al mes en pasado. */
    private void cerrar(String emp, String periodo) {
        fixtures.insertPayrollWithConcept(RULE_SYSTEM, EMPLOYEE_TYPE, emp, periodo,
                PAYROLL_TYPE, 1, "DEFINITIVE", "B_CC", new BigDecimal("1900.00"));
    }

    private CalculationRun lanzar(String emp, String periodo, PayrollRetroRequest retro) {
        CalculationRun run = launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, periodo, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, emp),
                        null),
                null,
                retro));
        entityManager.flush();
        entityManager.clear();
        return run;
    }

    private CalculationRun lanzarTodos(String periodo, PayrollRetroRequest retro, String... emps) {
        CalculationRun run = launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, periodo, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.EMPLOYEE_LIST,
                        null,
                        java.util.Arrays.stream(emps)
                                .map(e -> new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, e))
                                .toList()),
                null,
                retro));
        entityManager.flush();
        entityManager.clear();
        return run;
    }

    private List<com.b4rrhh.payroll.retro.domain.model.RetroMark> marcas(String emp) {
        return listMarks.list(new ListEmployeeRetroMarksCommand(RULE_SYSTEM, EMPLOYEE_TYPE, emp));
    }

    private int cuantosVigentes(String emp, String periodo) {
        return jdbc.queryForObject(
                "select count(*) from payroll.current_calculation"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ?",
                Integer.class, RULE_SYSTEM, EMPLOYEE_TYPE, emp, periodo);
    }

    /** Los avisos del recibo de ese mes, sin el {@code ELIGIBLE_REAL_EXECUTION} que llevan todos. */
    private Map<String, String> avisosDelRecibo(String emp, String periodo) {
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

    private String numeroUnico() {
        return "RL" + (System.nanoTime() % 1_000_000_000L);
    }
}
