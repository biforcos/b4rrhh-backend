package com.b4rrhh.employee.workcenter.application.usecase;

import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterContext;
import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterLookupPort;
import com.b4rrhh.employee.workcenter.application.port.PresencePeriod;
import com.b4rrhh.employee.workcenter.application.port.WorkCenterPresenceConsistencyPort;
import com.b4rrhh.employee.workcenter.application.service.WorkCenterTimelineService;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterAlreadyClosedException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterPresenceCoverageGapException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The deprecated {@code close} as a correction of the end date judged by the
 * component (ADR-057). The timeline service is real; the repository and the
 * presence port are mocked. Closing on the day the presence ends is the
 * termination flow and leaves no gap; closing while the presence goes on
 * does, and is rejected.
 */
@ExtendWith(MockitoExtension.class)
class CloseWorkCenterServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
    private static final LocalDate START = LocalDate.of(2026, 1, 10);

    @Mock
    private WorkCenterRepository workCenterRepository;
    @Mock
    private EmployeeWorkCenterLookupPort employeeWorkCenterLookupPort;
    @Mock
    private RuleSystemRepository ruleSystemRepository;
    @Mock
    private WorkCenterPresenceConsistencyPort presencePort;

    private CloseWorkCenterService service;

    @BeforeEach
    void setUp() {
        service = new CloseWorkCenterService(
                workCenterRepository,
                employeeWorkCenterLookupPort,
                ruleSystemRepository,
                new WorkCenterTimelineService(workCenterRepository, presencePort)
        );
    }

    @Test
    void closesWorkCenterAssignmentOnTheDayThePresenceEnds() {
        WorkCenter existing = activeWorkCenter(1, START);
        givenEmployeeWithSeries(new PresencePeriod(START, LocalDate.of(2026, 1, 20)), existing);
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 1)).thenReturn(Optional.of(existing));
        when(workCenterRepository.save(any(WorkCenter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkCenter closed = service.close(command(1, LocalDate.of(2026, 1, 20)));

        assertEquals(LocalDate.of(2026, 1, 20), closed.getEndDate());

        ArgumentCaptor<WorkCenter> captor = ArgumentCaptor.forClass(WorkCenter.class);
        verify(workCenterRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getWorkCenterAssignmentNumber());
        assertEquals(LocalDate.of(2026, 1, 20), captor.getValue().getEndDate());
    }

    @Test
    void allowsSameDayClose() {
        WorkCenter existing = activeWorkCenter(1, START);
        givenEmployeeWithSeries(new PresencePeriod(START, START), existing);
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 1)).thenReturn(Optional.of(existing));
        when(workCenterRepository.save(any(WorkCenter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkCenter closed = service.close(command(1, START));

        assertEquals(START, closed.getEndDate());
    }

    // ADR-057: closing the assignment in force while the presence goes on leaves the rest of
    // the presence uncovered. The next assignment is what closes this one.
    @Test
    void rejectsClosingWhileThePresenceGoesOnNamingTheGap() {
        WorkCenter existing = activeWorkCenter(1, START);
        givenEmployeeWithSeries(new PresencePeriod(START, null), existing);
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 1)).thenReturn(Optional.of(existing));

        WorkCenterPresenceCoverageGapException ex = assertThrows(
                WorkCenterPresenceCoverageGapException.class,
                () -> service.close(command(1, LocalDate.of(2026, 1, 20)))
        );

        assertEquals(List.of(new WorkCenterPeriod(LocalDate.of(2026, 1, 21), null)), ex.gaps());
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    @Test
    void rejectsCloseWhenAssignmentDoesNotExist() {
        whenEmployeeExists();
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 1)).thenReturn(Optional.empty());

        assertThrows(
                WorkCenterNotFoundException.class,
                () -> service.close(command(1, LocalDate.of(2026, 1, 20)))
        );
    }

    @Test
    void rejectsCloseWhenAlreadyClosed() {
        WorkCenter existing = new WorkCenter(
                11L, 10L, 1, "MADRID_HQ", START, LocalDate.of(2026, 1, 15), LocalDateTime.now(), LocalDateTime.now()
        );
        whenEmployeeExists();
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 1)).thenReturn(Optional.of(existing));

        assertThrows(
                WorkCenterAlreadyClosedException.class,
                () -> service.close(command(1, LocalDate.of(2026, 1, 20)))
        );
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    @Test
    void rejectsCloseWhenResultingPeriodIsOutsidePresenceHistory() {
        WorkCenter existing = activeWorkCenter(1, START);
        givenEmployeeWithSeries(new PresencePeriod(START, LocalDate.of(2026, 1, 15)), existing);
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 1)).thenReturn(Optional.of(existing));

        assertThrows(
                WorkCenterOutsidePresencePeriodException.class,
                () -> service.close(command(1, LocalDate.of(2026, 1, 20)))
        );
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    private CloseWorkCenterCommand command(int number, LocalDate endDate) {
        return new CloseWorkCenterCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER, number, endDate);
    }

    private void givenEmployeeWithSeries(PresencePeriod presence, WorkCenter... occurrences) {
        whenEmployeeExists();
        when(workCenterRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(presence));
    }

    private void whenEmployeeExists() {
        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeWorkCenterLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(new EmployeeWorkCenterContext(10L, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER)));
    }

    private WorkCenter activeWorkCenter(int assignmentNumber, LocalDate startDate) {
        return new WorkCenter(
                (long) assignmentNumber,
                10L,
                assignmentNumber,
                "MADRID_HQ",
                startDate,
                null,
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
