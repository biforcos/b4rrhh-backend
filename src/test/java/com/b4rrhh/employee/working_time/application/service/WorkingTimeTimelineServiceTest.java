package com.b4rrhh.employee.working_time.application.service;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import com.b4rrhh.employee.working_time.application.model.WorkingTimePlan;
import com.b4rrhh.employee.working_time.application.model.WorkingTimePlanAdjustment;
import com.b4rrhh.employee.working_time.application.port.WorkingTimePresenceConsistencyPort;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeCoverageGapException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOutsidePresencePeriodException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOverlapException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeDerivedHours;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeOccurrence;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimePeriod;
import com.b4rrhh.employee.working_time.domain.port.WorkingTimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
 * and has two working times, the second still open.
 *
 * <pre>
 *   #1  2026-01-01 .. 2026-01-31
 *   #2  2026-02-01 .. (open)
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class WorkingTimeTimelineServiceTest {

    private static final Long EMPLOYEE_ID = 10L;
    private static final WorkingTimeDerivedHours DERIVED_HOURS = new WorkingTimeDerivedHours(
            new BigDecimal("20.00"), new BigDecimal("4.00"), new BigDecimal("86.80"));

    private static final WorkingTime FIRST = workingTime(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final WorkingTime SECOND = workingTime(2, LocalDate.of(2026, 2, 1), null);

    @Mock
    private WorkingTimeRepository workingTimeRepository;
    @Mock
    private WorkingTimePresenceConsistencyPort presencePort;

    private WorkingTimeTimelineService service;

    @BeforeEach
    void setUp() {
        service = new WorkingTimeTimelineService(workingTimeRepository, presencePort);
    }

    @Test
    void addingAfterTheOpenOneClosesItTheDayBeforeAndNamesItByNumber() {
        givenSeries(FIRST, SECOND);

        WorkingTimePlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 3, 16), null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.ADD, plan.operation());
        assertEquals(new WorkingTimeOccurrence(null, LocalDate.of(2026, 3, 16), null), plan.occurrence());
        assertEquals(
                new WorkingTimePlanAdjustment(
                        2,
                        new WorkingTimePeriod(LocalDate.of(2026, 2, 1), null),
                        new WorkingTimePeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 15))
                ),
                plan.adjustedOccurrence()
        );
        assertEquals(
                List.of(
                        new WorkingTimeOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        new WorkingTimeOccurrence(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 15)),
                        new WorkingTimeOccurrence(null, LocalDate.of(2026, 3, 16), null)
                ),
                plan.projected()
        );
    }

    @Test
    void addingThatLeavesAGapNamesTheGapAndTheNeighboursByNumber() {
        WorkingTime closedSecond = workingTime(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        givenSeries(FIRST, closedSecond);

        WorkingTimePlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 4, 1), null));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertNull(plan.adjustedOccurrence());
        assertEquals(List.of(new WorkingTimePeriod(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))), plan.gaps());
        assertEquals(
                List.of(
                        new WorkingTimeOccurrence(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)),
                        new WorkingTimeOccurrence(null, LocalDate.of(2026, 4, 1), null)
                ),
                plan.stretchCandidates()
        );
    }

    @Test
    void removingTheLastOneReopensThePreviousOne() {
        givenSeries(FIRST, SECOND);

        WorkingTimePlan plan = service.planRemove(EMPLOYEE_ID, SECOND);

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.REMOVE, plan.operation());
        assertEquals(new WorkingTimeOccurrence(2, LocalDate.of(2026, 2, 1), null), plan.occurrence());
        assertEquals(
                new WorkingTimePlanAdjustment(
                        1,
                        new WorkingTimePeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        new WorkingTimePeriod(LocalDate.of(2026, 1, 1), null)
                ),
                plan.adjustedOccurrence()
        );
        assertEquals(
                List.of(new WorkingTimeOccurrence(1, LocalDate.of(2026, 1, 1), null)),
                plan.projected()
        );
    }

    @Test
    void removingOneInTheMiddleIsRejectedNamingTheNeighboursToStretch() {
        WorkingTime closedSecond = workingTime(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        WorkingTime third = workingTime(3, LocalDate.of(2026, 3, 1), null);
        givenSeries(FIRST, closedSecond, third);

        WorkingTimePlan plan = service.planRemove(EMPLOYEE_ID, closedSecond);

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertEquals(List.of(new WorkingTimePeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))), plan.gaps());
        assertEquals(
                List.of(
                        new WorkingTimeOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        new WorkingTimeOccurrence(3, LocalDate.of(2026, 3, 1), null)
                ),
                plan.stretchCandidates()
        );
    }

    @Test
    void correctingKeepsTheNumberUnderTheNewDates() {
        WorkingTime closedTooEarly = workingTime(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        givenSeries(FIRST, closedTooEarly);

        WorkingTimePlan plan = service.planCorrect(EMPLOYEE_ID, closedTooEarly, range(LocalDate.of(2026, 2, 1), null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertNull(plan.adjustedOccurrence());
        assertEquals(new WorkingTimeOccurrence(2, LocalDate.of(2026, 2, 1), null), plan.occurrence());
        assertEquals(
                List.of(
                        new WorkingTimeOccurrence(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        new WorkingTimeOccurrence(2, LocalDate.of(2026, 2, 1), null)
                ),
                plan.projected()
        );
    }

    @Test
    void aRejectedPlanForAGapBecomesTheGapException() {
        WorkingTime closedSecond = workingTime(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        givenSeries(FIRST, closedSecond);
        WorkingTimePlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 4, 1), null));

        WorkingTimeCoverageGapException ex = assertThrows(
                WorkingTimeCoverageGapException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );

        assertEquals(plan.gaps(), ex.gaps());
        assertEquals(plan.stretchCandidates(), ex.stretchCandidates());
    }

    @Test
    void aRejectedPlanForAnOverlapBecomesTheOverlapExceptionWithTheSharedDates() {
        givenSeries(FIRST, SECOND);
        WorkingTimePlan plan = service.planAdd(
                EMPLOYEE_ID,
                range(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 10))
        );

        WorkingTimeOverlapException ex = assertThrows(
                WorkingTimeOverlapException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );

        assertEquals(List.of(new WorkingTimePeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10))), ex.overlaps());
    }

    @Test
    void aRejectedPlanOutsideThePresenceBecomesTheOutsidePresenceException() {
        givenSeries(FIRST, SECOND);
        WorkingTimePlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2025, 12, 1), null));

        assertThrows(
                WorkingTimeOutsidePresencePeriodException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );
    }

    @Test
    void anAcceptedPlanPassesThrough() {
        givenSeries(FIRST, SECOND);
        WorkingTimePlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 3, 16), null));

        service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001");
    }

    private void givenSeries(WorkingTime... occurrences) {
        when(workingTimeRepository.findByEmployeeIdOrderByStartDate(EMPLOYEE_ID)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of(range(LocalDate.of(2026, 1, 1), null)));
    }

    private static DateRange range(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }

    private static WorkingTime workingTime(int number, LocalDate startDate, LocalDate endDate) {
        return WorkingTime.rehydrate(
                (long) number,
                EMPLOYEE_ID,
                number,
                startDate,
                endDate,
                new BigDecimal("50"),
                DERIVED_HOURS,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
