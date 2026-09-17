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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El recibo dice si las reglas han cambiado desde que se calculó ({@code backend#107}).
 *
 * <p>El recibo <b>no</b> cambia: es lo que el motor calculó y así se queda ({@code ADR-062}). Lo que
 * se añade es que lo diga, porque si no, después de editar una regla parece que no ha pasado nada.
 *
 * <p>La edición se hace <b>por la API</b>, con el mismo {@code PUT} que usa el designer, y no con un
 * {@code update} por JDBC. No es ceremonia: la fecha que se compara es el {@code updated_at} de la
 * fila, y hasta este issue la entidad no lo mapeaba — editar por la API dejaba la marca en la del
 * día de la siembra. Un test que escribiera por JDBC habría pasado con el defecto puesto.
 *
 * <p>Y hay dos empleados porque el caso que decide el diseño es el segundo: una edición afecta a
 * <b>todos</b> los recibos del sistema de reglas, no sólo al que estabas mirando. Si la marca
 * viviera en el flujo de la edición, los otros 872 se quedarían rancios en silencio.
 */
@TestWebSobreEsquemaReal
class ThePayslipSaysWhetherTheRulesChangedIntegrationTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202504";
    private static final String PAYROLL_TYPE  = "NORMAL";

    private static final LocalDate JANUARY_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate APRIL_1   = LocalDate.of(2025, 4, 1);

    private static final String TABLE_CODE    = "P02_99002405011982";
    private static final String CATEGORY_CODE = "99002405-G2";

    private static final String PAYROLL_URL =
            "/payrolls/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}"
                    + "/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}";

    private static final String ROW_URL = "/payroll-engine/{ruleSystemCode}/tables/{tableCode}/rows/{rowId}";

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
    void editingARuleMarksEveryPayroll_andRecalculatingClearsOnlyTheOneRecalculated() throws Exception {
        String elQueMirabas = hireWholeMonth();
        String elOtro = hireWholeMonth();
        calculate(elQueMirabas);
        calculate(elOtro);

        assertFalse(rulesChanged(elQueMirabas), "recién calculado, nada ha cambiado desde entonces");
        assertFalse(rulesChanged(elOtro), "recién calculado, nada ha cambiado desde entonces");

        // El gesto entero: un administrador de nóminas sube el precio día del convenio.
        editDailyValue(rowIdValidOn(APRIL_1), new BigDecimal("45.00"));

        assertTrue(rulesChanged(elQueMirabas),
                "las reglas se han tocado después de calcularse: el recibo tiene que decirlo");
        assertTrue(rulesChanged(elOtro),
                "y también el recibo que no venías de mirar, que es por lo que esto va en el recibo");

        // Y se calla solo, sin que nadie lo apague: recalcular mueve el calculated_at por delante
        // del cambio.
        recalculate(elQueMirabas);

        assertFalse(rulesChanged(elQueMirabas), "recalculado contra las reglas de ahora: se calla");
        assertTrue(rulesChanged(elOtro), "y el que no se recalculó sigue avisando");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private boolean rulesChanged(String employeeNumber) throws Exception {
        String body = mockMvc
                .perform(get(PAYROLL_URL, RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE, 1))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        Map<String, Object> payroll = JSON.readValue(body, new TypeReference<Map<String, Object>>() { });
        Object marca = payroll.get("rulesChangedSinceCalculation");
        assertNotNull(marca, "la marca va siempre en el recibo, no sólo cuando está levantada");
        return (Boolean) marca;
    }

    /** El mismo PUT que usa el designer para cambiar un precio. */
    private void editDailyValue(long rowId, BigDecimal dailyValue) throws Exception {
        mockMvc.perform(put(ROW_URL, RULE_SYSTEM, TABLE_CODE, rowId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dailyValue\":" + dailyValue.toPlainString() + "}"))
                .andExpect(status().isOk());
        entityManager.flush();
    }

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

    private void calculate(String employeeNumber) {
        assertEquals("COMPLETED", launch.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employeeNumber),
                        null),
                null)).status());
        entityManager.flush();
    }

    private void recalculate(String employeeNumber) {
        invalidateUseCase.invalidate(new InvalidatePayrollCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE, 1, "RULES_CHANGED"));
        recalculateUseCase.recalculate(new RecalculatePayrollCommand(
                RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE, 1, null));
        entityManager.flush();
    }

    private String hireWholeMonth() {
        String emp = "RC" + (System.nanoTime() % 1_000_000_000L);
        long empId = fixtures.insertEmployee(RULE_SYSTEM, EMPLOYEE_TYPE, emp);
        fixtures.insertPresence(empId, 1, JANUARY_1, null);
        fixtures.insertLaborClassification(empId, JANUARY_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), JANUARY_1, null);
        return emp;
    }
}
