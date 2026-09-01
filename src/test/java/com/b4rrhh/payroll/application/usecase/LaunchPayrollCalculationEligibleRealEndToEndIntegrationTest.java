package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.scenario.PayrollScenarioFixtures;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Sin peticion HTTP, pero con la aplicacion entera, en transaccion y sobre el
// esquema real: lo mismo que la franja E2E, y por eso comparte su contexto y
// su clon en vez de declarar uno propio (#2).
//
// TST y no ESP por la misma razon que PayrollHappyPathIntegrationTest: las
// migraciones ya siembran para ESP el grafo que el fixture monta.
@TestWebSobreEsquemaReal
class LaunchPayrollCalculationEligibleRealEndToEndIntegrationTest {

    private static final String RULE_SYSTEM = "TST";

    private String employeeNumber;
    private PayrollScenarioFixtures fixtures;

    @Autowired
    private LaunchPayrollCalculationUseCase launchPayrollCalculationUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpData() {
        fixtures = new PayrollScenarioFixtures(jdbcTemplate);
        fixtures.seedConceptGraph(RULE_SYSTEM);

        employeeNumber = "ER" + (System.nanoTime() % 1_000_000_000L);
        long employeeId = fixtures.insertEmployee(RULE_SYSTEM, "INTERNAL", employeeNumber);
        fixtures.insertPresence(employeeId, 1, LocalDate.of(2025, 1, 1), null);
        fixtures.insertLaborClassification(employeeId, LocalDate.of(2025, 1, 1));
        fixtures.insertWorkingTime(employeeId, new BigDecimal("100.00"),
                LocalDate.of(2025, 1, 1), null);
    }

    @Test
    void launchPersistsEligibleConceptsWithAggregates() {
        var run = launchPayrollCalculationUseCase.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM,
                "202501",
                "NORMAL",
                "ENGINE",
                "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget("INTERNAL", employeeNumber),
                        null
                )
        ));

        assertEquals("COMPLETED", run.status());
        assertEquals(1, run.totalCandidates());
        assertEquals(1, run.totalCalculated());

        Long payrollId = jdbcTemplate.queryForObject(
                "select id from payroll.payroll where rule_system_code = ? and employee_type_code = ? and employee_number = ? and payroll_period_code = ? and payroll_type_code = ? and presence_number = ? and status = ?",
                Long.class,
                RULE_SYSTEM, "INTERNAL", employeeNumber, "202501", "NORMAL", 1, "CALCULATED");
        assertNotNull(payrollId);

        Integer conceptCount = jdbcTemplate.queryForObject(
                "select count(*) from payroll.payroll_concept where payroll_id = ?",
                Integer.class, payrollId);
        assertEquals(7, conceptCount);

        for (String unpersisted : new String[]{"D01","J01","P01","P02","B01","P_SS_CC","P_SS_DESEMPLEO","P_IRPF"}) {
            assertEquals(0, jdbcTemplate.queryForObject(
                    "select count(*) from payroll.payroll_concept where payroll_id = ? and concept_code = ?",
                    Integer.class, payrollId, unpersisted),
                    unpersisted + " must not be persisted");
        }

        BigDecimal amount101 = jdbcTemplate.queryForObject(
                "select amount from payroll.payroll_concept where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, "101");
        BigDecimal quantity101 = jdbcTemplate.queryForObject(
                "select quantity from payroll.payroll_concept where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, "101");
        BigDecimal rate101 = jdbcTemplate.queryForObject(
                "select rate from payroll.payroll_concept where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, "101");
        assertEquals(0, new BigDecimal("1425.00").compareTo(amount101));
        assertEquals(0, new BigDecimal("30").compareTo(quantity101));
        assertEquals(0, new BigDecimal("47.50").compareTo(rate101));

        BigDecimal amount700 = jdbcTemplate.queryForObject(
                "select amount from payroll.payroll_concept where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, "700");
        assertEquals(0, new BigDecimal("66.98").compareTo(amount700));

        BigDecimal amount703 = jdbcTemplate.queryForObject(
                "select amount from payroll.payroll_concept where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, "703");
        assertEquals(0, new BigDecimal("22.09").compareTo(amount703));

        BigDecimal amount800 = jdbcTemplate.queryForObject(
                "select amount from payroll.payroll_concept where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, "800");
        assertEquals(0, new BigDecimal("213.75").compareTo(amount800));

        BigDecimal amount970 = jdbcTemplate.queryForObject(
                "select amount from payroll.payroll_concept where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, "970");
        BigDecimal amount980 = jdbcTemplate.queryForObject(
                "select amount from payroll.payroll_concept where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, "980");
        BigDecimal amount990 = jdbcTemplate.queryForObject(
                "select amount from payroll.payroll_concept where payroll_id = ? and concept_code = ?",
                BigDecimal.class, payrollId, "990");
        assertEquals(0, new BigDecimal("1425.00").compareTo(amount970));
        assertEquals(0, new BigDecimal("302.82").compareTo(amount980));
        assertEquals(0, new BigDecimal("1122.18").compareTo(amount990));

        Integer warningCount = jdbcTemplate.queryForObject(
                "select count(*) from payroll.payroll_warning where payroll_id = ? and warning_code = ?",
                Integer.class, payrollId, "ELIGIBLE_REAL_EXECUTION");
        Integer snapshotCount = jdbcTemplate.queryForObject(
                "select count(*) from payroll.payroll_context_snapshot where payroll_id = ? and snapshot_type_code = ?",
                Integer.class, payrollId, "EMPLOYEE_PAYROLL_CONTEXT");
        Integer claimsAfterLaunch = jdbcTemplate.queryForObject(
                "select count(*) from payroll.calculation_claim where run_id = ?",
                Integer.class, run.id());
        Integer segmentCount = jdbcTemplate.queryForObject(
                "select count(*) from payroll.payroll_segment where payroll_id = ?",
                Integer.class, payrollId);
        LocalDate segmentStart = jdbcTemplate.queryForObject(
                "select segment_start from payroll.payroll_segment where payroll_id = ?",
                LocalDate.class, payrollId);
        assertEquals(1, warningCount);
        assertEquals(1, snapshotCount);
        assertEquals(0, claimsAfterLaunch);
        assertEquals(1, segmentCount);
        assertEquals(LocalDate.of(2025, 1, 1), segmentStart);
    }
}
