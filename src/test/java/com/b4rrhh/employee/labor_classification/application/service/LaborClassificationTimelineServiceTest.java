package com.b4rrhh.employee.labor_classification.application.service;

import com.b4rrhh.employee.labor_classification.application.model.LaborClassificationPlan;
import com.b4rrhh.employee.labor_classification.application.model.LaborClassificationPlanAdjustment;
import com.b4rrhh.employee.labor_classification.application.port.LaborClassificationPresenceConsistencyPort;
import com.b4rrhh.employee.labor_classification.application.port.PresencePeriod;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationCoverageIncompleteException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationIsACorrectionException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationOutsidePresencePeriodException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationOverlapException;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassificationPeriod;
import com.b4rrhh.employee.labor_classification.domain.port.LaborClassificationRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * The series under test: the employee is present from 2026-01-01 onwards
 * and has two labor classifications, the second still open. A labor
 * classification is identified by the day it starts, so the plan names them
 * by their dates.
 *
 * <pre>
 *   AGR_OFFICE/CAT_ADMIN  2026-01-01 .. 2026-01-31
 *   AGR_TECH/CAT_TECH_1   2026-02-01 .. (open)
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class LaborClassificationTimelineServiceTest {

    private static final Long EMPLOYEE_ID = 10L;
    private static final LaborClassification FIRST = occurrence(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final LaborClassification SECOND = occurrence(LocalDate.of(2026, 2, 1), null);

    @Mock
    private LaborClassificationRepository laborClassificationRepository;
    @Mock
    private LaborClassificationPresenceConsistencyPort presencePort;

    private LaborClassificationTimelineService service;

    @BeforeEach
    void setUp() {
        service = new LaborClassificationTimelineService(laborClassificationRepository, presencePort);
    }

    @Test
    void addingAfterTheOpenOneClosesItTheDayBefore() {
        givenSeries(FIRST, SECOND);

        LaborClassificationPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 3, 16), null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.ADD, plan.operation());
        assertEquals(period(LocalDate.of(2026, 3, 16), null), plan.occurrence());
        assertEquals(
                new LaborClassificationPlanAdjustment(
                        period(LocalDate.of(2026, 2, 1), null),
                        period(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 15))
                ),
                plan.adjustedOccurrence()
        );
        assertEquals(
                List.of(
                        period(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        period(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 15)),
                        period(LocalDate.of(2026, 3, 16), null)
                ),
                plan.projected()
        );
    }

    @Test
    void addingInTheMiddleOfAClosedOneSplitsItAndTheTailIsAGap() {
        LaborClassification closedSecond = occurrence(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31));
        givenSeries(FIRST, closedSecond);

        LaborClassificationPlan plan = service.planAdd(
                EMPLOYEE_ID, range(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15)));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertEquals(
                new LaborClassificationPlanAdjustment(
                        period(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31)),
                        period(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))
                ),
                plan.adjustedOccurrence()
        );
        // The presence is open, so the gap runs from the day after the new one onwards.
        assertEquals(List.of(period(LocalDate.of(2026, 3, 16), null)), plan.gaps());
        assertEquals(List.of(period(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15))), plan.stretchCandidates());
    }

    @Test
    void addingThatLeavesAGapNamesTheGapAndTheNeighbours() {
        LaborClassification closedSecond = occurrence(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        givenSeries(FIRST, closedSecond);

        LaborClassificationPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 4, 1), null));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertNull(plan.adjustedOccurrence());
        assertEquals(List.of(period(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))), plan.gaps());
        assertEquals(
                List.of(
                        period(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)),
                        period(LocalDate.of(2026, 4, 1), null)
                ),
                plan.stretchCandidates()
        );
    }

    @Test
    void removingTheLastOneReopensThePreviousOne() {
        givenSeries(FIRST, SECOND);

        LaborClassificationPlan plan = service.planRemove(EMPLOYEE_ID, SECOND);

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.REMOVE, plan.operation());
        assertEquals(period(LocalDate.of(2026, 2, 1), null), plan.occurrence());
        assertEquals(
                new LaborClassificationPlanAdjustment(
                        period(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        period(LocalDate.of(2026, 1, 1), null)
                ),
                plan.adjustedOccurrence()
        );
        assertEquals(List.of(period(LocalDate.of(2026, 1, 1), null)), plan.projected());
    }

    @Test
    void removingOneInTheMiddleIsRejectedNamingTheNeighboursToStretch() {
        LaborClassification closedSecond = occurrence(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        LaborClassification third = occurrence(LocalDate.of(2026, 3, 1), null);
        givenSeries(FIRST, closedSecond, third);

        LaborClassificationPlan plan = service.planRemove(EMPLOYEE_ID, closedSecond);

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertEquals(List.of(period(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))), plan.gaps());
        assertEquals(
                List.of(
                        period(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        period(LocalDate.of(2026, 3, 1), null)
                ),
                plan.stretchCandidates()
        );
    }

    @Test
    void correctingMovesOnlyTheCorrectedOne() {
        LaborClassification closedTooEarly = occurrence(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        givenSeries(FIRST, closedTooEarly);

        LaborClassificationPlan plan = service.planCorrect(
                EMPLOYEE_ID, closedTooEarly, range(LocalDate.of(2026, 2, 1), null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertNull(plan.adjustedOccurrence());
        assertEquals(period(LocalDate.of(2026, 2, 1), null), plan.occurrence());
        assertEquals(period(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)), plan.correctedOccurrence());
        assertEquals(
                List.of(
                        period(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        period(LocalDate.of(2026, 2, 1), null)
                ),
                plan.projected()
        );
    }

    @Test
    void addingOnTheStartDateOfAnExistingOneIsRejectedAsItsCorrection() {
        givenSeries(FIRST, SECOND);

        LaborClassificationPlan plan = service.planAdd(
                EMPLOYEE_ID, range(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31)));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(period(LocalDate.of(2026, 2, 1), null), plan.correctedOccurrence());
        assertEquals(period(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31)), plan.occurrence());
        assertNull(plan.adjustedOccurrence());
    }

    @Test
    void aPlanRejectedAsACorrectionBecomesTheIsACorrectionExceptionNamingTheOccurrenceToCorrect() {
        givenSeries(FIRST, SECOND);
        LaborClassificationPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 2, 1), null));

        LaborClassificationIsACorrectionException ex = assertThrows(
                LaborClassificationIsACorrectionException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );

        assertEquals(period(LocalDate.of(2026, 2, 1), null), ex.correctedOccurrence());
        assertEquals(period(LocalDate.of(2026, 2, 1), null), ex.requested());
        assertTrue(ex.getMessage().contains("correct"), ex.getMessage());
    }

    @Test
    void aRejectedPlanForAGapBecomesTheCoverageIncompleteExceptionWithTheGapsNamed() {
        LaborClassification closedSecond = occurrence(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        givenSeries(FIRST, closedSecond);
        LaborClassificationPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 4, 1), null));

        LaborClassificationCoverageIncompleteException ex = assertThrows(
                LaborClassificationCoverageIncompleteException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );

        assertEquals(plan.gaps(), ex.gaps());
        assertEquals(plan.stretchCandidates(), ex.stretchCandidates());
    }

    @Test
    void aRejectedPlanForAnOverlapBecomesTheOverlapExceptionWithTheSharedDates() {
        givenSeries(FIRST, SECOND);
        LaborClassificationPlan plan = service.planAdd(
                EMPLOYEE_ID, range(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 10)));

        LaborClassificationOverlapException ex = assertThrows(
                LaborClassificationOverlapException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );

        assertEquals(List.of(period(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10))), ex.overlaps());
    }

    @Test
    void aRejectedPlanOutsideThePresenceBecomesTheOutsidePresenceException() {
        givenSeries(FIRST, SECOND);
        LaborClassificationPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2025, 12, 1), null));

        assertThrows(
                LaborClassificationOutsidePresencePeriodException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );
    }

    @Test
    void anAcceptedPlanPassesThrough() {
        givenSeries(FIRST, SECOND);
        LaborClassificationPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 3, 16), null));

        service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001");
    }

    private void givenSeries(LaborClassification... occurrences) {
        when(laborClassificationRepository.findByEmployeeIdOrderByStartDate(EMPLOYEE_ID)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of(new PresencePeriod(LocalDate.of(2026, 1, 1), null)));
    }

    private static DateRange range(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }

    private static LaborClassificationPeriod period(LocalDate startDate, LocalDate endDate) {
        return new LaborClassificationPeriod(startDate, endDate);
    }

    private static LaborClassification occurrence(LocalDate startDate, LocalDate endDate) {
        return new LaborClassification(EMPLOYEE_ID, "AGR_OFFICE", "CAT_ADMIN", startDate, endDate);
    }
}
