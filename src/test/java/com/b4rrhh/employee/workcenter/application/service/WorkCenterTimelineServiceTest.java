package com.b4rrhh.employee.workcenter.application.service;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import com.b4rrhh.employee.workcenter.application.model.WorkCenterPlan;
import com.b4rrhh.employee.workcenter.application.model.WorkCenterPlanAdjustment;
import com.b4rrhh.employee.workcenter.application.port.PresencePeriod;
import com.b4rrhh.employee.workcenter.application.port.WorkCenterPresenceConsistencyPort;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterIsACorrectionException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOverlapException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterPresenceCoverageGapException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterOccurrence;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterPeriod;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * The series under test: the employee is present from 2026-01-01 onwards
 * and has two work center assignments, the second still open.
 *
 * <pre>
 *   #1  2026-01-01 .. 2026-01-31
 *   #2  2026-02-01 .. (open)
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class WorkCenterTimelineServiceTest {

    private static final Long EMPLOYEE_ID = 10L;

    private static final WorkCenter FIRST = workCenter(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final WorkCenter SECOND = workCenter(2, LocalDate.of(2026, 2, 1), null);

    @Mock
    private WorkCenterRepository workCenterRepository;
    @Mock
    private WorkCenterPresenceConsistencyPort presencePort;

    private WorkCenterTimelineService service;

    @BeforeEach
    void setUp() {
        service = new WorkCenterTimelineService(workCenterRepository, presencePort);
    }

    @Test
    void addingAfterTheOpenOneClosesItTheDayBeforeAndNamesItByNumber() {
        givenSeries(FIRST, SECOND);

        WorkCenterPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 3, 16), null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.ADD, plan.operation());
        assertEquals(new WorkCenterOccurrence(null, LocalDate.of(2026, 3, 16), null), plan.occurrence());
        assertEquals(
                new WorkCenterPlanAdjustment(
                        2,
                        new WorkCenterPeriod(LocalDate.of(2026, 2, 1), null),
                        new WorkCenterPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 15))
                ),
                plan.adjustedOccurrence()
        );
        assertEquals(
                List.of(
                        new WorkCenterOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        new WorkCenterOccurrence(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 15)),
                        new WorkCenterOccurrence(null, LocalDate.of(2026, 3, 16), null)
                ),
                plan.projected()
        );
    }

    // The SPLIT the old planner did inside a closed assignment: the covering one is closed the
    // day before and the new one takes the effective date. The tail of the closed one is the
    // user's to give: the component never fabricates a third assignment (ADR-057 §3).
    @Test
    void addingInsideAClosedOneSplitsItAndTheTailIsTheNewOnesToTake() {
        WorkCenter closedSecond = workCenter(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31));
        WorkCenter third = workCenter(3, LocalDate.of(2026, 4, 1), null);
        givenSeries(FIRST, closedSecond, third);

        WorkCenterPlan plan = service.planAdd(
                EMPLOYEE_ID,
                range(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))
        );

        assertTrue(plan.isAccepted());
        assertEquals(
                new WorkCenterPlanAdjustment(
                        2,
                        new WorkCenterPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31)),
                        new WorkCenterPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))
                ),
                plan.adjustedOccurrence()
        );
        assertEquals(
                List.of(
                        new WorkCenterOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        new WorkCenterOccurrence(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)),
                        new WorkCenterOccurrence(null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)),
                        new WorkCenterOccurrence(3, LocalDate.of(2026, 4, 1), null)
                ),
                plan.projected()
        );
    }

    @Test
    void addingThatLeavesAGapNamesTheGapAndTheNeighboursByNumber() {
        WorkCenter closedSecond = workCenter(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        givenSeries(FIRST, closedSecond);

        WorkCenterPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 4, 1), null));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertNull(plan.adjustedOccurrence());
        assertEquals(List.of(new WorkCenterPeriod(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))), plan.gaps());
        assertEquals(
                List.of(
                        new WorkCenterOccurrence(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)),
                        new WorkCenterOccurrence(null, LocalDate.of(2026, 4, 1), null)
                ),
                plan.stretchCandidates()
        );
    }

    @Test
    void removingTheLastOneReopensThePreviousOne() {
        givenSeries(FIRST, SECOND);

        WorkCenterPlan plan = service.planRemove(EMPLOYEE_ID, SECOND);

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.REMOVE, plan.operation());
        assertEquals(new WorkCenterOccurrence(2, LocalDate.of(2026, 2, 1), null), plan.occurrence());
        assertEquals(
                new WorkCenterPlanAdjustment(
                        1,
                        new WorkCenterPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        new WorkCenterPeriod(LocalDate.of(2026, 1, 1), null)
                ),
                plan.adjustedOccurrence()
        );
        assertEquals(
                List.of(new WorkCenterOccurrence(1, LocalDate.of(2026, 1, 1), null)),
                plan.projected()
        );
    }

    @Test
    void removingOneInTheMiddleIsRejectedNamingTheNeighboursToStretch() {
        WorkCenter closedSecond = workCenter(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        WorkCenter third = workCenter(3, LocalDate.of(2026, 3, 1), null);
        givenSeries(FIRST, closedSecond, third);

        WorkCenterPlan plan = service.planRemove(EMPLOYEE_ID, closedSecond);

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertEquals(List.of(new WorkCenterPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))), plan.gaps());
        assertEquals(
                List.of(
                        new WorkCenterOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        new WorkCenterOccurrence(3, LocalDate.of(2026, 3, 1), null)
                ),
                plan.stretchCandidates()
        );
    }

    @Test
    void correctingKeepsTheNumberUnderTheNewDates() {
        WorkCenter closedTooEarly = workCenter(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        givenSeries(FIRST, closedTooEarly);

        WorkCenterPlan plan = service.planCorrect(EMPLOYEE_ID, closedTooEarly, range(LocalDate.of(2026, 2, 1), null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertNull(plan.adjustedOccurrence());
        assertEquals(new WorkCenterOccurrence(2, LocalDate.of(2026, 2, 1), null), plan.occurrence());
        assertEquals(
                List.of(
                        new WorkCenterOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        new WorkCenterOccurrence(2, LocalDate.of(2026, 2, 1), null)
                ),
                plan.projected()
        );
    }

    // What the old EXACT_START replaced silently is now said: the add is the correction of #2.
    @Test
    void addingOnTheStartDateOfAnExistingOneIsRejectedAsItsCorrectionAndNamesItByNumber() {
        givenSeries(FIRST, SECOND);

        WorkCenterPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31)));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(new WorkCenterOccurrence(2, LocalDate.of(2026, 2, 1), null), plan.correctedOccurrence());
        assertEquals(new WorkCenterOccurrence(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31)), plan.occurrence());
        assertNull(plan.adjustedOccurrence());
        assertEquals(
                List.of(
                        new WorkCenterOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        new WorkCenterOccurrence(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31))
                ),
                plan.projected()
        );
    }

    @Test
    void aPlanRejectedAsACorrectionBecomesTheIsACorrectionExceptionNamingTheAssignmentToCorrect() {
        givenSeries(FIRST, SECOND);
        WorkCenterPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 2, 1), null));

        WorkCenterIsACorrectionException ex = assertThrows(
                WorkCenterIsACorrectionException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );

        assertEquals(new WorkCenterOccurrence(2, LocalDate.of(2026, 2, 1), null), ex.correctedOccurrence());
        assertEquals(new WorkCenterPeriod(LocalDate.of(2026, 2, 1), null), ex.requested());
    }

    @Test
    void aRejectedPlanForAGapBecomesTheCoverageGapException() {
        WorkCenter closedSecond = workCenter(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        givenSeries(FIRST, closedSecond);
        WorkCenterPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 4, 1), null));

        WorkCenterPresenceCoverageGapException ex = assertThrows(
                WorkCenterPresenceCoverageGapException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );

        assertEquals(plan.gaps(), ex.gaps());
        assertEquals(plan.stretchCandidates(), ex.stretchCandidates());
    }

    @Test
    void aRejectedPlanForAnOverlapBecomesTheOverlapExceptionWithTheSharedDates() {
        givenSeries(FIRST, SECOND);
        WorkCenterPlan plan = service.planAdd(
                EMPLOYEE_ID,
                range(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 10))
        );

        WorkCenterOverlapException ex = assertThrows(
                WorkCenterOverlapException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );

        assertEquals(List.of(new WorkCenterPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10))), ex.overlaps());
    }

    @Test
    void aRejectedPlanOutsideThePresenceBecomesTheOutsidePresenceException() {
        givenSeries(FIRST, SECOND);
        WorkCenterPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2025, 12, 1), null));

        assertThrows(
                WorkCenterOutsidePresencePeriodException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );
    }

    @Test
    void anAcceptedPlanPassesThrough() {
        givenSeries(FIRST, SECOND);
        WorkCenterPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 3, 16), null));

        service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001");
    }

    private void givenSeries(WorkCenter... occurrences) {
        when(workCenterRepository.findByEmployeeIdOrderByStartDate(EMPLOYEE_ID)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of(new PresencePeriod(LocalDate.of(2026, 1, 1), null)));
    }

    private static DateRange range(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }

    private static WorkCenter workCenter(int number, LocalDate startDate, LocalDate endDate) {
        return new WorkCenter(
                (long) number,
                EMPLOYEE_ID,
                number,
                "MADRID_HQ",
                startDate,
                endDate,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
