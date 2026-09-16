package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.scenario.PayrollScenarioFixtures;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las dos propiedades de redondeo del concepto, y el invariante que las acompana (backend#61).
 *
 * <h2>Por que estos tests son la condicion y no un extra</h2>
 *
 * Ya metimos una propiedad de calculo en el catalogo de conceptos y hubo que quitarla:
 * {@code result_composition_mode} con su {@code ACCUMULATE} (V90), retirada en la V120 por
 * <b>inerte</b> — estaba en el esquema y no la leia nadie. La condicion que el issue pone para que
 * estas dos entren es la que aquella no tuvo: <b>cada una se lee desde el primer dia, y hay un test
 * que demuestra que cambiar su valor cambia un resultado.</b>
 *
 * <p>Por eso los dos primeros tests no comprueban que la propiedad se guarde ni que se lea: calculan
 * el mismo recibo dos veces con dos valores y comparan los importes. Una propiedad que se lee y no
 * cambia nada seria igual de inerte que la V90.
 *
 * <h2>El escenario</h2>
 *
 * Un mes partido, que es donde el redondeo se ve: del 1 al 15 a jornada completa y del 16 al 31 a
 * media, que son <b>dieciseis</b> dias en el segundo tramo.
 * El precio de la categoria es <b>61,67</b>, asi que la media jornada da {@code 30,835} — el numero
 * exacto que el issue persigue, y el unico de la semilla que no cae redondo.
 */
@TestWebSobreEsquemaReal
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConceptRoundingGovernsTheAmountTest {

    private static final String RULE_SYSTEM = "RND";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD = "202501";
    private static final String PAYROLL_TYPE = "NORMAL";
    private static final LocalDate PERIOD_START = LocalDate.of(2025, 1, 1);
    private static final LocalDate MEDIA_JORNADA_DESDE = LocalDate.of(2025, 1, 16);
    /** El precio impar: 0,5 x 61,67 = 30,835. */
    private static final BigDecimal PRECIO_CATEGORIA = new BigDecimal("61.67");

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
            fixtures.setDailyRate(RULE_SYSTEM, PRECIO_CATEGORIA);
        }
        // Cada test parte del defecto: 2 y HALF_UP, que es lo que el motor hacia antes del #61.
        fixtures.setConceptRounding(RULE_SYSTEM, "P01", 2, "HALF_UP");
        fixtures.setConceptRounding(RULE_SYSTEM, "101", 2, "HALF_UP");
    }

    /**
     * Primera propiedad: <b>los decimales</b>.
     *
     * <p>Es la decision que el issue dejo abierta y que la medicion sobre los 873 recibos cerro. Con
     * el precio a dos decimales, los dieciseis dias de media jornada se cobran a 30,84; con seis, a
     * 30,835. Ocho centimos en este tramo, y el numero que se paga es el segundo.
     */
    @Test
    void changingTheNumberOfDecimalsChangesTheAmount() {
        String employee = hireWithSplitMonth();
        launch(employee);

        BigDecimal conDos = lineaDelSegundoTramo(employee);
        assertEquals(0, new BigDecimal("493.44").compareTo(conDos),
                "con P01 a 2 decimales, 16 x 30,84: " + lineas(employee));

        fixtures.setConceptRounding(RULE_SYSTEM, "P01", 6, "HALF_UP");
        recalculate(employee);

        BigDecimal conSeis = lineaDelSegundoTramo(employee);
        assertEquals(0, new BigDecimal("493.36").compareTo(conSeis),
                "con P01 a 6 decimales, 16 x 30,835 = 493,36 exacto: " + lineas(employee));

        assertNotEquals(0, conDos.compareTo(conSeis),
                "si estos dos son iguales, la propiedad no la lee nadie y es la V90 otra vez");
    }

    /**
     * Segunda propiedad: <b>el modo</b>.
     *
     * <p>Y se mide donde el modo decide de verdad: {@code 30,835} a dos decimales es justo un empate.
     * {@code HALF_UP} lo sube a 30,84 y {@code DOWN} lo deja en 30,83, que son dieciseis centimos de
     * diferencia al mes por los dieciseis dias del tramo.
     */
    @Test
    void changingTheRoundingModeChangesTheAmount() {
        String employee = hireWithSplitMonth();
        launch(employee);

        BigDecimal conHalfUp = lineaDelSegundoTramo(employee);
        assertEquals(0, new BigDecimal("493.44").compareTo(conHalfUp),
                "HALF_UP sube el empate: 30,835 -> 30,84: " + lineas(employee));

        fixtures.setConceptRounding(RULE_SYSTEM, "P01", 2, "DOWN");
        recalculate(employee);

        BigDecimal conDown = lineaDelSegundoTramo(employee);
        assertEquals(0, new BigDecimal("493.28").compareTo(conDown),
                "DOWN lo deja en 30,83: " + lineas(employee));

        assertNotEquals(0, conHalfUp.compareTo(conDown),
                "si estos dos son iguales, el modo no lo lee nadie");
    }

    /**
     * El invariante: <b>nada se redondea dos veces</b>.
     *
     * <p>Es lo que hace que no exista el problema del residuo. El valor de periodo de un concepto
     * {@code SEGMENT} es la suma de sus valores de tramo <b>ya redondeados</b>, y nadie vuelve a
     * redondear encima; con eso la suma de las partes <b>es</b> el total por construccion, y no hay
     * centimos sobrantes que repartir a la ultima linea ni conceptos de ajuste que inventar.
     *
     * <h3>Dos condiciones para que esto mida algo, y las dos costaron un sabotaje</h3>
     *
     * <p><b>1. La suma tiene que llevar mas de dos decimales</b>, o un segundo redondeo a dos no se
     * notaria. Por eso el salario y el precio se declaran con <b>seis</b>, y por eso la jornada del
     * segundo tramo es un tercio: con media jornada, {@code 16 x 30,835} da {@code 493,36} exacto y
     * el test pasaria sin comprobar nada.
     *
     * <p><b>2. Los dos tramos tienen que tener el MISMO precio.</b> Esta la encontro un sabotaje que
     * salio verde: {@code collapsePayslipRows} agrupa por {@code concepto|tarifa}, asi que con dos
     * precios distintos las dos lineas no se fusionan, la funcion de fusion <b>no llega a
     * ejecutarse</b> y un redondeo puesto ahi no lo ve nadie. Con la version anterior de este test

     * —jornada completa y luego un tercio— el sabotaje pasaba. Por eso las dos ventanas llevan la
     * misma jornada y lo que las parte es la fecha: mismo precio, dos tramos, una sola linea.
     */
    @Test
    void nothingIsRoundedTwice() {
        String employee = hireWithTwoWindowsAtTheSameRate(new BigDecimal("33.33"));
        fixtures.setConceptRounding(RULE_SYSTEM, "J01", 6, "HALF_UP");
        fixtures.setConceptRounding(RULE_SYSTEM, "P01", 6, "HALF_UP");
        fixtures.setConceptRounding(RULE_SYSTEM, "101", 6, "HALF_UP");
        launch(employee);

        List<BigDecimal> pasos = jdbcTemplate.queryForList("""
                select s.amount
                  from payroll.payroll_calculation_step s
                  join payroll.payroll p on p.id = s.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ?
                   and s.concept_code = '101'
                 order by s.execution_order
                """, BigDecimal.class, RULE_SYSTEM, employee);

        assertEquals(2, pasos.size(), "el escenario necesita dos tramos");
        BigDecimal sumaExacta = pasos.get(0).add(pasos.get(1));

        // Que la suma tenga decimales que un segundo redondeo se comeria es la condicion para que
        // este test signifique algo. Si algun dia deja de cumplirse, el test avisa en vez de pasar.
        assertTrue(sumaExacta.stripTrailingZeros().scale() > 2,
                "la suma de los tramos tiene que llevar mas de dos decimales para que esto mida algo,"
                        + " y lleva " + sumaExacta.stripTrailingZeros().scale() + ": " + pasos);

        List<BigDecimal> lineasDelRecibo = jdbcTemplate.queryForList("""
                select c.amount
                  from payroll.payroll_concept c
                  join payroll.payroll p on p.id = c.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ? and c.concept_code = '101'
                """, BigDecimal.class, RULE_SYSTEM, employee);

        // Que sea UNA linea es parte de la comprobacion: si salieran dos, el folio no habria
        // fusionado nada y la igualdad de abajo se cumpliria por no haber hecho nada.
        assertEquals(1, lineasDelRecibo.size(),
                "los dos tramos van al mismo precio y tienen que fundirse en una linea: " + lineasDelRecibo);
        BigDecimal lineaDelRecibo = lineasDelRecibo.get(0);

        assertEquals(0, sumaExacta.compareTo(lineaDelRecibo),
                "la linea del recibo tiene que ser la suma EXACTA de sus pasos, sin redondear otra vez:"
                        + " pasos=" + pasos + " suma=" + sumaExacta + " linea=" + lineaDelRecibo);

        // Y el total de devengos: se redondea una vez, al agregarlo, y no antes.
        BigDecimal totalDevengos = jdbcTemplate.queryForObject("""
                select s.amount
                  from payroll.payroll_calculation_step s
                  join payroll.payroll p on p.id = s.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ? and s.concept_code = '970'
                """, BigDecimal.class, RULE_SYSTEM, employee);

        assertEquals(0, sumaExacta.setScale(2, java.math.RoundingMode.HALF_UP).compareTo(totalDevengos),
                "el total es la suma de los tramos redondeada UNA vez, con los decimales del total:"
                        + " suma=" + sumaExacta + " total=" + totalDevengos);
    }

    private String hireWithSplitMonth() {
        return hireWithSplitMonth(new BigDecimal("50.00"));
    }

    /**
     * Dos ventanas de jornada con el <b>mismo</b> porcentaje: el periodo se parte igual —lo parte la
     * frontera de las ventanas, no su valor— pero los dos tramos salen al mismo precio, que es lo
     * que hace que el folio los fusione en una linea.
     */
    private String hireWithTwoWindowsAtTheSameRate(BigDecimal jornada) {
        String employeeNumber = "RN" + (System.nanoTime() % 1_000_000_000L);
        long employeeId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber);
        fixtures.insertPresence(employeeId, 1, PERIOD_START, null);
        fixtures.insertLaborClassification(employeeId, PERIOD_START);
        fixtures.insertWorkingTime(employeeId, jornada, PERIOD_START, MEDIA_JORNADA_DESDE.minusDays(1));
        fixtures.insertWorkingTime(employeeId, jornada, MEDIA_JORNADA_DESDE, null);
        return employeeNumber;
    }

    private String hireWithSplitMonth(BigDecimal jornadaDelSegundoTramo) {
        String employeeNumber = "RN" + (System.nanoTime() % 1_000_000_000L);
        long employeeId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber);
        fixtures.insertPresence(employeeId, 1, PERIOD_START, null);
        fixtures.insertLaborClassification(employeeId, PERIOD_START);
        fixtures.insertWorkingTime(employeeId, new BigDecimal("100.00"),
                PERIOD_START, MEDIA_JORNADA_DESDE.minusDays(1));
        fixtures.insertWorkingTime(employeeId, jornadaDelSegundoTramo,
                MEDIA_JORNADA_DESDE, null);
        return employeeNumber;
    }

    /** El importe del tramo de media jornada, que es el que el redondeo mueve. */
    private BigDecimal lineaDelSegundoTramo(String employee) {
        return jdbcTemplate.queryForObject("""
                select s.amount
                  from payroll.payroll_calculation_step s
                  join payroll.payroll p on p.id = s.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ?
                   and s.concept_code = '101' and s.segment_start_date = ?
                """, BigDecimal.class, RULE_SYSTEM, employee, MEDIA_JORNADA_DESDE);
    }

    private String lineas(String employee) {
        List<Map<String, Object>> filas = jdbcTemplate.queryForList("""
                select s.concept_code, s.segment_start_date, s.quantity, s.rate, s.amount
                  from payroll.payroll_calculation_step s
                  join payroll.payroll p on p.id = s.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ?
                   and s.concept_code in ('J01','P01','101','970')
                 order by s.execution_order
                """, RULE_SYSTEM, employee);
        return filas.toString();
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
