package com.b4rrhh.employee.labor_classification.application.usecase;

import com.b4rrhh.employee.labor_classification.application.command.PlanLaborClassificationChangeCommand;
import com.b4rrhh.employee.labor_classification.application.model.LaborClassificationPlan;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationContext;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationLookupPort;
import com.b4rrhh.employee.labor_classification.application.port.LaborClassificationPresenceConsistencyPort;
import com.b4rrhh.employee.labor_classification.application.port.PresencePeriod;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationTimelineService;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationEmployeeNotFoundException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationNotFoundException;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassificationPeriod;
import com.b4rrhh.employee.labor_classification.domain.port.LaborClassificationRepository;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The plan is asked for and nothing is written. The employee is present from
 * 2026-01-01 onwards with one open labor classification from that day.
 */
@ExtendWith(MockitoExtension.class)
class PlanLaborClassificationChangeServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
    private static final LocalDate PRESENCE_START = LocalDate.of(2026, 1, 1);

    @Mock
    private LaborClassificationRepository laborClassificationRepository;
    @Mock
    private EmployeeLaborClassificationLookupPort employeeLaborClassificationLookupPort;
    @Mock
    private LaborClassificationPresenceConsistencyPort presencePort;

    private PlanLaborClassificationChangeService service;

    @BeforeEach
    void setUp() {
        service = new PlanLaborClassificationChangeService(
                laborClassificationRepository,
                employeeLaborClassificationLookupPort,
                new LaborClassificationTimelineService(laborClassificationRepository, presencePort)
        );
    }

    @Test
    void plansAnAddWithoutWritingAnything() {
        givenEmployeeWithSeries(occurrence(PRESENCE_START, null));

        LaborClassificationPlan plan = service.plan(command(TimelineOperation.ADD, null, LocalDate.of(2026, 1, 16), null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.ADD, plan.operation());
        assertEquals(PRESENCE_START, plan.adjustedOccurrence().before().startDate());
        assertEquals(LocalDate.of(2026, 1, 15), plan.adjustedOccurrence().after().endDate());
        assertEquals(
                List.of(
                        new LaborClassificationPeriod(PRESENCE_START, LocalDate.of(2026, 1, 15)),
                        new LaborClassificationPeriod(LocalDate.of(2026, 1, 16), null)
                ),
                plan.projected()
        );
        verify(laborClassificationRepository, never()).save(any());
        verify(laborClassificationRepository, never()).update(any(), any());
        verify(laborClassificationRepository, never()).delete(any());
    }

    @Test
    void aRejectedPlanComesBackAsAPlanNotAsAnError() {
        givenEmployeeWithSeries(occurrence(PRESENCE_START, LocalDate.of(2026, 1, 31)));

        LaborClassificationPlan plan = service.plan(command(TimelineOperation.ADD, null, LocalDate.of(2026, 3, 1), null));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertEquals(
                List.of(new LaborClassificationPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))),
                plan.gaps()
        );
    }

    @Test
    void thePlanSaysAnAddOnAnExistingStartDateIsACorrectionOfThatOccurrence() {
        givenEmployeeWithSeries(occurrence(PRESENCE_START, null));

        LaborClassificationPlan plan = service.plan(
                command(TimelineOperation.ADD, null, PRESENCE_START, LocalDate.of(2026, 1, 15)));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(new LaborClassificationPeriod(PRESENCE_START, null), plan.correctedOccurrence());
        assertEquals(List.of(new LaborClassificationPeriod(PRESENCE_START, LocalDate.of(2026, 1, 15))), plan.projected());
    }

    @Test
    void plansARemovalOfTheOccurrenceIdentifiedByItsStartDate() {
        LaborClassification first = occurrence(PRESENCE_START, LocalDate.of(2026, 1, 15));
        LaborClassification last = occurrence(LocalDate.of(2026, 1, 16), null);
        givenEmployeeWithSeries(first, last);
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, last.getStartDate())).thenReturn(Optional.of(last));

        LaborClassificationPlan plan = service.plan(command(TimelineOperation.REMOVE, LocalDate.of(2026, 1, 16), null, null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.REMOVE, plan.operation());
        assertEquals(PRESENCE_START, plan.adjustedOccurrence().before().startDate());
        assertEquals(List.of(new LaborClassificationPeriod(PRESENCE_START, null)), plan.projected());
        verify(laborClassificationRepository, never()).delete(any());
    }

    @Test
    void plansACorrectionOfTheOccurrenceIdentifiedByItsStartDate() {
        LaborClassification closedTooEarly = occurrence(PRESENCE_START, LocalDate.of(2026, 1, 31));
        givenEmployeeWithSeries(closedTooEarly);
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, PRESENCE_START)).thenReturn(Optional.of(closedTooEarly));

        LaborClassificationPlan plan = service.plan(command(TimelineOperation.CORRECT, PRESENCE_START, PRESENCE_START, null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(List.of(new LaborClassificationPeriod(PRESENCE_START, null)), plan.projected());
        verify(laborClassificationRepository, never()).update(any(), any());
    }

    @Test
    void anAddNeedsAStartDate() {
        whenEmployeeExists();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.plan(command(TimelineOperation.ADD, null, null, null))
        );
    }

    @Test
    void aRemovalNeedsTheStartDateOfAnExistingOccurrence() {
        whenEmployeeExists();
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, LocalDate.of(2026, 9, 1))).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.plan(command(TimelineOperation.REMOVE, null, null, null))
        );
        assertThrows(
                LaborClassificationNotFoundException.class,
                () -> service.plan(command(TimelineOperation.REMOVE, LocalDate.of(2026, 9, 1), null, null))
        );
    }

    @Test
    void theOperationIsRequired() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.plan(command(null, null, PRESENCE_START, null))
        );
    }

    @Test
    void rejectsWhenTheEmployeeDoesNotExist() {
        when(employeeLaborClassificationLookupPort.findByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.empty());

        assertThrows(
                LaborClassificationEmployeeNotFoundException.class,
                () -> service.plan(command(TimelineOperation.ADD, null, PRESENCE_START, null))
        );
    }

    private PlanLaborClassificationChangeCommand command(
            TimelineOperation operation,
            LocalDate laborClassificationStartDate,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new PlanLaborClassificationChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER, operation, laborClassificationStartDate, startDate, endDate
        );
    }

    private void givenEmployeeWithSeries(LaborClassification... occurrences) {
        whenEmployeeExists();
        when(laborClassificationRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new PresencePeriod(PRESENCE_START, null)));
    }

    private void whenEmployeeExists() {
        when(employeeLaborClassificationLookupPort.findByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(new EmployeeLaborClassificationContext(10L, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER)));
    }

    private static LaborClassification occurrence(LocalDate startDate, LocalDate endDate) {
        return new LaborClassification(10L, "AGR_OFFICE", "CAT_ADMIN", startDate, endDate);
    }
}
