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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Una linea del folio que funde varios pasos lo dice, y se puede llegar de una a otros
 * ({@code backend#103}).
 *
 * <h2>El caso que nadie acierta por casualidad</h2>
 *
 * <p>El issue lo plantea con cuatro tramos donde un cambio de categoria y uno de jornada se
 * compensan, de modo que <b>dos tramos no contiguos</b> acaban al mismo precio y el folio los funde
 * en una linea. La categoria todavia no rompe el periodo —eso es el {@code backend#47}— asi que aqui
 * el mismo caso se monta con lo unico que hoy lo rompe: cuatro ventanas de jornada,
 * {@code 100 / 50 / 100 / 50}.
 *
 * <p>Da la misma forma, que es lo que importa: cuatro tramos, dos precios, y <b>los tramos 1 y 3 al
 * mismo precio sin ser contiguos</b>. El folio saca dos lineas donde el motor dio cuatro pasos, y
 * hasta este issue nada decia que cada una fuera una suma. Lo que este escenario no reproduce es que
 * la coincidencia venga de <b>causas distintas</b>; eso llega con el {@code backend#47} y el enlace
 * ya estara puesto.
 */
@TestWebSobreEsquemaReal
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AMergedPayslipLineSaysWhichStepsItMergesTest {

    private static final String RULE_SYSTEM = "MRG";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD = "202501";
    private static final String PAYROLL_TYPE = "NORMAL";
    private static final LocalDate PERIOD_START = LocalDate.of(2025, 1, 1);

    @Autowired
    private LaunchPayrollCalculationUseCase launchPayrollCalculationUseCase;

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
    }

    @Test
    void fourSegmentsAtTwoPricesBecomeTwoLinesThatSayWhatTheyMerge() {
        String employee = hireWithFourWindows();
        launch(employee);

        List<Map<String, Object>> pasos = steps(employee, "101");
        assertEquals(4, pasos.size(), "el escenario necesita cuatro tramos: " + pasos);

        List<Map<String, Object>> lineas = payslipLines(employee, "101");
        assertEquals(2, lineas.size(),
                "cuatro tramos a dos precios tienen que dar dos lineas: " + lineas);

        // Cada linea dice de cuantos pasos viene, y son dos y dos.
        for (Map<String, Object> linea : lineas) {
            assertEquals(2, ((Number) linea.get("merged_step_count")).intValue(),
                    "cada linea funde dos tramos: " + lineas);
        }

        // Y de la linea se llega a sus pasos sin reconstruir ninguna agrupacion.
        for (Map<String, Object> linea : lineas) {
            int lineNumber = ((Number) linea.get("line_number")).intValue();
            List<Map<String, Object>> suyos = pasos.stream()
                    .filter(p -> lineNumber == ((Number) p.get("payslip_line_number")).intValue())
                    .toList();
            assertEquals(2, suyos.size(), "la linea " + lineNumber + " tiene que tener dos pasos: " + pasos);

            // Los dos a la misma tarifa —es lo que los funde— y la suma es el importe de la linea.
            assertEquals(1, suyos.stream().map(p -> p.get("rate")).distinct().count(),
                    "los pasos de una linea comparten tarifa: " + suyos);
            BigDecimal suma = suyos.stream()
                    .map(p -> (BigDecimal) p.get("amount"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, suma.compareTo((BigDecimal) linea.get("amount")),
                    "la linea es la suma de sus pasos: " + suyos + " vs " + linea);
        }

        // Y el caso del issue: los dos tramos de una de las lineas NO son contiguos.
        List<Map<String, Object>> primeraLinea = pasos.stream()
                .filter(p -> 1 == ((Number) p.get("payslip_line_number")).intValue())
                .toList();
        LocalDate finDelPrimero = ((java.sql.Date) primeraLinea.get(0).get("segment_end_date")).toLocalDate();
        LocalDate inicioDelSegundo = ((java.sql.Date) primeraLinea.get(1).get("segment_start_date")).toLocalDate();
        assertTrue(inicioDelSegundo.isAfter(finDelPrimero.plusDays(1)),
                "los dos tramos de esta linea tienen que NO ser contiguos, y son "
                        + finDelPrimero + " -> " + inicioDelSegundo);
    }

    /**
     * El criterio 5, que es el que se olvida: <b>un recibo sin fusiones no ensena ninguna marca.</b>
     *
     * <p>Una marca que sale siempre no marca nada. Con una sola ventana de jornada hay un tramo por
     * concepto y cada linea viene de un paso: {@code mergedStepCount} vale uno en todas, que es lo
     * que el cliente mira para no pintar nada.
     */
    @Test
    void aReceiptWithNoMergesShowsNoMark() {
        String employee = hireWithOneWindow();
        launch(employee);

        List<Map<String, Object>> lineas = payslipLines(employee, null);
        assertTrue(lineas.size() >= 5, "el recibo tiene que tener lineas: " + lineas);
        assertEquals(List.of(1), lineas.stream()
                        .map(l -> ((Number) l.get("merged_step_count")).intValue())
                        .distinct()
                        .toList(),
                "ninguna linea funde nada, asi que todas valen 1: " + lineas);

        // Y cada paso impreso apunta a una linea distinta: no hay dos compartiendo numero.
        List<Map<String, Object>> impresos = jdbcTemplate.queryForList("""
                select s.concept_code, s.payslip_line_number
                  from payroll.payroll_calculation_step s
                  join payroll.payroll p on p.id = s.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ?
                   and s.payslip_order_code is not null
                """, RULE_SYSTEM, employee);
        assertEquals(impresos.size(),
                impresos.stream().map(i -> i.get("payslip_line_number")).distinct().count(),
                "sin fusiones, cada paso impreso va a su propia linea: " + impresos);
    }

    /**
     * Y un paso con linea es siempre un paso que se imprime.
     *
     * <p>La implicacion va en un solo sentido, y el otro se cayo con el {@code backend#104}: desde
     * la regla del cero, un paso puede tener orden de folio y <b>no</b> tener linea, porque su linea
     * valia cero y no se imprimio. Lo que no puede pasar nunca es lo contrario — una linea apuntada
     * por un paso que el folio no conoce.
     */
    @Test
    void aStepWithALineIsAlwaysAStepThatGetsPrinted() {
        String employee = hireWithOneWindow();
        launch(employee);

        List<Map<String, Object>> descuadres = jdbcTemplate.queryForList("""
                select s.concept_code, s.payslip_order_code, s.payslip_line_number
                  from payroll.payroll_calculation_step s
                  join payroll.payroll p on p.id = s.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ?
                   and s.payslip_line_number is not null
                   and s.payslip_order_code is null
                """, RULE_SYSTEM, employee);
        assertTrue(descuadres.isEmpty(),
                "un paso con linea tiene que tener orden de folio: " + descuadres);
    }

    private String hireWithFourWindows() {
        String employeeNumber = "MG" + (System.nanoTime() % 1_000_000_000L);
        long employeeId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber);
        fixtures.insertPresence(employeeId, 1, PERIOD_START, null);
        fixtures.insertLaborClassification(employeeId, PERIOD_START);
        // 100 / 50 / 100 / 50: dos precios, y los tramos 1 y 3 al mismo sin ser contiguos.
        fixtures.insertWorkingTime(employeeId, new BigDecimal("100.00"),
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 10));
        fixtures.insertWorkingTime(employeeId, new BigDecimal("50.00"),
                LocalDate.of(2025, 1, 11), LocalDate.of(2025, 1, 15));
        fixtures.insertWorkingTime(employeeId, new BigDecimal("100.00"),
                LocalDate.of(2025, 1, 16), LocalDate.of(2025, 1, 22));
        fixtures.insertWorkingTime(employeeId, new BigDecimal("50.00"),
                LocalDate.of(2025, 1, 23), null);
        return employeeNumber;
    }

    private String hireWithOneWindow() {
        String employeeNumber = "MG" + (System.nanoTime() % 1_000_000_000L);
        long employeeId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber);
        fixtures.insertPresence(employeeId, 1, PERIOD_START, null);
        fixtures.insertLaborClassification(employeeId, PERIOD_START);
        fixtures.insertWorkingTime(employeeId, new BigDecimal("100.00"), PERIOD_START, null);
        return employeeNumber;
    }

    private List<Map<String, Object>> steps(String employee, String conceptCode) {
        return jdbcTemplate.queryForList("""
                select s.execution_order, s.concept_code, s.segment_start_date, s.segment_end_date,
                       s.rate, s.amount, s.payslip_line_number
                  from payroll.payroll_calculation_step s
                  join payroll.payroll p on p.id = s.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ? and s.concept_code = ?
                 order by s.execution_order
                """, RULE_SYSTEM, employee, conceptCode);
    }

    private List<Map<String, Object>> payslipLines(String employee, String conceptCode) {
        if (conceptCode == null) {
            return jdbcTemplate.queryForList("""
                    select c.line_number, c.concept_code, c.amount, c.merged_step_count
                      from payroll.payroll_concept c
                      join payroll.payroll p on p.id = c.payroll_id
                     where p.rule_system_code = ? and p.employee_number = ?
                     order by c.line_number
                    """, RULE_SYSTEM, employee);
        }
        return jdbcTemplate.queryForList("""
                select c.line_number, c.concept_code, c.amount, c.merged_step_count
                  from payroll.payroll_concept c
                  join payroll.payroll p on p.id = c.payroll_id
                 where p.rule_system_code = ? and p.employee_number = ? and c.concept_code = ?
                 order by c.line_number
                """, RULE_SYSTEM, employee, conceptCode);
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
}
