package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.model.CostCenterDistributionPlan;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionIsACorrectionException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionOverlapException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionPeriod;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The five cases of backend#48, plus the correction asked for as an add
 * (backend#52) and the correction of a typo in place (backend#54), against
 * the real schema: the cost center series is written through the temporal
 * component and the invariants of ADR-057 decide. The occurrence is the
 * window, so every case that moves a window is checked line by line. The
 * series declares optional coverage (ADR-057, decision 1; backend#54), so
 * the cases the other verticals reject for a gap are accepted here and the
 * gap stays; the overlap invariant does not depend on the coverage. The
 * catalog is the seeded one: CC_ADMIN, CC_HR and CC_IT are active in ESP
 * (V50).
 */
@TestSobreEsquemaReal
class CostCenterTimelineFlywayIntegrationTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final LocalDate DAY_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate DAY_15 = LocalDate.of(2026, 1, 15);
    private static final LocalDate DAY_16 = LocalDate.of(2026, 1, 16);
    private static final LocalDate JAN_31 = LocalDate.of(2026, 1, 31);
    private static final LocalDate FEB_1 = LocalDate.of(2026, 2, 1);

    @Autowired
    private CreateCostCenterDistributionService createService;
    @Autowired
    private UpdateCostCenterDistributionService updateService;
    @Autowired
    private DeleteCostCenterDistributionService deleteService;
    @Autowired
    private PlanCostCenterDistributionChangeService planService;
    @Autowired
    private CloseCostCenterDistributionService closeService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    private String employeeNumber;
    private Long employeeId;

    @BeforeEach
    void anEmployeePresentFromDayOne() {
        employeeNumber = "CC" + (System.nanoTime() % 1_000_000_000L);
        employeeId = DatosDePrueba.empleado(jdbcTemplate, employeeNumber);
        DatosDePrueba.presencia(jdbcTemplate, employeeId, 1, DAY_1, null);
    }

    // backend#48, case 1: adding at the end closes the window in force, every line of it.
    @Test
    void addingFromTheSixteenthClosesEveryLineOfTheOpenWindowOnTheFifteenth() {
        createService.create(create(DAY_1, null, Map.of("CC_ADMIN", 60, "CC_HR", 40)));

        CostCenterDistributionWindow second = createService.create(create(DAY_16, null, Map.of("CC_IT", 100)));
        entityManager.flush();

        assertEquals(DAY_16, second.getStartDate());
        assertNull(second.getEndDate());
        assertEquals(3, persistedCount());
        assertEquals(List.of(DAY_15, DAY_15), persistedEndDates(DAY_1));
        assertEquals(Arrays.asList((LocalDate) null), persistedEndDates(DAY_16));
    }

    // backend#48, case 2: adding in the middle splits the covering window; the new one takes the
    // dates the user gives and the series stays covered.
    @Test
    void addingInTheMiddleSplitsTheCoveringWindowAndLeavesNoGap() {
        createService.create(create(DAY_1, null, Map.of("CC_ADMIN", 100)));
        createService.create(create(FEB_1, null, Map.of("CC_HR", 100)));

        createService.create(create(DAY_16, JAN_31, Map.of("CC_IT", 100)));
        entityManager.flush();

        assertEquals(3, persistedCount());
        assertEquals(List.of(DAY_15), persistedEndDates(DAY_1));
        assertEquals(List.of(JAN_31), persistedEndDates(DAY_16));
        assertEquals(Arrays.asList((LocalDate) null), persistedEndDates(FEB_1));
    }

    // backend#48, case 3: outside the presence.
    @Test
    void addingBeforeThePresenceIsRejectedAndPersistsNothing() {
        assertThrows(
                CostCenterOutsidePresencePeriodException.class,
                () -> createService.create(create(LocalDate.of(2025, 12, 1), null, Map.of("CC_ADMIN", 100)))
        );
        entityManager.flush();

        assertEquals(0, persistedCount());
    }

    // Optional coverage (backend#54): the add closes the window in force the day before, and the
    // stretch after the new window stays uncovered. That is a legal state for this series.
    @Test
    void addingOneThatLeavesAGapIsAcceptedAndTheGapStays() {
        createService.create(create(DAY_1, null, Map.of("CC_ADMIN", 100)));

        createService.create(create(FEB_1, LocalDate.of(2026, 2, 28), Map.of("CC_HR", 100)));
        entityManager.flush();

        assertEquals(2, persistedCount());
        assertEquals(List.of(JAN_31), persistedEndDates(DAY_1));
        assertEquals(List.of(LocalDate.of(2026, 2, 28)), persistedEndDates(FEB_1));
    }

    // backend#54: an employee without a distribution may start one later than the hire date. The
    // thousand employees the old seed left without a distribution are this case.
    @Test
    void anEmployeeWithoutADistributionCanStartOneAfterTheHireDate() {
        CostCenterDistributionWindow first = createService.create(create(DAY_16, null, Map.of("CC_ADMIN", 100)));
        entityManager.flush();

        assertEquals(DAY_16, first.getStartDate());
        assertNull(first.getEndDate());
        assertEquals(1, persistedCount());
        assertEquals(Arrays.asList((LocalDate) null), persistedEndDates(DAY_16));
    }

    // The plan shows the gap without holding it against the add: the screen can say what the
    // series will look like, and the user decides.
    @Test
    void thePlanNamesTheGapAnAddWouldLeaveWithoutRejectingIt() {
        CostCenterDistributionPlan plan = planService.plan(new PlanCostCenterDistributionChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, TimelineOperation.ADD, null, DAY_16, null
        ));
        entityManager.flush();

        assertTrue(plan.isAccepted());
        assertNull(plan.rejection());
        assertEquals(List.of(new CostCenterDistributionPeriod(DAY_1, DAY_15)), plan.gaps());
        assertEquals(List.of(new CostCenterDistributionPeriod(DAY_16, null)), plan.stretchCandidates());
        assertEquals(0, persistedCount());
    }

    // backend#52: an add on the start date of the existing window is its correction, not a
    // second window, and it is rejected saying so.
    @Test
    void addingOnTheStartDateOfTheExistingWindowIsRejectedAsItsCorrectionAndPersistsNothing() {
        createService.create(create(DAY_1, null, Map.of("CC_ADMIN", 100)));

        CostCenterDistributionIsACorrectionException ex = assertThrows(
                CostCenterDistributionIsACorrectionException.class,
                () -> createService.create(create(DAY_1, null, Map.of("CC_HR", 100)))
        );
        entityManager.flush();

        assertEquals(new CostCenterDistributionPeriod(DAY_1, null), ex.correctedOccurrence());
        assertTrue(ex.getMessage().contains("correct"), ex.getMessage());
        assertEquals(1, persistedCount());
        assertEquals(List.of("CC_ADMIN"), persistedCodes(DAY_1));
    }

    // backend#54: the typo is fixed in place. No window is invented in the history.
    @Test
    void correctingAPercentageReplacesTheLinesOfTheWindowUnderTheSameDates() {
        createService.create(create(DAY_1, null, Map.of("CC_ADMIN", 60, "CC_HR", 40)));

        CostCenterDistributionWindow corrected = updateService.update(update(DAY_1, null, null,
                Map.of("CC_ADMIN", 70, "CC_HR", 30)));
        entityManager.flush();

        assertEquals(DAY_1, corrected.getStartDate());
        assertNull(corrected.getEndDate());
        assertEquals(2, persistedCount());
        assertEquals(new BigDecimal("70.00"), persistedPercentage(DAY_1, "CC_ADMIN"));
        assertEquals(new BigDecimal("30.00"), persistedPercentage(DAY_1, "CC_HR"));
    }

    // The correction the plan proposes for an add on an existing start date, asked for as such.
    @Test
    void theCorrectionAskedForAsSuchReplacesTheCostCentersWithoutADuplicateWindow() {
        createService.create(create(DAY_1, null, Map.of("CC_ADMIN", 100)));

        updateService.update(update(DAY_1, null, null, Map.of("CC_HR", 50, "CC_IT", 50)));
        entityManager.flush();

        assertEquals(2, persistedCount());
        assertEquals(List.of("CC_HR", "CC_IT"), persistedCodes(DAY_1));
    }

    // Optional coverage: moving the second window forward leaves DAY_16..JAN_31 uncovered, and the
    // correction goes through. Nothing else moves: the first window keeps its end (decision 3).
    @Test
    void correctingTheDatesThatLeaveAGapIsAcceptedAndNothingElseMoves() {
        createService.create(create(DAY_1, null, Map.of("CC_ADMIN", 100)));
        createService.create(create(DAY_16, null, Map.of("CC_HR", 100)));

        updateService.update(update(DAY_16, FEB_1, null, Map.of("CC_HR", 100)));
        entityManager.flush();

        assertEquals(2, persistedCount());
        assertEquals(List.of(DAY_15), persistedEndDates(DAY_1));
        assertEquals(List.of(), persistedCodes(DAY_16));
        assertEquals(List.of("CC_HR"), persistedCodes(FEB_1));
        assertEquals(Arrays.asList((LocalDate) null), persistedEndDates(FEB_1));
    }

    // The overlap invariant does not depend on the coverage: it still rejects, naming the shared
    // dates, and persists nothing.
    @Test
    void correctingTheDatesOntoAnotherWindowIsRejectedAsAnOverlapAndPersistsNothing() {
        createService.create(create(DAY_1, null, Map.of("CC_ADMIN", 100)));
        createService.create(create(DAY_16, null, Map.of("CC_HR", 100)));

        CostCenterDistributionOverlapException ex = assertThrows(
                CostCenterDistributionOverlapException.class,
                () -> updateService.update(update(DAY_16, LocalDate.of(2026, 1, 10), null, Map.of("CC_HR", 100)))
        );
        entityManager.flush();

        assertEquals(List.of(new CostCenterDistributionPeriod(LocalDate.of(2026, 1, 10), DAY_15)), ex.overlaps());
        assertEquals(2, persistedCount());
        assertEquals(List.of("CC_HR"), persistedCodes(DAY_16));
        assertEquals(List.of(DAY_15), persistedEndDates(DAY_1));
    }

    // backend#48, case 4: deleting the last window reopens the previous one, every line of it.
    @Test
    void deletingTheLastWindowReopensEveryLineOfThePreviousOne() {
        createService.create(create(DAY_1, null, Map.of("CC_ADMIN", 60, "CC_HR", 40)));
        createService.create(create(DAY_16, null, Map.of("CC_IT", 100)));

        deleteService.delete(new DeleteCostCenterDistributionCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, DAY_16));
        entityManager.flush();

        assertEquals(2, persistedCount());
        assertEquals(Arrays.asList(null, null), persistedEndDates(DAY_1));
    }

    // backend#48, case 5, read the other way here (backend#54): the series declares optional
    // coverage, so removing a window in the middle goes through and leaves the gap between its
    // neighbours. Nothing else moves: the first window keeps its end and the last stays open. The
    // component sees the gap afterwards: the next add names it.
    @Test
    void deletingAWindowInTheMiddleIsAcceptedAndLeavesTheGapBetweenItsNeighbours() {
        createService.create(create(DAY_1, null, Map.of("CC_ADMIN", 100)));
        createService.create(create(DAY_16, null, Map.of("CC_HR", 100)));
        createService.create(create(FEB_1, null, Map.of("CC_IT", 100)));

        deleteService.delete(new DeleteCostCenterDistributionCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, DAY_16));
        entityManager.flush();

        assertEquals(2, persistedCount());
        assertEquals(List.of(DAY_15), persistedEndDates(DAY_1));
        assertEquals(List.of(), persistedCodes(DAY_16));
        assertEquals(Arrays.asList((LocalDate) null), persistedEndDates(FEB_1));

        CostCenterDistributionPlan next = planService.plan(new PlanCostCenterDistributionChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, TimelineOperation.ADD, null, LocalDate.of(2026, 3, 1), null
        ));
        assertTrue(next.isAccepted());
        assertEquals(List.of(new CostCenterDistributionPeriod(DAY_16, JAN_31)), next.gaps());
    }

    @Test
    void deletingTheOnlyWindowLeavesTheEmployeeWithoutADistribution() {
        createService.create(create(DAY_1, null, Map.of("CC_ADMIN", 100)));

        deleteService.delete(new DeleteCostCenterDistributionCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, DAY_1));
        entityManager.flush();

        assertEquals(0, persistedCount());
    }

    @Test
    void thePlanCanBeAskedForWithoutApplyingIt() {
        createService.create(create(DAY_1, null, Map.of("CC_ADMIN", 60, "CC_HR", 40)));

        CostCenterDistributionPlan plan = planService.plan(new PlanCostCenterDistributionChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, TimelineOperation.ADD, null, DAY_16, null
        ));
        entityManager.flush();

        assertTrue(plan.isAccepted());
        assertEquals(new CostCenterDistributionPeriod(DAY_1, DAY_15), plan.adjustedOccurrence().after());
        assertEquals(
                List.of(new CostCenterDistributionPeriod(DAY_1, DAY_15), new CostCenterDistributionPeriod(DAY_16, null)),
                plan.projected()
        );
        assertEquals(2, persistedCount());
        assertEquals(Arrays.asList(null, null), persistedEndDates(DAY_1));
    }

    @Test
    void thePlanSaysAnAddOnAnExistingStartDateIsACorrectionOfThatWindow() {
        createService.create(create(DAY_1, null, Map.of("CC_ADMIN", 100)));

        CostCenterDistributionPlan plan = planService.plan(new PlanCostCenterDistributionChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, TimelineOperation.ADD, null, DAY_1, DAY_15
        ));
        entityManager.flush();

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(new CostCenterDistributionPeriod(DAY_1, null), plan.correctedOccurrence());
        assertEquals(List.of(new CostCenterDistributionPeriod(DAY_1, DAY_15)), plan.projected());
        assertEquals(1, persistedCount());
        assertEquals(Arrays.asList((LocalDate) null), persistedEndDates(DAY_1));
    }

    // The deprecated close, on the termination date: the presence ended that day, so every line
    // of the window in force closes with it and no gap is left.
    @Test
    void closingTheOpenWindowOnTheTerminationDateClosesEveryLineOfIt() {
        createService.create(create(DAY_1, null, Map.of("CC_ADMIN", 60, "CC_HR", 40)));
        jdbcTemplate.update(
                "update employee.presence set end_date = ?, exit_reason_code = 'TERMINATION' where employee_id = ?",
                JAN_31, employeeId
        );

        closeService.close(new CloseCostCenterDistributionCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, DAY_1, JAN_31));
        entityManager.flush();

        assertEquals(List.of(JAN_31, JAN_31), persistedEndDates(DAY_1));
    }

    // Optional coverage: the deprecated close on a date inside an open presence leaves the rest of
    // it uncovered, and that is legal here.
    @Test
    void closingTheOpenWindowWhileThePresenceGoesOnIsAcceptedAndLeavesTheGap() {
        createService.create(create(DAY_1, null, Map.of("CC_ADMIN", 100)));

        closeService.close(new CloseCostCenterDistributionCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, DAY_1, DAY_15));
        entityManager.flush();

        assertEquals(List.of(DAY_15), persistedEndDates(DAY_1));
    }

    private CreateCostCenterDistributionCommand create(LocalDate startDate, LocalDate endDate, Map<String, Integer> items) {
        return new CreateCostCenterDistributionCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, startDate, endDate, items(items));
    }

    private UpdateCostCenterDistributionCommand update(
            LocalDate windowStartDate,
            LocalDate startDate,
            LocalDate endDate,
            Map<String, Integer> items
    ) {
        return new UpdateCostCenterDistributionCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, windowStartDate, startDate, endDate, items(items));
    }

    private static List<CostCenterDistributionItem> items(Map<String, Integer> items) {
        return items.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new CostCenterDistributionItem(entry.getKey(), BigDecimal.valueOf(entry.getValue())))
                .toList();
    }

    private int persistedCount() {
        return jdbcTemplate.queryForObject(
                "select count(*) from employee.cost_center where employee_id = ?",
                Integer.class,
                employeeId
        );
    }

    private List<LocalDate> persistedEndDates(LocalDate windowStartDate) {
        return jdbcTemplate.queryForList(
                "select end_date from employee.cost_center where employee_id = ? and start_date = ? order by cost_center_code",
                LocalDate.class,
                employeeId,
                windowStartDate
        );
    }

    private List<String> persistedCodes(LocalDate windowStartDate) {
        return jdbcTemplate.queryForList(
                "select cost_center_code from employee.cost_center where employee_id = ? and start_date = ? order by cost_center_code",
                String.class,
                employeeId,
                windowStartDate
        );
    }

    private BigDecimal persistedPercentage(LocalDate windowStartDate, String costCenterCode) {
        return jdbcTemplate.queryForObject(
                "select allocation_percentage from employee.cost_center where employee_id = ? and start_date = ? and cost_center_code = ?",
                BigDecimal.class,
                employeeId,
                windowStartDate,
                costCenterCode
        );
    }
}
