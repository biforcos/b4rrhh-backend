package com.b4rrhh.payroll.scenario;

import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Un cambio de categoría de convenio a mitad de mes parte el período, y uno de contrato también
 * ({@code backend#47}).
 *
 * <p>Hasta aquí lo partía <b>sólo</b> la jornada. Un cambio de categoría el día 16 no creaba
 * segmento: el precio del día se resolvía una vez para todo el mes —con la categoría del último
 * tramo— y se aplicaba a los quince días que se pagaron a otro precio. <b>No es que estuviera
 * desactivado: es que no existía el mecanismo.</b>
 *
 * <p>Lo que este test sujeta, y que no sujeta ningún otro:
 *
 * <ul>
 *   <li><b>Que el precio de cada tramo sea el suyo.</b> Es lo que hace visible el fallo: con un
 *       solo segmento el recibo trae un número redondo que parece correcto, y sólo se ve que miente
 *       comparándolo con los dos tramos a mano.
 *   <li><b>Que el folio no funda los dos tramos.</b> El colapso agrupa por {@code concepto|tarifa},
 *       así que dos precios distintos son dos líneas — y eso es justo lo que el colapso está
 *       pensado para proteger.
 *   <li><b>Que el contrato también parta</b>, aunque hoy no lo lea ningún concepto. Ahí los dos
 *       tramos valen lo mismo, el folio los vuelve a juntar en una línea, y lo único que queda es
 *       un paso de más en la pestaña de cálculo. Es lo que se paga hoy por que el día que algún
 *       concepto lea el contrato ya esté partido.
 * </ul>
 *
 * <p>Sobre TST y no sobre ESP porque hace falta una segunda categoría con otro precio, y la siembra
 * de ESP trae las suyas: fabricarla allí sería tocar la reglamentación real para hacer un test.
 */
@TestWebSobreEsquemaReal
class AMidMonthChangeOfCategoryOrContractSplitsThePeriodIntegrationTest {

    private static final String RULE_SYSTEM = "TST";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD = "202501";

    private static final LocalDate ENERO_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate ENERO_15 = LocalDate.of(2025, 1, 15);
    private static final LocalDate ENERO_16 = LocalDate.of(2025, 1, 16);

    /** La categoría de la siembra, a 47,50 el día. */
    private static final String CATEGORIA_BASE = "99002405-G2";
    private static final BigDecimal PRECIO_BASE = new BigDecimal("47.50");

    /** Y la segunda, que es a lo que asciende el día 16. */
    private static final String CATEGORIA_NUEVA = "99002405-G9";
    private static final BigDecimal PRECIO_NUEVO = new BigDecimal("60.00");

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
        fixtures.seedConceptGraph(RULE_SYSTEM);
        fixtures.seedAgreementCategory(RULE_SYSTEM, CATEGORIA_NUEVA, PRECIO_NUEVO);
    }

    @Test
    void unCambioDeCategoriaAMitadDeMesParteElPeriodoYCadaTramoCobraASuPrecio() {
        String emp = contratar();
        fixtures.insertLaborClassification(empIdDe(emp), ENERO_1, ENERO_15, CATEGORIA_BASE);
        fixtures.insertLaborClassification(empIdDe(emp), ENERO_16, null, CATEGORIA_NUEVA);

        calcular(emp);

        List<Map<String, Object>> salario = pasosDe(emp, "101");
        assertEquals(2, salario.size(), "el cambio de categoria parte el periodo en dos");

        // Cada tramo a su precio: 15 dias a 47,50 y 16 a 60,00. Con un solo segmento los 31 dias
        // salian al precio del ultimo tramo, que son 240 euros de mas y ninguna senal de que lo
        // fueran.
        assertEquals(ENERO_15, ((java.sql.Date) salario.get(0).get("segment_end_date")).toLocalDate());
        assertEquals(0, PRECIO_BASE.compareTo((BigDecimal) salario.get(0).get("rate")));
        assertEquals(0, new BigDecimal("712.50").compareTo((BigDecimal) salario.get(0).get("amount")));

        assertEquals(ENERO_16, ((java.sql.Date) salario.get(1).get("segment_start_date")).toLocalDate());
        assertEquals(0, PRECIO_NUEVO.compareTo((BigDecimal) salario.get(1).get("rate")));
        assertEquals(0, new BigDecimal("960.00").compareTo((BigDecimal) salario.get(1).get("amount")));

        // Y el folio no los funde: el colapso agrupa por concepto|tarifa, y las tarifas son dos.
        List<Map<String, Object>> lineas = lineasDe(emp, "101");
        assertEquals(2, lineas.size(), "dos precios son dos lineas de recibo");
        assertNotEquals(lineas.get(0).get("rate"), lineas.get(1).get("rate"));
    }

    /**
     * Y cada tramo dice de qué fila salió su precio ({@code backend#107}), que es la otra mitad de
     * lo mismo: sin partir el período había una sola fila para todo el mes, y era la del tramo que
     * no se estaba cobrando la mitad de los días.
     */
    @Test
    void cadaTramoGuardaLaFilaDeTablaDeSuCategoria() {
        String emp = contratar();
        fixtures.insertLaborClassification(empIdDe(emp), ENERO_1, ENERO_15, CATEGORIA_BASE);
        fixtures.insertLaborClassification(empIdDe(emp), ENERO_16, null, CATEGORIA_NUEVA);

        calcular(emp);

        List<Map<String, Object>> precioDia = pasosDe(emp, "P02");
        assertEquals(2, precioDia.size(), "el precio dia se evalua una vez por tramo");
        Object filaPrimera = precioDia.get(0).get("source_table_row_id");
        Object filaSegunda = precioDia.get(1).get("source_table_row_id");
        assertNotNull(filaPrimera);
        assertNotNull(filaSegunda);
        assertNotEquals(filaPrimera, filaSegunda, "dos categorias son dos filas de tabla");
    }

    /**
     * El contrato parte el período, y el salario de los dos tramos vale lo mismo, así que el folio
     * los vuelve a juntar en una línea: el recibo enseña una y el cálculo dos pasos.
     *
     * <p>Los códigos eran {@code IND} y {@code TMP}, inventados, y valían porque nadie leía el
     * contrato. Desde el {@code backend#124} sí se lee —de él sale la modalidad de desempleo— y un
     * código que no está en el catálogo para la corrida. Ahora son el 100 y el 401, que es además
     * el cambio que de verdad mueve algo: de indefinido a duración determinada.
     */
    @Test
    void unCambioDeContratoAMitadDeMesTambienParte_yElFolioLosVuelveAJuntar() {
        String emp = contratar();
        fixtures.insertLaborClassification(empIdDe(emp), ENERO_1, null, CATEGORIA_BASE);
        fixtures.insertContract(empIdDe(emp), ENERO_1, ENERO_15, "100");
        fixtures.insertContract(empIdDe(emp), ENERO_16, null, "401");

        calcular(emp);

        assertEquals(2, pasosDe(emp, "101").size(), "el cambio de contrato parte el periodo");
        assertEquals(1, lineasDe(emp, "101").size(),
                "y el folio los funde: mismo concepto y mismo precio es una linea");
    }

    /** El control: sin ningún cambio, un segmento y una línea, como siempre. */
    @Test
    void sinCambiosSigueHabiendoUnSoloSegmento() {
        String emp = contratar();
        fixtures.insertLaborClassification(empIdDe(emp), ENERO_1, null, CATEGORIA_BASE);

        calcular(emp);

        assertEquals(1, pasosDe(emp, "101").size());
        assertEquals(1, lineasDe(emp, "101").size());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String contratar() {
        String emp = "SG" + (System.nanoTime() % 1_000_000_000L);
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, ENERO_1, null);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), ENERO_1, null);
        return emp;
    }

    private long empIdDe(String employeeNumber) {
        Long id = jdbc.queryForObject(
                "select id from employee.employee where rule_system_code = ? and employee_type_code = ? and employee_number = ?",
                Long.class, RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber);
        assertNotNull(id);
        return id;
    }

    private void calcular(String employeeNumber) {
        assertEquals("COMPLETED", launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, PERIOD, "NORMAL", "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employeeNumber),
                        null),
                null)).status());
        // Los pasos llevan clave asignada y su insert queda diferido hasta el vaciado de la sesion
        // (ADR-062): sin esto, contarlos por JDBC dentro de la transaccion del test daria cero.
        entityManager.flush();
    }

    private List<Map<String, Object>> pasosDe(String employeeNumber, String conceptCode) {
        return jdbc.queryForList(
                "select s.* from payroll.payroll_calculation_step s"
                        + " join payroll.payroll p on p.id = s.payroll_id"
                        + " where p.employee_number = ? and s.concept_code = ?"
                        + " order by s.execution_order",
                employeeNumber, conceptCode);
    }

    private List<Map<String, Object>> lineasDe(String employeeNumber, String conceptCode) {
        return jdbc.queryForList(
                "select c.* from payroll.payroll_concept c"
                        + " join payroll.payroll p on p.id = c.payroll_id"
                        + " where p.employee_number = ? and c.concept_code = ?"
                        + " order by c.line_number",
                employeeNumber, conceptCode);
    }
}
