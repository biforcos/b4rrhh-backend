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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La prestacion por incapacidad temporal, por tramos de dias, y la base que no baja con la baja
 * ({@code backend#129}, ADR-075).
 *
 * <h2>Las tres cosas que este test afirma a la vez</h2>
 *
 * <ol>
 *   <li><b>Los tramos de porcentaje.</b> Dias 1-3 nada; 4-15 el 60 % a cargo de la empresa; 16-20 el
 *       60 % en pago delegado; 21 en adelante el 75 %. Y los dias se cuentan <b>desde el inicio de la
 *       ausencia</b>, que puede estar en otro mes: no es leer otro mes, es una fecha.</li>
 *   <li><b>El complemento del convenio.</b> Grandes almacenes complementa hasta el 100 % del salario
 *       base de grupo (BOE-A-2023-13740, art. 50) y no paga nada los tres primeros dias. Con el, quien
 *       esta de baja el mes entero devenga exactamente lo que devengaria trabajando.</li>
 *   <li><b>La base no baja.</b> Durante la baja se sigue cotizando, y por eso hay una base mas:
 *       {@code B10 = dias de baja x base reguladora}. La invariante nueva es
 *       {@code B01 = B03 + B04 + B10}.</li>
 * </ol>
 *
 * <h2>Los numeros, y de donde salen</h2>
 *
 * <p>El precio del dia del convenio de los escenarios es <b>47,50</b>, y el convenio tiene cuatro pagas
 * extras, asi que la base reguladora diaria teorica es {@code 47,50 + 4 x 47,50 / 12 = 63,33}
 * ({@code backend#128}). Sobre ella: el 60 % es <b>38,00</b> y el 75 % es <b>47,50</b> — que es
 * exactamente el precio del dia, y eso no es casualidad: {@code 0,75 x 4/3 = 1}. El complemento del
 * convenio, que rellena hasta el 100 % del salario base, vale cero justo en el tramo del 75 %.
 */
@TestWebSobreEsquemaReal
class TheSickLeaveIsPaidByDayTranchesAndTheBaseDoesNotDropTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final LocalDate ENERO_1   = LocalDate.of(2025, 1, 1);
    private static final LocalDate MARZO_25  = LocalDate.of(2025, 3, 25);
    private static final LocalDate ABRIL_6   = LocalDate.of(2025, 4, 6);
    private static final LocalDate ABRIL_10  = LocalDate.of(2025, 4, 10);
    private static final LocalDate ABRIL_11  = LocalDate.of(2025, 4, 11);
    private static final LocalDate ABRIL_15  = LocalDate.of(2025, 4, 15);
    private static final LocalDate MAYO_31   = LocalDate.of(2025, 5, 31);

    private static final BigDecimal PRECIO_DIA = new BigDecimal("47.50");
    private static final BigDecimal BASE_REGULADORA_DIARIA = new BigDecimal("63.33");
    private static final BigDecimal AL_60 = new BigDecimal("38.00");
    private static final BigDecimal AL_75 = new BigDecimal("47.50");

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
     * Una baja de dos dias: se pierden los dos dias de salario y <b>no se cobra nada</b>.
     *
     * <p>Es el tramo 1-3, y las dos normas dicen lo mismo: el art. 173.1 de la LGSS abona el subsidio
     * «a partir del cuarto dia», y el art. 50 del convenio dice que no se percibe «retribucion ni
     * complemento alguno durante los tres primeros dias».
     *
     * <p>Y aun asi <b>se cotiza</b>: durante la baja se cotiza desde el primer dia, asi que la base
     * lleva sus dos dias.
     */
    @Test
    void unaBajaDeDosDiasNoPagaNadaYAunAsiCotiza() {
        String emp = numeroUnico();
        long empId = altaBasica(emp, ENERO_1);
        fixtures.insertAbsence(empId, "IT_COMMON", ABRIL_10, ABRIL_11);

        Long pid = calcular(emp);

        assertImporte(BigDecimal.ZERO, concepto(pid, "110"), "nada a cargo de la empresa");
        assertImporte(BigDecimal.ZERO, concepto(pid, "111"), "nada en pago delegado");
        assertImporte(BigDecimal.ZERO, concepto(pid, "112"), "y nada de complemento del convenio");
        assertImporte(PRECIO_DIA.multiply(new BigDecimal("28")), concepto(pid, "101"),
                "28 dias de salario: los dos de la baja no se devengan");
        assertImporte(BASE_REGULADORA_DIARIA.multiply(new BigDecimal("2")), concepto(pid, "B10"),
                "y los dos dias cotizan: se cotiza desde el primer dia de la baja");
        invarianteDeLaBase(pid);
    }

    /**
     * Una baja de diez dias dentro del mes: siete dias al 60 % a cargo de la empresa, y el complemento
     * del convenio rellenando hasta el salario base.
     */
    @Test
    void unaBajaDeDiezDiasPagaSieteAlSesentaPorCientoACargoDeLaEmpresa() {
        String emp = numeroUnico();
        long empId = altaBasica(emp, ENERO_1);
        fixtures.insertAbsence(empId, "IT_COMMON", ABRIL_6, ABRIL_15);

        Long pid = calcular(emp);

        assertImporte(AL_60.multiply(new BigDecimal("7")), concepto(pid, "110"),
                "dias 4 a 10 de la baja al 60 %: los tres primeros no se pagan y la baja dura diez");
        assertImporte(BigDecimal.ZERO, concepto(pid, "111"),
                "no llega al dia 16, asi que no hay pago delegado");
        assertImporte(PRECIO_DIA.multiply(new BigDecimal("7")).subtract(AL_60.multiply(new BigDecimal("7"))),
                concepto(pid, "112"),
                "el complemento rellena hasta el 100 % del salario base de grupo de esos siete dias");
        assertImporte(BASE_REGULADORA_DIARIA.multiply(new BigDecimal("10")), concepto(pid, "B10"),
                "los diez dias de baja cotizan, tambien los tres que no se pagan");
        invarianteDeLaBase(pid);
    }

    /**
     * Una baja que viene de marzo y ocupa abril entero: los tres tramos en un solo recibo.
     *
     * <p>La baja empieza el 25 de marzo, asi que el 1 de abril lleva <b>siete dias detras</b> y los dias
     * de abril son del 8 al 37 de la baja: ocho al 60 % de la empresa, cinco al 60 % en pago delegado y
     * diecisiete al 75 %. Es el caso que cruza el 15 -> 16 y el 20 -> 21 dentro del mes.
     *
     * <p>Y es el recibo mas distinto de todos: cero dias de salario, cero prorrata, y un total devengado
     * que sale <b>exactamente igual</b> al de un mes trabajado, porque el convenio complementa hasta el
     * 100 % del salario base de grupo.
     */
    @Test
    void unaBajaQueVieneDeMarzoYOcupaAbrilDejaLosTresTramosEnUnRecibo() {
        String emp = numeroUnico();
        long empId = altaBasica(emp, ENERO_1);
        fixtures.insertAbsence(empId, "IT_COMMON", MARZO_25, MAYO_31);

        Long pid = calcular(emp);

        assertImporte(AL_60.multiply(new BigDecimal("8")), concepto(pid, "110"),
                "dias 8 a 15 de la baja: ocho al 60 % y a cargo de la empresa (art. 173.1 LGSS)");
        assertImporte(AL_60.multiply(new BigDecimal("5")).add(AL_75.multiply(new BigDecimal("17"))),
                concepto(pid, "111"),
                "dias 16 a 20 al 60 % y 21 a 37 al 75 %, los dos en pago delegado y en una linea");
        assertImporte(BigDecimal.ZERO, concepto(pid, "101"),
                "ningun dia de salario: la baja ocupa el mes entero");

        assertImporte(PRECIO_DIA.multiply(new BigDecimal("30")), concepto(pid, "970"),
                "y el total devengado es el de un mes entero de salario base de grupo: eso es lo que"
                        + " significa que el convenio complemente hasta el 100 % (art. 50)");

        assertImporte(BASE_REGULADORA_DIARIA.multiply(new BigDecimal("30")), concepto(pid, "B10"),
                "los treinta dias cotizan por la base reguladora");
        invarianteDeLaBase(pid);
    }

    /**
     * Una baja <b>sin derecho a prestacion</b>: los dias se quitan y no hay nada mas.
     *
     * <p>La carencia -180 dias cotizados en cinco anos, art. 172.a) LGSS- no la sabe la nomina: la
     * decide el INSS y llega como un testigo de la baja. Sin el, la baja quita dias, no paga
     * prestacion, <b>no cotiza durante la baja</b> y el recibo lo dice.
     *
     * <p>Es la mitad que hace que las de arriba digan algo: sin este caso, una implementacion que
     * ignorara el testigo pasaria los otros tres.
     */
    @Test
    void unaBajaSinDerechoAPrestacionSoloQuitaDias() {
        String emp = numeroUnico();
        long empId = altaBasica(emp, ENERO_1);
        fixtures.insertAbsenceWithoutBenefit(empId, "IT_COMMON", ABRIL_6, ABRIL_15);

        Long pid = calcular(emp);

        assertImporte(PRECIO_DIA.multiply(new BigDecimal("20")), concepto(pid, "101"),
                "los diez dias de baja se quitan igual: eso no depende del derecho");
        assertImporte(BigDecimal.ZERO, concepto(pid, "110"));
        assertImporte(BigDecimal.ZERO, concepto(pid, "111"));
        assertImporte(BigDecimal.ZERO, concepto(pid, "112"));
        assertImporte(BigDecimal.ZERO, concepto(pid, "B10"),
                "y sin derecho tampoco hay base durante la baja");
        invarianteDeLaBase(pid);
    }

    /** Y una vacacion no toca nada de esto, que es lo que dejo dicho el {@code backend#127}. */
    @Test
    void unasVacacionesNoPaganPrestacionNiCotizanAparte() {
        String emp = numeroUnico();
        long empId = altaBasica(emp, ENERO_1);
        fixtures.insertAbsence(empId, "VACATION", ABRIL_6, ABRIL_15);

        Long pid = calcular(emp);

        assertImporte(PRECIO_DIA.multiply(new BigDecimal("30")), concepto(pid, "101"),
                "las vacaciones se cobran enteras");
        assertImporte(BigDecimal.ZERO, concepto(pid, "110"));
        assertImporte(BigDecimal.ZERO, concepto(pid, "B10"));
        invarianteDeLaBase(pid);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * La invariante del paso 5: {@code B01 = B03 + B04 + B10}.
     *
     * <p>La base de cotizacion es la remuneracion mensual mas la prorrata mas la base durante la baja, y
     * lo es <b>por construccion</b>: los tres alimentan {@code B01} y nadie mas lo hace. Se comprueba en
     * todos los casos de este test, tambien en los que no tienen baja, porque una invariante que solo se
     * comprueba donde se espera que valga no es una invariante.
     */
    private void invarianteDeLaBase(Long payrollId) {
        BigDecimal b01 = concepto(payrollId, "B01");
        BigDecimal suma = concepto(payrollId, "B03")
                .add(concepto(payrollId, "B04"))
                .add(concepto(payrollId, "B10"));
        assertEquals(0, b01.compareTo(suma),
                "B01 = B03 + B04 + B10 (esperado " + suma + ", fue " + b01 + ")");
    }

    private long altaBasica(String emp, LocalDate desde) {
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, desde, null);
        fixtures.insertLaborClassification(empId, desde);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), desde, null);
        return empId;
    }

    private Long calcular(String emp) {
        CalculationRun run = launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, emp),
                        null),
                null));
        assertEquals("COMPLETED", run.status());
        entityManager.flush();
        Long pid = jdbc.queryForObject(
                "select id from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = 1",
                Long.class, RULE_SYSTEM, EMPLOYEE_TYPE, emp, PERIOD, PAYROLL_TYPE);
        assertEquals("CALCULATED", jdbc.queryForObject(
                "select status from payroll.payroll where id = ?", String.class, pid));
        return pid;
    }

    /**
     * El importe compuesto de un concepto: la suma de sus pasos.
     *
     * <p>Se lee de los pasos y no de las lineas del recibo a proposito. Las lineas no estan cuando el
     * importe vale cero -la regla del cero del {@code backend#104}- y aqui hay que poder afirmar
     * exactamente eso: que el {@code 110} vale cero en una baja de dos dias.
     */
    private BigDecimal concepto(Long payrollId, String conceptCode) {
        BigDecimal total = jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from payroll.payroll_calculation_step"
                        + " where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, conceptCode);
        assertTrue(total != null, conceptCode + " tiene que haberse calculado");
        return total;
    }

    /** Las lineas del recibo, en orden, por si hace falta ver el folio entero al depurar. */
    @SuppressWarnings("unused")
    private List<String> lineasDelRecibo(Long payrollId) {
        return jdbc.queryForList(
                "select concept_code || ' ' || amount from payroll.payroll_concept"
                        + " where payroll_id = ? order by display_order",
                String.class, payrollId);
    }

    private static void assertImporte(BigDecimal esperado, BigDecimal real) {
        assertImporte(esperado, real, "");
    }

    private static void assertImporte(BigDecimal esperado, BigDecimal real, String porque) {
        assertEquals(0, esperado.compareTo(real),
                porque + " (esperado " + esperado + ", fue " + real + ")");
    }

    private String numeroUnico() {
        return "IT" + (System.nanoTime() % 1_000_000_000L);
    }
}
