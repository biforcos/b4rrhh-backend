package com.b4rrhh.employee.cost_center.application.service;

import com.b4rrhh.employee.cost_center.application.model.CostCenterDistributionPlan;
import com.b4rrhh.employee.cost_center.application.model.CostCenterDistributionPlanAdjustment;
import com.b4rrhh.employee.cost_center.application.port.CostCenterPresenceConsistencyPort;
import com.b4rrhh.employee.cost_center.application.port.PresencePeriod;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionCoverageGapException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionIsACorrectionException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionOverlapException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionPeriod;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
import com.b4rrhh.employee.cost_center.domain.service.CostCenterDistributionWindowGrouper;
import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * The series under test: the employee is present from 2026-01-01 onwards
 * and has two distribution windows, the second still open and split between
 * two cost centers. The occurrence the planner sees is the window, so the
 * two lines of the second one are one range, not two (ADR-057, decision 0).
 *
 * <pre>
 *   2026-01-01 .. 2026-01-31   CC_ADMIN 100
 *   2026-02-01 .. (open)       CC_HR 60 + CC_IT 40
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class CostCenterTimelineServiceTest {

    private static final Long EMPLOYEE_ID = 10L;
    private static final LocalDate JAN_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JAN_31 = LocalDate.of(2026, 1, 31);
    private static final LocalDate FEB_1 = LocalDate.of(2026, 2, 1);

    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private CostCenterPresenceConsistencyPort presencePort;

    private CostCenterTimelineService service;

    @BeforeEach
    void setUp() {
        service = new CostCenterTimelineService(
                costCenterRepository,
                presencePort,
                new CostCenterDistributionWindowGrouper()
        );
    }

    @Test
    void addingAfterTheOpenWindowClosesItTheDayBeforeAsOneOccurrence() {
        givenSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), line("CC_HR", 60, FEB_1, null), line("CC_IT", 40, FEB_1, null));

        CostCenterDistributionPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 3, 16), null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.ADD, plan.operation());
        assertEquals(period(LocalDate.of(2026, 3, 16), null), plan.occurrence());
        assertEquals(
                new CostCenterDistributionPlanAdjustment(
                        period(FEB_1, null),
                        period(FEB_1, LocalDate.of(2026, 3, 15))
                ),
                plan.adjustedOccurrence()
        );
        assertEquals(
                List.of(
                        period(JAN_1, JAN_31),
                        period(FEB_1, LocalDate.of(2026, 3, 15)),
                        period(LocalDate.of(2026, 3, 16), null)
                ),
                plan.projected()
        );
    }

    // The point of decision 0: measured by line, the two lines of the open window would be an
    // overlap of each other. Measured by window, they are one occurrence and the series is clean.
    @Test
    void theLinesOfOneWindowAreNeverAnOverlapAmongThemselves() {
        givenSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), line("CC_HR", 60, FEB_1, null), line("CC_IT", 40, FEB_1, null));

        CostCenterDistributionPlan plan = service.planCorrect(
                EMPLOYEE_ID,
                window(FEB_1, null, line("CC_HR", 60, FEB_1, null), line("CC_IT", 40, FEB_1, null)),
                range(FEB_1, null)
        );

        assertTrue(plan.isAccepted());
        assertEquals(List.of(), plan.overlaps());
        assertEquals(List.of(period(JAN_1, JAN_31), period(FEB_1, null)), plan.projected());
    }

    @Test
    void addingInsideAClosedWindowSplitsItAndTheTailIsTheNewOnesToTake() {
        givenSeries(
                line("CC_ADMIN", 100, JAN_1, JAN_31),
                line("CC_HR", 100, FEB_1, LocalDate.of(2026, 3, 31)),
                line("CC_IT", 100, LocalDate.of(2026, 4, 1), null)
        );

        CostCenterDistributionPlan plan = service.planAdd(
                EMPLOYEE_ID,
                range(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))
        );

        assertTrue(plan.isAccepted());
        assertEquals(
                new CostCenterDistributionPlanAdjustment(
                        period(FEB_1, LocalDate.of(2026, 3, 31)),
                        period(FEB_1, LocalDate.of(2026, 2, 28))
                ),
                plan.adjustedOccurrence()
        );
        assertEquals(
                List.of(
                        period(JAN_1, JAN_31),
                        period(FEB_1, LocalDate.of(2026, 2, 28)),
                        period(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)),
                        period(LocalDate.of(2026, 4, 1), null)
                ),
                plan.projected()
        );
    }

    @Test
    void addingThatLeavesAGapNamesTheGapAndTheNeighbours() {
        givenSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), line("CC_HR", 100, FEB_1, LocalDate.of(2026, 2, 28)));

        CostCenterDistributionPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 4, 1), null));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertNull(plan.adjustedOccurrence());
        assertEquals(List.of(period(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))), plan.gaps());
        assertEquals(
                List.of(period(FEB_1, LocalDate.of(2026, 2, 28)), period(LocalDate.of(2026, 4, 1), null)),
                plan.stretchCandidates()
        );
    }

    @Test
    void removingTheLastWindowReopensThePreviousOne() {
        givenSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), line("CC_HR", 60, FEB_1, null), line("CC_IT", 40, FEB_1, null));

        CostCenterDistributionPlan plan = service.planRemove(
                EMPLOYEE_ID,
                window(FEB_1, null, line("CC_HR", 60, FEB_1, null), line("CC_IT", 40, FEB_1, null))
        );

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.REMOVE, plan.operation());
        assertEquals(period(FEB_1, null), plan.occurrence());
        assertEquals(
                new CostCenterDistributionPlanAdjustment(period(JAN_1, JAN_31), period(JAN_1, null)),
                plan.adjustedOccurrence()
        );
        assertEquals(List.of(period(JAN_1, null)), plan.projected());
    }

    @Test
    void removingAWindowInTheMiddleIsRejectedNamingTheNeighboursToStretch() {
        givenSeries(
                line("CC_ADMIN", 100, JAN_1, JAN_31),
                line("CC_HR", 100, FEB_1, LocalDate.of(2026, 2, 28)),
                line("CC_IT", 100, LocalDate.of(2026, 3, 1), null)
        );

        CostCenterDistributionPlan plan = service.planRemove(
                EMPLOYEE_ID,
                window(FEB_1, LocalDate.of(2026, 2, 28), line("CC_HR", 100, FEB_1, LocalDate.of(2026, 2, 28)))
        );

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertEquals(List.of(period(FEB_1, LocalDate.of(2026, 2, 28))), plan.gaps());
        assertEquals(
                List.of(period(JAN_1, JAN_31), period(LocalDate.of(2026, 3, 1), null)),
                plan.stretchCandidates()
        );
    }

    @Test
    void correctingTheDatesMovesNothingElse() {
        givenSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), line("CC_HR", 100, FEB_1, LocalDate.of(2026, 2, 28)));

        CostCenterDistributionPlan plan = service.planCorrect(
                EMPLOYEE_ID,
                window(FEB_1, LocalDate.of(2026, 2, 28), line("CC_HR", 100, FEB_1, LocalDate.of(2026, 2, 28))),
                range(FEB_1, null)
        );

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertNull(plan.adjustedOccurrence());
        assertEquals(period(FEB_1, null), plan.occurrence());
        assertEquals(period(FEB_1, LocalDate.of(2026, 2, 28)), plan.correctedOccurrence());
        assertEquals(List.of(period(JAN_1, JAN_31), period(FEB_1, null)), plan.projected());
    }

    // What the old replace-from-date did silently on the same start date is now said: the add
    // is the correction of the window that starts that day.
    @Test
    void addingOnTheStartDateOfAnExistingWindowIsRejectedAsItsCorrection() {
        givenSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), line("CC_HR", 60, FEB_1, null), line("CC_IT", 40, FEB_1, null));

        CostCenterDistributionPlan plan = service.planAdd(EMPLOYEE_ID, range(FEB_1, LocalDate.of(2026, 3, 31)));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(period(FEB_1, null), plan.correctedOccurrence());
        assertEquals(period(FEB_1, LocalDate.of(2026, 3, 31)), plan.occurrence());
        assertNull(plan.adjustedOccurrence());
        assertEquals(List.of(period(JAN_1, JAN_31), period(FEB_1, LocalDate.of(2026, 3, 31))), plan.projected());
    }

    @Test
    void aPlanRejectedAsACorrectionBecomesTheIsACorrectionExceptionNamingTheWindowToCorrect() {
        givenSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), line("CC_HR", 100, FEB_1, null));
        CostCenterDistributionPlan plan = service.planAdd(EMPLOYEE_ID, range(FEB_1, null));

        CostCenterDistributionIsACorrectionException ex = assertThrows(
                CostCenterDistributionIsACorrectionException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );

        assertEquals(period(FEB_1, null), ex.correctedOccurrence());
        assertEquals(period(FEB_1, null), ex.requested());
    }

    @Test
    void aRejectedPlanForAGapBecomesTheCoverageGapException() {
        givenSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), line("CC_HR", 100, FEB_1, LocalDate.of(2026, 2, 28)));
        CostCenterDistributionPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 4, 1), null));

        CostCenterDistributionCoverageGapException ex = assertThrows(
                CostCenterDistributionCoverageGapException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );

        assertEquals(plan.gaps(), ex.gaps());
        assertEquals(plan.stretchCandidates(), ex.stretchCandidates());
    }

    @Test
    void aRejectedPlanForAnOverlapBecomesTheOverlapExceptionWithTheSharedDates() {
        givenSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), line("CC_HR", 100, FEB_1, null));
        CostCenterDistributionPlan plan = service.planAdd(
                EMPLOYEE_ID,
                range(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 10))
        );

        CostCenterDistributionOverlapException ex = assertThrows(
                CostCenterDistributionOverlapException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );

        assertEquals(List.of(period(FEB_1, LocalDate.of(2026, 2, 10))), ex.overlaps());
    }

    @Test
    void aRejectedPlanOutsideThePresenceBecomesTheOutsidePresenceException() {
        givenSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), line("CC_HR", 100, FEB_1, null));
        CostCenterDistributionPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2025, 12, 1), null));

        assertThrows(
                CostCenterOutsidePresencePeriodException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );
    }

    @Test
    void anAcceptedPlanPassesThrough() {
        givenSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), line("CC_HR", 100, FEB_1, null));
        CostCenterDistributionPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 3, 16), null));

        service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001");
    }

    private void givenSeries(CostCenterAllocation... lines) {
        when(costCenterRepository.findByEmployeeIdOrderByStartDate(EMPLOYEE_ID)).thenReturn(List.of(lines));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of(new PresencePeriod(JAN_1, null)));
    }

    private static DateRange range(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }

    private static CostCenterDistributionPeriod period(LocalDate startDate, LocalDate endDate) {
        return new CostCenterDistributionPeriod(startDate, endDate);
    }

    private static CostCenterAllocation line(String code, int percentage, LocalDate startDate, LocalDate endDate) {
        return new CostCenterAllocation(EMPLOYEE_ID, code, BigDecimal.valueOf(percentage), startDate, endDate);
    }

    private static CostCenterDistributionWindow window(LocalDate startDate, LocalDate endDate, CostCenterAllocation... lines) {
        return new CostCenterDistributionWindow(startDate, endDate, new ArrayList<>(List.of(lines)));
    }
}
