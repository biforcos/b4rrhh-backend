package com.b4rrhh.employee.working_time.application.usecase;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.working_time.application.port.EmployeeWorkingTimeContext;
import com.b4rrhh.employee.working_time.application.port.EmployeeWorkingTimeLookupPort;
import com.b4rrhh.employee.working_time.application.port.WorkingTimePresenceConsistencyPort;
import com.b4rrhh.employee.working_time.application.service.WorkingTimeTimelineService;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeCoverageGapException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNotFoundException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeDerivedHours;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeOccurrence;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimePeriod;
import com.b4rrhh.employee.working_time.domain.port.WorkingTimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Removing a working time (ADR-057, decision 3). The timeline service is
 * real and the repository and presence port are mocked: the employee is
 * present from 2026-01-01 onwards.
 */
@ExtendWith(MockitoExtension.class)
class DeleteWorkingTimeServiceTest {

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

    private DeleteWorkingTimeService service;

    @BeforeEach
    void setUp() {
        service = new DeleteWorkingTimeService(
                workingTimeRepository,
                employeeWorkingTimeLookupPort,
                new WorkingTimeTimelineService(workingTimeRepository, presencePort)
        );
    }

    @Test
    void deletingTheLastOneReopensThePreviousOne() {
        WorkingTime first = workingTime(1, PRESENCE_START, LocalDate.of(2026, 1, 15));
        WorkingTime last = workingTime(2, LocalDate.of(2026, 1, 16), null);
        givenEmployeeWithSeries(first, last);
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 2)).thenReturn(Optional.of(last));
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 1)).thenReturn(Optional.of(first));
        when(workingTimeRepository.save(any(WorkingTime.class))).thenAnswer(inv -> inv.getArgument(0));

        service.delete(command(2));

        ArgumentCaptor<WorkingTime> reopened = ArgumentCaptor.forClass(WorkingTime.class);
        verify(workingTimeRepository).save(reopened.capture());
        assertEquals(1, reopened.getValue().getWorkingTimeNumber());
        assertEquals(PRESENCE_START, reopened.getValue().getStartDate());
        assertNull(reopened.getValue().getEndDate());
        verify(workingTimeRepository).delete(last);
    }

    @Test
    void deletingTheOnlyOneIsRejectedBecauseThePresenceWouldBeUncovered() {
        WorkingTime only = workingTime(1, PRESENCE_START, null);
        givenEmployeeWithSeries(only);
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 1)).thenReturn(Optional.of(only));

        WorkingTimeCoverageGapException ex = assertThrows(
                WorkingTimeCoverageGapException.class,
                () -> service.delete(command(1))
        );

        assertEquals(List.of(new WorkingTimePeriod(PRESENCE_START, null)), ex.gaps());
        verify(workingTimeRepository, never()).delete(any());
        verify(workingTimeRepository, never()).save(any());
    }

    @Test
    void deletingOneInTheMiddleIsRejectedNamingTheNeighboursToStretch() {
        WorkingTime first = workingTime(1, PRESENCE_START, LocalDate.of(2026, 1, 15));
        WorkingTime middle = workingTime(2, LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31));
        WorkingTime last = workingTime(3, LocalDate.of(2026, 2, 1), null);
        givenEmployeeWithSeries(first, middle, last);
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 2)).thenReturn(Optional.of(middle));

        WorkingTimeCoverageGapException ex = assertThrows(
                WorkingTimeCoverageGapException.class,
                () -> service.delete(command(2))
        );

        assertEquals(
                List.of(new WorkingTimePeriod(LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31))),
                ex.gaps()
        );
        assertEquals(
                List.of(
                        new WorkingTimeOccurrence(1, PRESENCE_START, LocalDate.of(2026, 1, 15)),
                        new WorkingTimeOccurrence(3, LocalDate.of(2026, 2, 1), null)
                ),
                ex.stretchCandidates()
        );
        verify(workingTimeRepository, never()).delete(any());
        verify(workingTimeRepository, never()).save(any());
    }

    @Test
    void rejectsWhenTheWorkingTimeDoesNotExist() {
        whenEmployeeExists();
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 7)).thenReturn(Optional.empty());

        assertThrows(WorkingTimeNotFoundException.class, () -> service.delete(command(7)));
        verify(workingTimeRepository, never()).delete(any());
    }

    private DeleteWorkingTimeCommand command(int number) {
        return new DeleteWorkingTimeCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER, number);
    }

    private void givenEmployeeWithSeries(WorkingTime... occurrences) {
        whenEmployeeExists();
        when(workingTimeRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new DateRange(PRESENCE_START, null)));
    }

    private void whenEmployeeExists() {
        when(employeeWorkingTimeLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
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
