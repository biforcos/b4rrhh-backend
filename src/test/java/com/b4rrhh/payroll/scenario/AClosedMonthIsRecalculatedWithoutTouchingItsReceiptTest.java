package com.b4rrhh.payroll.scenario;

import com.b4rrhh.employee.payroll_input.application.usecase.CreateEmployeePayrollInputCommand;
import com.b4rrhh.employee.payroll_input.application.usecase.CreateEmployeePayrollInputUseCase;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.retro.application.usecase.RecalculateClosedPeriodsCommand;
import com.b4rrhh.payroll.retro.application.usecase.RecalculateClosedPeriodsUseCase;
import com.b4rrhh.payroll.retro.domain.model.CurrentCalculation;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Un mes cerrado se recalcula y <b>su recibo no cambia ni un byte</b> ({@code backend#131}, ADR-076).
 *
 * <h2>Lo que este test defiende</h2>
 *
 * <p>La distincion que hace seguro todo el paso 6:
 *
 * <blockquote>
 * El <b>recibo</b> es un documento: inmutable, se entrega, tiene PDF.<br>
 * El <b>calculo vigente</b> es estado: mutable, se pisa, no lo ve nadie.
 * </blockquote>
 *
 * <p>Y las dos mitades de esa frase hacen falta. Que el recibo no se toque sin que exista el vigente
 * seria un motor que no puede pagar atrasos; que exista el vigente sin que el recibo quede intacto es
 * un motor que pierde la unica copia de lo que se entrego — y entonces el numero que el empleado tiene
 * impreso deja de existir en el sistema.
 *
 * <p>Que ningun <i>camino</i> nuevo llegue al recibo desde una retro lo vigila el candado
 * {@code NoRetroPathWritesTheReceiptOfAClosedMonthTest}; eso no lo puede ver un test de comportamiento,
 * porque el escenario que compara es este y un camino nuevo no lo pone rojo.
 */
@TestWebSobreEsquemaReal
class AClosedMonthIsRecalculatedWithoutTouchingItsReceiptTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PAYROLL_TYPE  = "NORMAL";

    /** El concepto de entrada de las horas extra, que es el unico EMPLOYEE_INPUT del catalogo (V133). */
    private static final String CONCEPTO_DE_ENTRADA = "H01";

    private static final String JULIO  = "202507";
    private static final String AGOSTO = "202508";

    private static final LocalDate ENERO_1_2025  = LocalDate.of(2025, 1, 1);
    private static final LocalDate JULIO_10      = LocalDate.of(2025, 7, 10);
    private static final LocalDate JULIO_15      = LocalDate.of(2025, 7, 15);
    private static final LocalDate AGOSTO_10     = LocalDate.of(2025, 8, 10);
    private static final LocalDate AGOSTO_12     = LocalDate.of(2025, 8, 12);

    @Autowired private LaunchPayrollCalculationUseCase launch;
    @Autowired private RecalculateClosedPeriodsUseCase recalcular;
    @Autowired private CreateEmployeePayrollInputUseCase createInput;
    @Autowired private JdbcTemplate jdbc;

    @PersistenceContext private EntityManager entityManager;

    private PayrollScenarioFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new PayrollScenarioFixtures(jdbc);
    }

    /**
     * El caso central: agosto cerrado, unas horas metidas a agosto, y se recalcula.
     *
     * <p><b>El vigente lleva las horas y el recibo no ha cambiado</b>, y las dos mitades se comprueban:
     * la primera mirando la linea del vigente, la segunda comparando <i>toda</i> la fila del recibo y
     * <i>todas</i> sus lineas antes y despues. Comparar solo un importe dejaria pasar un recalculo que
     * moviera la fecha, el estado o el numero de lineas.
     */
    @Test
    void elVigenteLlevaLasHorasYElReciboDeAgostoNoHaCambiado() {
        String emp = numeroUnico();
        altaBasica(emp);

        Long reciboId = calcularYCerrar(emp, AGOSTO);
        String reciboAntes = huellaDelRecibo(reciboId);
        List<Map<String, Object>> lineasAntes = lineasDelRecibo(reciboId);
        assertFalse(lineasAntes.isEmpty(), "el recibo de agosto tiene lineas antes de la retro");

        // Las horas llegan tarde: agosto ya estaba entregado.
        createInput.create(new CreateEmployeePayrollInputCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, CONCEPTO_DE_ENTRADA, 202508, new BigDecimal("10")));

        var resultado = recalcular.recalculate(new RecalculateClosedPeriodsCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, 1, PAYROLL_TYPE,
                AGOSTO, AGOSTO, "ENGINE", "1.0", null));
        entityManager.flush();

        assertEquals(List.of(), resultado.notCalculated(),
                "agosto se puede recalcular: tiene todo lo que necesita");
        assertEquals(1, resultado.written().size());

        CurrentCalculation vigente = resultado.written().get(0);
        assertEquals(AGOSTO, vigente.getPayrollPeriodCode());
        assertTrue(importeDelVigente(vigente, "102").compareTo(BigDecimal.ZERO) > 0,
                "el vigente de agosto lleva las horas extra (102), que es lo que se metio tarde;"
                        + " lineas: " + resumen(vigente));

        // Y la otra mitad, que es la que importa.
        assertEquals(reciboAntes, huellaDelRecibo(reciboId),
                "el recibo de agosto no ha cambiado ni un byte: sigue siendo el documento que se"
                        + " entrego");
        assertEquals(lineasAntes, lineasDelRecibo(reciboId),
                "y ni una de sus lineas: mismo numero, mismos importes, mismos literales");
        assertTrue(importeDelRecibo(reciboId, "102").compareTo(BigDecimal.ZERO) == 0,
                "el recibo de agosto NO lleva las horas: se pagaran como atraso en el mes abierto,"
                        + " que es de lo que va el backend#133");
    }

    /**
     * El encadenado: julio y agosto cerrados, unas horas a <b>julio</b>, y se recalcula el tramo.
     *
     * <p>El vigente de agosto sale con una base reguladora <b>distinta</b> de la de su recibo, porque
     * leyo el vigente de julio y no el recibo de julio. Es la razon de que el tramo vaya hacia delante:
     * si agosto se calculara con el julio viejo, el atraso de agosto saldria mal por construccion y
     * nada lo diria.
     */
    @Test
    void elVigenteDeAgostoLeeElVigenteDeJulioYNoSuRecibo() {
        String emp = numeroUnico();
        long empId = altaBasica(emp);
        // Una baja en agosto: sin ella agosto no tiene base reguladora que leer (ADR-074 §2) y este
        // test no distinguiria nada.
        fixtures.insertAbsence(empId, "IT_COMMON", AGOSTO_10, AGOSTO_12);

        Long julioId = calcularYCerrar(emp, JULIO);
        Long agostoId = calcularYCerrar(emp, AGOSTO);

        BigDecimal baseJulioEnElRecibo = importeDelRecibo(julioId, "B_CC");
        BigDecimal brAgostoEnElRecibo = baseReguladoraDelRecibo(agostoId);
        assertTrue(baseJulioEnElRecibo.compareTo(BigDecimal.ZERO) > 0,
                "julio tiene base de contingencias comunes en su recibo");
        assertTrue(brAgostoEnElRecibo.compareTo(BigDecimal.ZERO) > 0,
                "y agosto tiene base reguladora, porque tiene baja");

        // Lo que llega tarde a JULIO es un permiso no retribuido, y no unas horas extra: las horas
        // extra NO cotizan en la base de contingencias comunes (backend#104, y hay un test que se llama
        // asi), asi que no moverian lo que agosto lee de julio y este test no distinguiria nada. El
        // permiso no retribuido si: quita dias de devengo (ADR-073) y con ellos baja la base.
        fixtures.insertAbsence(empId, "UNPAID_LEAVE", JULIO_10, JULIO_15);

        var resultado = recalcular.recalculate(new RecalculateClosedPeriodsCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, 1, PAYROLL_TYPE,
                JULIO, AGOSTO, "ENGINE", "1.0", null));
        entityManager.flush();

        assertEquals(List.of(), resultado.notCalculated());
        assertEquals(List.of(JULIO, AGOSTO),
                resultado.written().stream().map(CurrentCalculation::getPayrollPeriodCode).toList(),
                "el tramo se recalcula hacia delante y en orden: julio antes que agosto, porque"
                        + " agosto lee julio");

        CurrentCalculation vigenteJulio = resultado.written().get(0);
        CurrentCalculation vigenteAgosto = resultado.written().get(1);

        assertTrue(importeDelVigente(vigenteJulio, "B_CC").compareTo(baseJulioEnElRecibo) < 0,
                "el permiso no retribuido ha bajado la base de cotizacion de julio: "
                        + importeDelVigente(vigenteJulio, "B_CC") + " frente a " + baseJulioEnElRecibo);

        BigDecimal brAgostoEnElVigente = importeDelVigente(vigenteAgosto, "BR_CC");
        assertNotEquals(0, brAgostoEnElVigente.compareTo(brAgostoEnElRecibo),
                "la base reguladora de agosto ha cambiado, porque leyo el VIGENTE de julio y no su"
                        + " recibo: vigente " + brAgostoEnElVigente + ", recibo " + brAgostoEnElRecibo);
        assertTrue(brAgostoEnElVigente.compareTo(brAgostoEnElRecibo) < 0,
                "y ha bajado, como la base de julio de la que sale");
    }

    /**
     * Las reglas de cada mes son las <b>vigentes en aquel mes</b>.
     *
     * <p>Dos meses de <b>dos ejercicios distintos</b> recalculados en la misma corrida, cada uno con sus
     * topes. El motor ya sabia resolver vigencias por fecha ({@code backend#105}); lo que este test
     * defiende es que el tramo no cargue las reglas una vez con la fecha del lanzamiento, que es la
     * forma facil de escribirlo y calcularia diciembre de 2024 con los tipos de 2025 — dando un numero,
     * y estando mal.
     */
    @Test
    void unMesDeOtroEjercicioSeRecalculaConLasReglasDeSuEjercicio() {
        String emp = numeroUnico();
        fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        long empId = jdbc.queryForObject(
                "select id from employee.employee where rule_system_code = ? and employee_type_code = ?"
                        + " and employee_number = ?",
                Long.class, RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        LocalDate desde = LocalDate.of(2024, 1, 1);
        fixtures.insertPresence(empId, 1, desde, null);
        fixtures.insertLaborClassification(empId, desde);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), desde, null);

        Long diciembre24 = calcularYCerrar(emp, "202412");
        Long enero25 = calcularYCerrar(emp, "202501");

        var resultado = recalcular.recalculate(new RecalculateClosedPeriodsCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, emp, 1, PAYROLL_TYPE,
                "202412", "202501", "ENGINE", "1.0", null));
        entityManager.flush();

        assertEquals(List.of(), resultado.notCalculated());
        assertEquals(2, resultado.written().size());

        // Cada vigente reproduce el importe de SU recibo: es el mismo mes, calculado con las mismas
        // reglas que entonces, y nada ha cambiado entre medias. Si el tramo cargara las reglas una
        // sola vez, uno de los dos saldria distinto.
        for (CurrentCalculation vigente : resultado.written()) {
            Long reciboId = JULIO.equals(vigente.getPayrollPeriodCode()) ? null
                    : "202412".equals(vigente.getPayrollPeriodCode()) ? diciembre24 : enero25;
            assertEquals(0, importeDelVigente(vigente, "990").compareTo(importeDelRecibo(reciboId, "990")),
                    "el liquido del vigente de " + vigente.getPayrollPeriodCode()
                            + " tiene que ser el de su recibo: nada ha cambiado, y las reglas son las"
                            + " de aquel mes. vigente=" + importeDelVigente(vigente, "990")
                            + " recibo=" + importeDelRecibo(reciboId, "990"));
        }

        // Y que los dos ejercicios no son iguales, para que la comprobacion de arriba diga algo: los
        // topes de 2024 y de 2025 son distintos (V153).
        assertNotEquals(
                importeDelRecibo(diciembre24, "B_CC").stripTrailingZeros(),
                importeDelRecibo(enero25, "B_CC").stripTrailingZeros(),
                "si los dos ejercicios dieran la misma base, este test no distinguiria cargar las"
                        + " reglas por mes de cargarlas una vez");
    }

    /** Un tramo al reves no es un tramo. */
    @Test
    void unTramoHaciaAtrasSeRechaza() {
        String emp = numeroUnico();
        altaBasica(emp);

        IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> recalcular.recalculate(new RecalculateClosedPeriodsCommand(
                        RULE_SYSTEM, EMPLOYEE_TYPE, emp, 1, PAYROLL_TYPE,
                        AGOSTO, JULIO, "ENGINE", "1.0", null)));
        assertTrue(ex.getMessage().contains("hacia delante"), ex.getMessage());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long altaBasica(String emp) {
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, ENERO_1_2025, null);
        fixtures.insertLaborClassification(empId, ENERO_1_2025);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), ENERO_1_2025, null);
        return empId;
    }

    /** Calcula el mes por el camino normal y lo deja {@code DEFINITIVE}: un mes entregado. */
    private Long calcularYCerrar(String emp, String periodo) {
        CalculationRun run = launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, periodo, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, emp),
                        null),
                null));
        assertTrue("COMPLETED".equals(run.status()), "la corrida de " + periodo + ": " + run.status());
        entityManager.flush();

        Long id = jdbc.queryForObject(
                "select id from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = 1",
                Long.class, RULE_SYSTEM, EMPLOYEE_TYPE, emp, periodo, PAYROLL_TYPE);

        // Se cierra por SQL y no por el caso de uso de cierre a proposito: lo que este test necesita es
        // un mes entregado, y el camino por el que llego a estarlo es de otro issue.
        jdbc.update("update payroll.payroll set status = 'DEFINITIVE' where id = ?", id);
        entityManager.clear();
        return id;
    }

    /**
     * Toda la fila del recibo menos lo que cambia sin que nadie lo toque.
     *
     * <p>Se comparan el estado, el motivo, el instante de calculo, el motor y su version: si un
     * recalculo hubiera pasado por aqui, cualquiera de los cinco lo delataria. {@code updated_at} no
     * entra porque lo mueve el propio {@code @PreUpdate} de cualquier lectura con escritura alrededor, y
     * un test que fallara por eso seria ruido.
     */
    private String huellaDelRecibo(Long payrollId) {
        return jdbc.queryForObject(
                "select status || '|' || coalesce(status_reason_code, '-') || '|' || calculated_at"
                        + " || '|' || calculation_engine_code || '|' || calculation_engine_version"
                        + " from payroll.payroll where id = ?",
                String.class, payrollId);
    }

    private List<Map<String, Object>> lineasDelRecibo(Long payrollId) {
        return jdbc.queryForList(
                "select line_number, concept_code, concept_mnemonic, concept_label, amount, quantity,"
                        + " rate, concept_nature_code, origin_period_code, display_order"
                        + " from payroll.payroll_concept where payroll_id = ? order by line_number",
                payrollId);
    }

    private BigDecimal importeDelRecibo(Long payrollId, String conceptCode) {
        return jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from payroll.payroll_concept"
                        + " where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, conceptCode);
    }

    /**
     * La base reguladora del recibo, leida de los pasos.
     *
     * <p>De los pasos y no de las lineas porque {@code BR_CC} es tecnica y no se imprime. El maximo y no
     * la suma: es de ambito {@code SEGMENT} y vale cero en los tramos trabajados (ADR-074 §6).
     */
    private BigDecimal baseReguladoraDelRecibo(Long payrollId) {
        return jdbc.queryForObject(
                "select coalesce(max(amount), 0) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = 'BR_CC'",
                BigDecimal.class, payrollId);
    }

    /**
     * El importe de una linea del vigente.
     *
     * <p>El maximo y no la suma, por lo mismo que arriba: las lineas del vigente son las del folio, y
     * las de ambito de tramo pueden venir en varias. Para los conceptos de periodo —{@code 990},
     * {@code B_CC}— hay una sola y el maximo es ella.
     */
    private BigDecimal importeDelVigente(CurrentCalculation vigente, String conceptCode) {
        return vigente.getConcepts().stream()
                .filter(c -> conceptCode.equals(c.conceptCode()))
                .map(c -> c.amount())
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private static String resumen(CurrentCalculation vigente) {
        return vigente.getConcepts().stream()
                .map(c -> c.conceptCode() + "=" + c.amount())
                .toList().toString();
    }

    private String numeroUnico() {
        return "VG" + (System.nanoTime() % 1_000_000_000L);
    }
}
