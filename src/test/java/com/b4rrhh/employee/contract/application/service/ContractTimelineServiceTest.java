package com.b4rrhh.employee.contract.application.service;

import com.b4rrhh.employee.contract.application.model.ContractPlan;
import com.b4rrhh.employee.contract.application.model.ContractPlanAdjustment;
import com.b4rrhh.employee.contract.application.port.ContractPresenceConsistencyPort;
import com.b4rrhh.employee.contract.application.port.PresencePeriod;
import com.b4rrhh.employee.contract.domain.exception.ContractCoverageIncompleteException;
import com.b4rrhh.employee.contract.domain.exception.ContractIsACorrectionException;
import com.b4rrhh.employee.contract.domain.exception.ContractOutsidePresencePeriodException;
import com.b4rrhh.employee.contract.domain.exception.ContractOverlapException;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.contract.domain.model.ContractPeriod;
import com.b4rrhh.employee.contract.domain.port.ContractRepository;
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
 * and has two contracts, the second still open. A contract is identified by
 * the day it starts, so the plan names them by their dates.
 *
 * <pre>
 *   IND/FT1  2026-01-01 .. 2026-01-31
 *   TMP/PT1  2026-02-01 .. (open)
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class ContractTimelineServiceTest {

    private static final Long EMPLOYEE_ID = 10L;
    private static final Contract FIRST = contract(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final Contract SECOND = contract(LocalDate.of(2026, 2, 1), null);

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ContractPresenceConsistencyPort presencePort;

    private ContractTimelineService service;

    @BeforeEach
    void setUp() {
        service = new ContractTimelineService(contractRepository, presencePort);
    }

    @Test
    void addingAfterTheOpenOneClosesItTheDayBefore() {
        givenSeries(FIRST, SECOND);

        ContractPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 3, 16), null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.ADD, plan.operation());
        assertEquals(period(LocalDate.of(2026, 3, 16), null), plan.occurrence());
        assertEquals(
                new ContractPlanAdjustment(
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
        Contract closedSecond = contract(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31));
        givenSeries(FIRST, closedSecond);

        ContractPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15)));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertEquals(
                new ContractPlanAdjustment(
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
        Contract closedSecond = contract(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        givenSeries(FIRST, closedSecond);

        ContractPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 4, 1), null));

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

        ContractPlan plan = service.planRemove(EMPLOYEE_ID, SECOND);

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.REMOVE, plan.operation());
        assertEquals(period(LocalDate.of(2026, 2, 1), null), plan.occurrence());
        assertEquals(
                new ContractPlanAdjustment(
                        period(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                        period(LocalDate.of(2026, 1, 1), null)
                ),
                plan.adjustedOccurrence()
        );
        assertEquals(List.of(period(LocalDate.of(2026, 1, 1), null)), plan.projected());
    }

    @Test
    void removingOneInTheMiddleIsRejectedNamingTheNeighboursToStretch() {
        Contract closedSecond = contract(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        Contract third = contract(LocalDate.of(2026, 3, 1), null);
        givenSeries(FIRST, closedSecond, third);

        ContractPlan plan = service.planRemove(EMPLOYEE_ID, closedSecond);

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
        Contract closedTooEarly = contract(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        givenSeries(FIRST, closedTooEarly);

        ContractPlan plan = service.planCorrect(EMPLOYEE_ID, closedTooEarly, range(LocalDate.of(2026, 2, 1), null));

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

        ContractPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31)));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(period(LocalDate.of(2026, 2, 1), null), plan.correctedOccurrence());
        assertEquals(period(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31)), plan.occurrence());
        assertNull(plan.adjustedOccurrence());
    }

    @Test
    void aPlanRejectedAsACorrectionBecomesTheIsACorrectionExceptionNamingTheContractToCorrect() {
        givenSeries(FIRST, SECOND);
        ContractPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 2, 1), null));

        ContractIsACorrectionException ex = assertThrows(
                ContractIsACorrectionException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );

        assertEquals(period(LocalDate.of(2026, 2, 1), null), ex.correctedOccurrence());
        assertEquals(period(LocalDate.of(2026, 2, 1), null), ex.requested());
        assertTrue(ex.getMessage().contains("correct"), ex.getMessage());
    }

    @Test
    void aRejectedPlanForAGapBecomesTheCoverageIncompleteExceptionWithTheGapsNamed() {
        Contract closedSecond = contract(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        givenSeries(FIRST, closedSecond);
        ContractPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 4, 1), null));

        ContractCoverageIncompleteException ex = assertThrows(
                ContractCoverageIncompleteException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );

        assertEquals(plan.gaps(), ex.gaps());
        assertEquals(plan.stretchCandidates(), ex.stretchCandidates());
    }

    @Test
    void aRejectedPlanForAnOverlapBecomesTheOverlapExceptionWithTheSharedDates() {
        givenSeries(FIRST, SECOND);
        ContractPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 10)));

        ContractOverlapException ex = assertThrows(
                ContractOverlapException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );

        assertEquals(List.of(period(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10))), ex.overlaps());
    }

    @Test
    void aRejectedPlanOutsideThePresenceBecomesTheOutsidePresenceException() {
        givenSeries(FIRST, SECOND);
        ContractPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2025, 12, 1), null));

        assertThrows(
                ContractOutsidePresencePeriodException.class,
                () -> service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001")
        );
    }

    @Test
    void anAcceptedPlanPassesThrough() {
        givenSeries(FIRST, SECOND);
        ContractPlan plan = service.planAdd(EMPLOYEE_ID, range(LocalDate.of(2026, 3, 16), null));

        service.requireAccepted(plan, "ESP", "INTERNAL", "EMP001");
    }

    private void givenSeries(Contract... occurrences) {
        when(contractRepository.findByEmployeeIdOrderByStartDate(EMPLOYEE_ID)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of(new PresencePeriod(LocalDate.of(2026, 1, 1), null)));
    }

    private static DateRange range(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }

    private static ContractPeriod period(LocalDate startDate, LocalDate endDate) {
        return new ContractPeriod(startDate, endDate);
    }

    private static Contract contract(LocalDate startDate, LocalDate endDate) {
        return new Contract(EMPLOYEE_ID, "IND", "FT1", startDate, endDate);
    }
}
