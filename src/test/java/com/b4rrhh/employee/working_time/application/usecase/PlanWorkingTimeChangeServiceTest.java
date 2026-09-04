package com.b4rrhh.employee.working_time.application.usecase;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import com.b4rrhh.employee.working_time.application.model.WorkingTimePlan;
import com.b4rrhh.employee.working_time.application.port.EmployeeWorkingTimeContext;
import com.b4rrhh.employee.working_time.application.port.EmployeeWorkingTimeLookupPort;
import com.b4rrhh.employee.working_time.application.port.WorkingTimePresenceConsistencyPort;
import com.b4rrhh.employee.working_time.application.service.WorkingTimeTimelineService;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeEmployeeNotFoundException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNotFoundException;
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
 * 2026-01-01 onwards with one open working time from that day.
 */
@ExtendWith(MockitoExtension.class)
class PlanWorkingTimeChangeServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
    private static final LocalDate PRESENCE_START = LocalDate.of(2026, 1, 1);
    private static final WorkingTimeDerivedHours DERIVED_HOURS = new WorkingTimeDerivedHours(
            new BigDecimal("20.00"), new BigDecimal("4.00"), new BigDecimal("86.80"));

    @Mock
    private WorkingTimeRepository workingTimeRepository;
    @Mock
    private EmployeeWorkingTimeLookupPort employeeWorkingTimeLookupPort;
    @Mock
    private WorkingTimePresenceConsistencyPort presencePort;

    private PlanWorkingTimeChangeService service;

    @BeforeEach
    void setUp() {
        service = new PlanWorkingTimeChangeService(
                workingTimeRepository,
                employeeWorkingTimeLookupPort,
                new WorkingTimeTimelineService(workingTimeRepository, presencePort)
        );
    }

    @Test
    void plansAnAddWithoutWritingAnything() {
        givenEmployeeWithSeries(workingTime(1, PRESENCE_START, null));

        WorkingTimePlan plan = service.plan(command(TimelineOperation.ADD, null, LocalDate.of(2026, 1, 16), null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.ADD, plan.operation());
        assertEquals(1, plan.adjustedOccurrence().workingTimeNumber());
        assertEquals(LocalDate.of(2026, 1, 15), plan.adjustedOccurrence().after().endDate());
        assertEquals(
                List.of(
                        new WorkingTimeOccurrence(1, PRESENCE_START, LocalDate.of(2026, 1, 15)),
                        new WorkingTimeOccurrence(null, LocalDate.of(2026, 1, 16), null)
                ),
                plan.projected()
        );
        verify(workingTimeRepository, never()).save(any());
        verify(workingTimeRepository, never()).delete(any());
    }

    @Test
    void aRejectedPlanComesBackAsAPlanNotAsAnError() {
        givenEmployeeWithSeries(workingTime(1, PRESENCE_START, LocalDate.of(2026, 1, 31)));

        WorkingTimePlan plan = service.plan(command(TimelineOperation.ADD, null, LocalDate.of(2026, 3, 1), null));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertEquals(List.of(new WorkingTimePeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))), plan.gaps());
    }

    @Test
    void plansARemovalOfANumberedOccurrence() {
        WorkingTime first = workingTime(1, PRESENCE_START, LocalDate.of(2026, 1, 15));
        WorkingTime last = workingTime(2, LocalDate.of(2026, 1, 16), null);
        givenEmployeeWithSeries(first, last);
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 2)).thenReturn(Optional.of(last));

        WorkingTimePlan plan = service.plan(command(TimelineOperation.REMOVE, 2, null, null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.REMOVE, plan.operation());
        assertEquals(1, plan.adjustedOccurrence().workingTimeNumber());
        assertEquals(List.of(new WorkingTimeOccurrence(1, PRESENCE_START, null)), plan.projected());
        verify(workingTimeRepository, never()).delete(any());
    }

    @Test
    void plansACorrectionOfANumberedOccurrence() {
        WorkingTime closedTooEarly = workingTime(1, PRESENCE_START, LocalDate.of(2026, 1, 31));
        givenEmployeeWithSeries(closedTooEarly);
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 1)).thenReturn(Optional.of(closedTooEarly));

        WorkingTimePlan plan = service.plan(command(TimelineOperation.CORRECT, 1, PRESENCE_START, null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(List.of(new WorkingTimeOccurrence(1, PRESENCE_START, null)), plan.projected());
        verify(workingTimeRepository, never()).save(any());
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
    void aRemovalNeedsAnExistingNumber() {
        whenEmployeeExists();
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 9)).thenReturn(Optional.empty());

        assertThrows(
                WorkingTimeNotFoundException.class,
                () -> service.plan(command(TimelineOperation.REMOVE, 9, null, null))
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
        when(employeeWorkingTimeLookupPort.findByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.empty());

        assertThrows(
                WorkingTimeEmployeeNotFoundException.class,
                () -> service.plan(command(TimelineOperation.ADD, null, PRESENCE_START, null))
        );
    }

    private PlanWorkingTimeChangeCommand command(
            TimelineOperation operation,
            Integer workingTimeNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new PlanWorkingTimeChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER, operation, workingTimeNumber, startDate, endDate
        );
    }

    private void givenEmployeeWithSeries(WorkingTime... occurrences) {
        whenEmployeeExists();
        when(workingTimeRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new DateRange(PRESENCE_START, null)));
    }

    private void whenEmployeeExists() {
        when(employeeWorkingTimeLookupPort.findByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(new EmployeeWorkingTimeContext(10L, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER)));
    }

    private static WorkingTime workingTime(int number, LocalDate startDate, LocalDate endDate) {
        return WorkingTime.rehydrate(
                (long) number,
                10L,
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
