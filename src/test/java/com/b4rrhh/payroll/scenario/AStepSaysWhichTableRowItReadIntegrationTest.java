package com.b4rrhh.payroll.scenario;

import com.b4rrhh.payroll.application.usecase.InvalidatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.InvalidatePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.application.usecase.RecalculatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.RecalculatePayrollUseCase;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un paso dice de qué fila de tabla leyó su valor, y lo dice porque lo guardó ({@code backend#107}).
 *
 * <p>Sobre ESP y no sobre TST: la cadena que se prueba —{@code P02} alimentado por una tabla, la
 * tabla atada al convenio por una vinculación, y la fila elegida por categoría y vigencia— es la
 * que siembran las migraciones, y montarla a mano en un fixture probaría el fixture.
 *
 * <p>Lo que este test sujeta y no sujeta ningún otro:
 *
 * <ul>
 *   <li><b>Que el nulo signifique algo.</b> De los 38 pasos de un recibo ESP leen una fila dos. Si
 *       el resto trajera direcciones inventadas, la pantalla ofrecería saltos que no llevan a
 *       ninguna parte, que es peor que no ofrecer ninguno.
 *   <li><b>Que esté <i>guardado</i> y no resuelto al leer.</b> Es toda la diferencia, y sólo se ve
 *       cuando la búsqueda deja de contestar lo mismo: se cierra la vigencia de la fila leída y se
 *       abre otra. Un recibo recalculado después apunta a la nueva; el que no se recalculó sigue
 *       apuntando a la suya, que hoy ni siquiera se elegiría. Reconstruyendo la búsqueda los dos
 *       dirían lo mismo, y uno de los dos estaría mintiendo.
 * </ul>
 */
@TestWebSobreEsquemaReal
class AStepSaysWhichTableRowItReadIntegrationTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final LocalDate JANUARY_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate MARCH_31  = LocalDate.of(2025, 3, 31);
    private static final LocalDate APRIL_1   = LocalDate.of(2025, 4, 1);

    /** El concepto cuyo valor sale de la tabla salarial del convenio: el precio día pleno. */
    private static final String TABLE_FED_CONCEPT = "P02";

    /**
     * Y los dos del catálogo ESP que leen una fila, que no es uno.
     *
     * <p>Aquí decía {@code P02} a secas, y el árbol lo desmintió al primer intento: el
     * {@code backend#104} declaró las horas extra y con ellas {@code P03}, el precio de la hora,
     * que también sale de una tabla. Son dos lecturas de tabla en un recibo de 38 pasos, y el
     * recuento va escrito porque es lo que distingue «el motor anota lo que lee» de «el motor
     * anota algo».
     */
    private static final List<String> TABLE_FED_CONCEPTS = List.of("P02", "P03");

    private static final String TABLE_CODE    = "P02_99002405011982";
    private static final String CATEGORY_CODE = "99002405-G2";

    private static final String STEPS_URL =
            "/payrolls/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}"
                    + "/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}/steps";

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LaunchPayrollCalculationUseCase launch;

    @Autowired
    private RecalculatePayrollUseCase recalculateUseCase;

    @Autowired
    private InvalidatePayrollUseCase invalidateUseCase;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    private PayrollScenarioFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new PayrollScenarioFixtures(jdbc);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void theStepFedByATableCarriesItsRow_andEveryOtherStepCarriesNone() throws Exception {
        long filaVigente = rowIdValidOn(APRIL_1);

        String emp = hireWholeMonth();
        calculate(emp);

        List<Map<String, Object>> steps = getSteps(emp);

        Map<String, Object> precioDia = stepOf(steps, TABLE_FED_CONCEPT);
        assertEquals(TABLE_CODE, precioDia.get("sourceTableCode"),
                "el precio día pleno se lee de la tabla del convenio");
        assertEquals(filaVigente, ((Number) precioDia.get("sourceTableRowId")).longValue(),
                "y de la fila que estaba vigente para su categoría");

        // Y el nulo del resto significa «este valor no vino de ninguna fila», no «todavía no se
        // sabe»: un AGGREGATE suma conceptos y un PERCENTAGE multiplica una base por un tipo.
        List<Object> conFila = steps.stream()
                .filter(step -> step.get("sourceTableRowId") != null)
                .map(step -> step.get("conceptCode"))
                .sorted()
                .toList();
        assertEquals(TABLE_FED_CONCEPTS, conFila,
                "de los " + steps.size() + " pasos del recibo leen una fila estos y sólo estos");

        // Y los que leen tabla son los que la reglamentación dice que la leen: un DIRECT_AMOUNT
        // alimentado por una tabla. Nada de lo que se calcula a partir de otros conceptos.
        for (Map<String, Object> step : steps) {
            if (step.get("sourceTableCode") != null) {
                assertEquals("DIRECT_AMOUNT", step.get("calculationType"),
                        "sólo un importe leído puede decir de dónde se leyó: " + step.get("conceptCode"));
            }
        }

        for (Map<String, Object> step : steps) {
            if (step.get("sourceTableCode") == null) {
                assertNull(step.get("sourceTableRowId"),
                        "media dirección no lleva a ningún sitio: " + step.get("conceptCode"));
            } else {
                assertNotNull(step.get("sourceTableRowId"),
                        "media dirección no lleva a ningún sitio: " + step.get("conceptCode"));
            }
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aRecalculationAfterTheRowChangedPointsAtTheNewOne_andThePayrollNotRecalculatedKeepsItsOwn()
            throws Exception {
        long filaVieja = rowIdValidOn(APRIL_1);

        String recalculado = hireWholeMonth();
        String intacto = hireWholeMonth();
        calculate(recalculado);
        calculate(intacto);

        assertEquals(filaVieja, sourceRowOf(recalculado));
        assertEquals(filaVieja, sourceRowOf(intacto));

        // El convenio se actualiza: la fila que los dos leyeron se cierra en marzo y se abre otra
        // desde abril. A partir de aquí, la misma búsqueda ya no contesta lo mismo.
        long filaNueva = replaceRowFromApril(filaVieja, new BigDecimal("45.00"));
        assertNotEquals(filaVieja, filaNueva);
        assertEquals(filaNueva, rowIdValidOn(APRIL_1), "la búsqueda de hoy ya elige la fila nueva");

        recalculate(recalculado);

        assertEquals(filaNueva, sourceRowOf(recalculado),
                "el recibo recalculado guarda la fila que el motor acaba de leer");
        assertEquals(filaVieja, sourceRowOf(intacto),
                "y el que no se recalculó sigue apuntando a la que leyó, que hoy ya no se elegiría");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** La fila que la búsqueda del motor elegiría para la categoría del fixture en esa fecha. */
    private long rowIdValidOn(LocalDate date) {
        Long id = jdbc.queryForObject(
                "select id from payroll.payroll_table_row"
                        + " where rule_system_code = ? and table_code = ? and search_code = ?"
                        + "   and active = true and start_date <= ?"
                        + "   and (end_date is null or end_date >= ?)"
                        + " order by start_date desc"
                        + " limit 1",
                Long.class, RULE_SYSTEM, TABLE_CODE, CATEGORY_CODE, date, date);
        assertNotNull(id, "el escenario necesita una fila vigente para " + CATEGORY_CODE);
        return id;
    }

    /** Cierra la fila vigente en marzo y abre otra desde abril. Devuelve el id de la nueva. */
    private long replaceRowFromApril(long filaVieja, BigDecimal dailyValue) {
        LocalDate inicioVieja = jdbc.queryForObject(
                "select start_date from payroll.payroll_table_row where id = ?",
                LocalDate.class, filaVieja);
        assertNotNull(inicioVieja);
        assertTrue(inicioVieja.isBefore(APRIL_1),
                "el escenario necesita que la fila vigente empiece antes de abril");

        assertEquals(1, jdbc.update(
                "update payroll.payroll_table_row set end_date = ? where id = ?",
                MARCH_31, filaVieja));
        jdbc.update(
                "insert into payroll.payroll_table_row"
                        + " (rule_system_code, table_code, search_code, start_date, end_date,"
                        + "  monthly_value, daily_value, active)"
                        + " values (?, ?, ?, ?, null, null, ?, true)",
                RULE_SYSTEM, TABLE_CODE, CATEGORY_CODE, APRIL_1, dailyValue);

        return rowIdValidOn(APRIL_1);
    }

    private long sourceRowOf(String employeeNumber) throws Exception {
        Object id = stepOf(getSteps(employeeNumber), TABLE_FED_CONCEPT).get("sourceTableRowId");
        assertNotNull(id, "el paso del precio día pleno tiene que decir de qué fila salió");
        return ((Number) id).longValue();
    }

    private static Map<String, Object> stepOf(List<Map<String, Object>> steps, String conceptCode) {
        List<Map<String, Object>> matching = steps.stream()
                .filter(step -> conceptCode.equals(step.get("conceptCode")))
                .toList();
        assertEquals(1, matching.size(), "un solo paso de " + conceptCode + " en un mes entero");
        return matching.getFirst();
    }

    private List<Map<String, Object>> getSteps(String employeeNumber) throws Exception {
        String body = mockMvc
                .perform(get(STEPS_URL, RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE, 1))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JSON.readValue(body, new TypeReference<List<Map<String, Object>>>() { });
    }

    private void calculate(String employeeNumber) {
        assertEquals("COMPLETED", launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employeeNumber),
                        null),
                null)).status());
        // Los pasos llevan clave asignada y su insert queda diferido hasta el vaciado de la
        // sesion; estos tests van en transaccion y no hay commit que vacie por ellos (ADR-062).
        entityManager.flush();
    }

    /**
     * Anular y recalcular, que es el gesto entero: el recálculo sólo sale de {@code NOT_VALID}
     * ({@code ADR-059}), y no es un detalle del test — es lo que hace un operador de nómina cuando
     * el recibo que tiene delante dejó de reflejar las reglas.
     */
    private void recalculate(String employeeNumber) {
        invalidateUseCase.invalidate(new InvalidatePayrollCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE, 1,
                "RULES_CHANGED"));
        recalculateUseCase.recalculate(new RecalculatePayrollCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE, 1, null));
        entityManager.flush();
    }

    private String hireWholeMonth() {
        String emp = "SR" + (System.nanoTime() % 1_000_000_000L);
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, JANUARY_1, null);
        fixtures.insertLaborClassification(empId, JANUARY_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), JANUARY_1, null);
        return emp;
    }
}
