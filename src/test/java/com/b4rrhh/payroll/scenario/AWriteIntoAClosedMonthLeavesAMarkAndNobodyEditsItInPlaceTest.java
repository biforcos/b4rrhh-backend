package com.b4rrhh.payroll.scenario;

import com.b4rrhh.employee.absence.application.usecase.DeleteAbsenceCommand;
import com.b4rrhh.employee.absence.application.usecase.DeleteAbsenceUseCase;
import com.b4rrhh.employee.absence.application.usecase.UpsertAbsenceCommand;
import com.b4rrhh.employee.absence.application.usecase.UpsertAbsenceUseCase;
import com.b4rrhh.employee.payroll_input.application.usecase.CreateEmployeePayrollInputCommand;
import com.b4rrhh.employee.payroll_input.application.usecase.CreateEmployeePayrollInputUseCase;
import com.b4rrhh.payroll.retro.application.usecase.DiscardRetroMarkCommand;
import com.b4rrhh.payroll.retro.application.usecase.DiscardRetroMarkUseCase;
import com.b4rrhh.payroll.retro.application.usecase.ListEmployeeRetroMarksCommand;
import com.b4rrhh.payroll.retro.application.usecase.ListEmployeeRetroMarksUseCase;
import com.b4rrhh.payroll.retro.domain.exception.RetroMarkNotActiveException;
import com.b4rrhh.payroll.retro.domain.model.RetroMark;
import com.b4rrhh.payroll.retro.domain.model.RetroMarkStatus;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Escribir en un mes que ya tiene recibo entregado deja una marca; escribir en uno abierto no deja
 * nada ({@code backend#130}, paso 6 de {@code b4rrhh/workspace#9}).
 *
 * <h2>Lo que este test defiende</h2>
 *
 * <p>Tres cosas, y las tres se pueden romper sin que nada mas se entere:
 *
 * <ol>
 *   <li><b>Que «pasado» sea lo cerrado y no lo antiguo.</b> Un mes calculado y sin cerrar no es
 *       pasado: se vuelve a calcular y no se le ha contado a nadie. Si la regla se relajara a «antes
 *       de hoy», cada correccion normal del mes en curso dejaria una marca y el motor recalcularia
 *       meses que no hacen falta.</li>
 *   <li><b>Que sean registros y no un indicador.</b> Dos escrituras a pasado son dos filas. Una
 *       columna «recalcular desde» que se sobreescribe pasaria el primer caso y perderia la cuenta en
 *       el segundo — y no habria forma de notarlo desde el recibo.</li>
 *   <li><b>Que descartar no borre.</b> La fila se queda, en {@code DISCARDED}, con quien y con por
 *       que. Es lo que le permite al recibo contar que <i>habia</i> una correccion conocida que
 *       alguien decidio no pagar, que es exactamente lo que un empleado pregunta.</li>
 * </ol>
 *
 * <p>Que ninguna vertical se salte el puerto lo vigila el candado
 * {@code EveryDatedWriteAnnouncesItselfThroughOnePortTest}; eso no lo puede ver un test de
 * comportamiento, porque una vertical que no avisa no rompe nada hoy.
 */
@TestWebSobreEsquemaReal
class AWriteIntoAClosedMonthLeavesAMarkAndNobodyEditsItInPlaceTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final LocalDate ENERO_1    = LocalDate.of(2026, 1, 1);
    private static final LocalDate JULIO_10   = LocalDate.of(2026, 7, 10);
    private static final LocalDate JULIO_12   = LocalDate.of(2026, 7, 12);
    private static final LocalDate AGOSTO_10  = LocalDate.of(2026, 8, 10);
    private static final LocalDate AGOSTO_12  = LocalDate.of(2026, 8, 12);

    private static final String JULIO     = "202607";
    private static final String AGOSTO    = "202608";

    @Autowired private UpsertAbsenceUseCase upsertAbsence;
    @Autowired private DeleteAbsenceUseCase deleteAbsence;
    @Autowired private CreateEmployeePayrollInputUseCase createInput;
    @Autowired private ListEmployeeRetroMarksUseCase listMarks;
    @Autowired private DiscardRetroMarkUseCase discardMark;
    @Autowired private JdbcTemplate jdbc;

    private PayrollScenarioFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new PayrollScenarioFixtures(jdbc);
    }

    /**
     * El caso que da sentido a los demas: <b>unas horas a un mes cerrado dejan marca; a uno abierto,
     * ninguna</b>.
     *
     * <p>Los dos en el mismo test a proposito. Por separado, una implementacion que marcara siempre
     * pasaria el primero, y una que no marcara nunca pasaria el segundo; juntos, hay que distinguir.
     */
    @Test
    void unasHorasAUnMesCerradoDejanMarcaYAUnoAbiertoNinguna() {
        String emp = numeroUnico();
        altaBasica(emp);
        reciboCerrado(emp, AGOSTO);

        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, "H01", 202608, new BigDecimal("10")));

        List<RetroMark> tras = marcas(emp);
        assertEquals(1, tras.size(), "agosto tiene recibo entregado: la escritura deja una marca");
        RetroMark m = tras.get(0);
        assertEquals(AGOSTO, m.getFromPeriodCode(), "y la marca es de agosto, el mes de la escritura");
        assertEquals(RetroMarkStatus.ACTIVE, m.getStatus(), "nace pendiente o no nace");
        assertEquals("PAYROLL_INPUT", m.getSource().verticalCode());
        assertEquals("employee.employee_payroll_input", m.getSource().table());
        assertEquals("H01/202608", m.getSource().rowKey(),
                "la entrada de nomina no tiene id surrogado: se identifica por concepto y periodo");
        assertNull(m.getSource().rowId(), "y por eso no hay id");
        assertEquals(1, (int) (Integer) m.getPresenceNumber(),
                "la marca es de la presencia del recibo, porque el recibo es de una presencia");

        // Y ahora el mismo empleado, el mismo concepto, un mes SIN recibo entregado.
        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, "H01", 202609, new BigDecimal("10")));

        assertEquals(1, marcas(emp).size(),
                "septiembre no tiene recibo entregado, asi que no es pasado y no deja marca:"
                        + " sigue habiendo una sola, la de agosto");
    }

    /**
     * Un mes <b>calculado y sin cerrar</b> tampoco es pasado.
     *
     * <p>Es el caso que separa «pasado» de «antiguo», y el que se rompe si alguien decide que basta
     * con que exista un recibo. Un recibo {@code CALCULATED} se vuelve a calcular entero y no ha
     * salido del sistema: no hay nada que corregir con un atraso.
     */
    @Test
    void unMesCalculadoYSinCerrarNoEsPasado() {
        String emp = numeroUnico();
        altaBasica(emp);
        fixtures.insertPayrollWithConcept(RULE_SYSTEM, EMPLOYEE_TYPE, emp, AGOSTO,
                PAYROLL_TYPE, 1, "CALCULATED", "B_CC", new BigDecimal("3000.00"));

        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, "H01", 202608, new BigDecimal("10")));

        assertTrue(marcas(emp).isEmpty(),
                "agosto existe pero todavia puede cambiar, asi que no se le ha contado a nadie y no"
                        + " hay atraso que pagar; marcas: " + marcas(emp));
    }

    /**
     * Dos escrituras a pasado son <b>dos filas</b>, no una actualizada.
     *
     * <p>De dos verticales distintas y a dos meses distintos, que es como llega en la operativa real:
     * unas horas olvidadas de agosto y una baja que nadie registro en julio. Una columna que se
     * sobreescribe daria una sola fila y perderia el rastro de la otra.
     */
    @Test
    void dosEscriturasAPasadoDejanDosFilas() {
        String emp = numeroUnico();
        long empId = altaBasica(emp);
        reciboCerrado(emp, JULIO);
        reciboCerrado(emp, AGOSTO);

        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, "H01", 202608, new BigDecimal("10")));
        upsertAbsence.upsert(new UpsertAbsenceCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, "IT_COMMON", JULIO_10, 0, JULIO_12, null, null));

        List<RetroMark> tras = marcas(emp);
        assertEquals(2, tras.size(), "dos hechos distintos son dos filas: " + resumen(tras));
        assertEquals(List.of("ABSENCE", "PAYROLL_INPUT"),
                tras.stream().map(x -> x.getSource().verticalCode()).sorted().toList(),
                "y cada una dice de que vertical viene, que es por lo que agrupa la ficha");
        assertEquals(List.of(JULIO, AGOSTO),
                tras.stream().map(RetroMark::getFromPeriodCode).sorted().toList(),
                "y a que mes alcanza cada una: el motor consumira el minimo, que es julio");

        // La de la ausencia lleva id y no clave, porque la ausencia SI tiene id surrogado.
        RetroMark laDeLaAusencia = tras.stream()
                .filter(x -> "ABSENCE".equals(x.getSource().verticalCode())).findFirst().orElseThrow();
        assertNotNull(laDeLaAusencia.getSource().rowId(),
                "la ausencia se identifica por id, asi que la marca lo guarda");
        assertEquals("employee.employee_absence", laDeLaAusencia.getSource().table());
        assertTrue(empId > 0, "el empleado existe: " + empId);
    }

    /**
     * Borrar tambien es escribir.
     *
     * <p>Quitar una baja de un mes cerrado le devuelve los dias que le habia quitado (ADR-073), asi
     * que mueve el recibo igual que ponerla. Y la fila ya no esta: la marca es lo unico que queda de
     * ella, con su id, para que se pueda decir cual era.
     */
    @Test
    void borrarUnaBajaDeUnMesCerradoTambienDejaMarca() {
        String emp = numeroUnico();
        long empId = altaBasica(emp);
        fixtures.insertAbsence(empId, "IT_COMMON", AGOSTO_10, AGOSTO_12);
        reciboCerrado(emp, AGOSTO);

        deleteAbsence.delete(new DeleteAbsenceCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, "IT_COMMON", AGOSTO_10, 0));

        List<RetroMark> tras = marcas(emp);
        assertEquals(1, tras.size(), "un borrado en un mes cerrado deja marca: " + resumen(tras));
        assertEquals(AGOSTO, tras.get(0).getFromPeriodCode());
        assertEquals("ABSENCE", tras.get(0).getSource().verticalCode());
        assertNotNull(tras.get(0).getSource().rowId(),
                "el id de la fila borrada se guarda: la marca es lo unico que queda de ella");
    }

    /**
     * Descartar <b>no borra</b>: la fila sigue, en su estado, con quien y con por que.
     *
     * <p>Y no se puede descartar dos veces. Dos motivos sobre la misma fila dejarian al recibo sin
     * poder decir cual valia.
     */
    @Test
    void descartarDejaLaFilaConQuienYPorQueYNoSePuedeDosVeces() {
        String emp = numeroUnico();
        altaBasica(emp);
        reciboCerrado(emp, AGOSTO);
        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, "H01", 202608, new BigDecimal("10")));

        Long id = marcas(emp).get(0).getId();

        RetroMark descartada = discardMark.discard(new DiscardRetroMarkCommand(
                id, "juan", "Las horas ya se pagaron en mano en agosto"));

        assertEquals(RetroMarkStatus.DISCARDED, descartada.getStatus());
        assertEquals("juan", descartada.getDiscardedBy());
        assertEquals("Las horas ya se pagaron en mano en agosto", descartada.getDiscardReason());
        assertNotNull(descartada.getDiscardedAt());

        List<RetroMark> tras = marcas(emp);
        assertEquals(1, tras.size(),
                "descartar no es borrar: la fila sigue ahi, y es lo que permite al recibo contar que"
                        + " habia una correccion que alguien decidio no pagar");
        assertEquals(RetroMarkStatus.DISCARDED, tras.get(0).getStatus());
        assertEquals(AGOSTO, tras.get(0).getFromPeriodCode(),
                "y sigue diciendo a que mes alcanzaba, que es parte de lo que hay que contar");

        assertThrows(RetroMarkNotActiveException.class,
                () -> discardMark.discard(new DiscardRetroMarkCommand(id, "juan", "otra vez")),
                "una marca ya descartada no se vuelve a descartar: quedarian dos motivos y ninguna"
                        + " forma de saber cual valia");
    }

    /**
     * Un descarte sin motivo no se guarda.
     *
     * <p>El motivo no es cortesia: sin el, la fila descartada no sirve para lo unico que la fila
     * descartada existe para hacer, que es contarlo.
     */
    @Test
    void unDescarteSinMotivoSeRechaza() {
        String emp = numeroUnico();
        altaBasica(emp);
        reciboCerrado(emp, AGOSTO);
        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, "H01", 202608, new BigDecimal("10")));
        Long id = marcas(emp).get(0).getId();

        assertThrows(IllegalArgumentException.class,
                () -> discardMark.discard(new DiscardRetroMarkCommand(id, "juan", "   ")));

        assertEquals(RetroMarkStatus.ACTIVE, marcas(emp).get(0).getStatus(),
                "y la marca se queda activa: un descarte que no se pudo contar no ocurrio");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long altaBasica(String emp) {
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, ENERO_1, null);
        fixtures.insertLaborClassification(empId, ENERO_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), ENERO_1, null);
        return empId;
    }

    /** Un recibo entregado de ese mes, que es lo que convierte al mes en pasado. */
    private void reciboCerrado(String emp, String periodo) {
        fixtures.insertPayrollWithConcept(RULE_SYSTEM, EMPLOYEE_TYPE, emp, periodo,
                PAYROLL_TYPE, 1, "DEFINITIVE", "B_CC", new BigDecimal("3000.00"));
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
        return "RM" + (System.nanoTime() % 1_000_000_000L);
    }

}
