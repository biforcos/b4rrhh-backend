package com.b4rrhh.employee.workcenter.application.usecase;

import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterContext;
import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterLookupPort;
import com.b4rrhh.employee.workcenter.application.port.PresencePeriod;
import com.b4rrhh.employee.workcenter.application.port.WorkCenterPresenceConsistencyPort;
import com.b4rrhh.employee.workcenter.application.service.WorkCenterTimelineService;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterPresenceCoverageGapException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterOccurrence;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterPeriod;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterRepository;
import com.b4rrhh.rulesystem.domain.model.RuleSystem;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * Removing a work center assignment (ADR-057, decision 3): the delete is
 * bounded by the invariants. The timeline service is real and the repository
 * and presence port are mocked: the employee is present from 2026-01-01
 * onwards.
 */
@ExtendWith(MockitoExtension.class)
class DeleteWorkCenterServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
    private static final LocalDate PRESENCE_START = LocalDate.of(2026, 1, 1);

    @Mock
    private WorkCenterRepository workCenterRepository;
    @Mock
    private EmployeeWorkCenterLookupPort employeeWorkCenterLookupPort;
    @Mock
    private WorkCenterPresenceConsistencyPort presencePort;
    @Mock
    private RuleSystemRepository ruleSystemRepository;

    private DeleteWorkCenterService service;

    @BeforeEach
    void setUp() {
        service = new DeleteWorkCenterService(
                workCenterRepository,
                employeeWorkCenterLookupPort,
                ruleSystemRepository,
                new WorkCenterTimelineService(workCenterRepository, presencePort)
        );
    }

    @Test
    void deletingTheLastOneReopensThePreviousOne() {
        WorkCenter first = workCenter(1, PRESENCE_START, LocalDate.of(2026, 1, 15));
        WorkCenter last = workCenter(2, LocalDate.of(2026, 1, 16), null);
        givenEmployeeWithSeries(first, last);
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 2)).thenReturn(Optional.of(last));
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 1)).thenReturn(Optional.of(first));
        when(workCenterRepository.save(any(WorkCenter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(command(2));

        ArgumentCaptor<WorkCenter> reopened = ArgumentCaptor.forClass(WorkCenter.class);
        verify(workCenterRepository).save(reopened.capture());
        assertEquals(1, reopened.getValue().getWorkCenterAssignmentNumber());
        assertEquals(PRESENCE_START, reopened.getValue().getStartDate());
        assertNull(reopened.getValue().getEndDate());
        verify(workCenterRepository).delete(last);
    }

    // The old rule ("the assignment that starts a presence cannot be deleted") is a case of
    // the gap invariant: the only assignment starts the presence, and removing it uncovers it.
    @Test
    void deletingTheOnlyOneWhichStartsThePresenceIsRejectedBecauseThePresenceWouldBeUncovered() {
        WorkCenter only = workCenter(1, PRESENCE_START, null);
        givenEmployeeWithSeries(only);
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 1)).thenReturn(Optional.of(only));

        WorkCenterPresenceCoverageGapException ex = assertThrows(
                WorkCenterPresenceCoverageGapException.class,
                () -> service.delete(command(1))
        );

        assertEquals(List.of(new WorkCenterPeriod(PRESENCE_START, null)), ex.gaps());
        verify(workCenterRepository, never()).delete(any());
        verify(workCenterRepository, never()).save(any());
    }

    @Test
    void deletingOneInTheMiddleIsRejectedNamingTheNeighboursToStretch() {
        WorkCenter first = workCenter(1, PRESENCE_START, LocalDate.of(2026, 1, 15));
        WorkCenter middle = workCenter(2, LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31));
        WorkCenter last = workCenter(3, LocalDate.of(2026, 2, 1), null);
        givenEmployeeWithSeries(first, middle, last);
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 2)).thenReturn(Optional.of(middle));

        WorkCenterPresenceCoverageGapException ex = assertThrows(
                WorkCenterPresenceCoverageGapException.class,
                () -> service.delete(command(2))
        );

        assertEquals(
                List.of(new WorkCenterPeriod(LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31))),
                ex.gaps()
        );
        assertEquals(
                List.of(
                        new WorkCenterOccurrence(1, PRESENCE_START, LocalDate.of(2026, 1, 15)),
                        new WorkCenterOccurrence(3, LocalDate.of(2026, 2, 1), null)
                ),
                ex.stretchCandidates()
        );
        verify(workCenterRepository, never()).delete(any());
        verify(workCenterRepository, never()).save(any());
    }

    @Test
    void throwsNotFoundWhenAssignmentDoesNotExistForEmployeeBusinessKey() {
        whenEmployeeExists();
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 99)).thenReturn(Optional.empty());

        assertThrows(WorkCenterNotFoundException.class, () -> service.delete(command(99)));

        verify(workCenterRepository).findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 99);
        verify(workCenterRepository, never()).delete(any());
    }

    private DeleteWorkCenterCommand command(int number) {
        return new DeleteWorkCenterCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER, number);
    }

    private void givenEmployeeWithSeries(WorkCenter... occurrences) {
        whenEmployeeExists();
        when(workCenterRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new PresencePeriod(PRESENCE_START, null)));
    }

    private void whenEmployeeExists() {
        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeWorkCenterLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(new EmployeeWorkCenterContext(10L, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER)));
    }

    private static WorkCenter workCenter(int number, LocalDate startDate, LocalDate endDate) {
        return new WorkCenter(
                (long) number,
                10L,
                number,
                "MADRID_HQ",
                startDate,
                endDate,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private RuleSystem ruleSystem(String code) {
        return new RuleSystem(
                1L,
                code,
                "Spain",
                "ESP",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
