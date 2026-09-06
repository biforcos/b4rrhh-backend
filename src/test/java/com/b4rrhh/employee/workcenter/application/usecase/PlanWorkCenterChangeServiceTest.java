package com.b4rrhh.employee.workcenter.application.usecase;

import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import com.b4rrhh.employee.workcenter.application.model.WorkCenterPlan;
import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterContext;
import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterLookupPort;
import com.b4rrhh.employee.workcenter.application.port.PresencePeriod;
import com.b4rrhh.employee.workcenter.application.port.WorkCenterPresenceConsistencyPort;
import com.b4rrhh.employee.workcenter.application.service.WorkCenterTimelineService;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterEmployeeNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterNotFoundException;
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
 * The plan use case answers and never writes (ADR-057, decision 6). The
 * timeline service is real; the employee is present from 2026-01-01 onwards
 * with one open assignment #1 from that day.
 */
@ExtendWith(MockitoExtension.class)
class PlanWorkCenterChangeServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
    private static final LocalDate PRESENCE_START = LocalDate.of(2026, 1, 1);
    private static final WorkCenter OPEN = workCenter(1, PRESENCE_START, null);

    @Mock
    private WorkCenterRepository workCenterRepository;
    @Mock
    private EmployeeWorkCenterLookupPort employeeWorkCenterLookupPort;
    @Mock
    private WorkCenterPresenceConsistencyPort presencePort;

    private PlanWorkCenterChangeService service;

    @BeforeEach
    void setUp() {
        service = new PlanWorkCenterChangeService(
                workCenterRepository,
                employeeWorkCenterLookupPort,
                new WorkCenterTimelineService(workCenterRepository, presencePort)
        );
    }

    @Test
    void anAddIsPlannedWithoutTouchingTheRepository() {
        givenEmployeeWithSeries(OPEN);

        WorkCenterPlan plan = service.plan(command(TimelineOperation.ADD, null, LocalDate.of(2026, 2, 1), null));

        assertTrue(plan.isAccepted());
        assertEquals(1, plan.adjustedOccurrence().workCenterAssignmentNumber());
        assertEquals(LocalDate.of(2026, 1, 31), plan.adjustedOccurrence().after().endDate());
        assertEquals(
                List.of(
                        new WorkCenterOccurrence(1, PRESENCE_START, LocalDate.of(2026, 1, 31)),
                        new WorkCenterOccurrence(null, LocalDate.of(2026, 2, 1), null)
                ),
                plan.projected()
        );
        verify(workCenterRepository, never()).save(any());
        verify(workCenterRepository, never()).delete(any());
    }

    @Test
    void anAddOnAnExistingStartDateComesBackAsTheCorrectionOfThatAssignmentRejectedAsSuch() {
        givenEmployeeWithSeries(OPEN);

        WorkCenterPlan plan = service.plan(command(TimelineOperation.ADD, null, PRESENCE_START, LocalDate.of(2026, 1, 15)));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(new WorkCenterOccurrence(1, PRESENCE_START, null), plan.correctedOccurrence());
        assertEquals(List.of(new WorkCenterOccurrence(1, PRESENCE_START, LocalDate.of(2026, 1, 15))), plan.projected());
    }

    @Test
    void aRemovalIsPlannedByNumber() {
        WorkCenter closedFirst = workCenter(1, PRESENCE_START, LocalDate.of(2026, 1, 31));
        WorkCenter second = workCenter(2, LocalDate.of(2026, 2, 1), null);
        givenEmployeeWithSeries(closedFirst, second);
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 2)).thenReturn(Optional.of(second));

        WorkCenterPlan plan = service.plan(command(TimelineOperation.REMOVE, 2, null, null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.REMOVE, plan.operation());
        assertEquals(1, plan.adjustedOccurrence().workCenterAssignmentNumber());
        assertEquals(List.of(new WorkCenterOccurrence(1, PRESENCE_START, null)), plan.projected());
        verify(workCenterRepository, never()).delete(any());
    }

    @Test
    void aCorrectionThatLeavesAGapComesBackRejectedNamingTheGap() {
        WorkCenter closedFirst = workCenter(1, PRESENCE_START, LocalDate.of(2026, 1, 31));
        WorkCenter second = workCenter(2, LocalDate.of(2026, 2, 1), null);
        givenEmployeeWithSeries(closedFirst, second);
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 2)).thenReturn(Optional.of(second));

        WorkCenterPlan plan = service.plan(command(TimelineOperation.CORRECT, 2, LocalDate.of(2026, 3, 1), null));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertEquals(List.of(new WorkCenterPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))), plan.gaps());
        verify(workCenterRepository, never()).save(any());
    }

    @Test
    void rejectsARemovalWithoutANumber() {
        whenEmployeeExists();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.plan(command(TimelineOperation.REMOVE, null, null, null))
        );
    }

    @Test
    void rejectsAnAddWithoutAStartDate() {
        whenEmployeeExists();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.plan(command(TimelineOperation.ADD, null, null, null))
        );
    }

    @Test
    void rejectsAnUnknownAssignment() {
        whenEmployeeExists();
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 7)).thenReturn(Optional.empty());

        assertThrows(
                WorkCenterNotFoundException.class,
                () -> service.plan(command(TimelineOperation.REMOVE, 7, null, null))
        );
    }

    @Test
    void rejectsAnUnknownEmployee() {
        when(employeeWorkCenterLookupPort.findByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.empty());

        assertThrows(
                WorkCenterEmployeeNotFoundException.class,
                () -> service.plan(command(TimelineOperation.ADD, null, LocalDate.of(2026, 2, 1), null))
        );
    }

    private PlanWorkCenterChangeCommand command(
            TimelineOperation operation,
            Integer number,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new PlanWorkCenterChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER, operation, number, startDate, endDate
        );
    }

    private void givenEmployeeWithSeries(WorkCenter... occurrences) {
        whenEmployeeExists();
        when(workCenterRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new PresencePeriod(PRESENCE_START, null)));
    }

    private void whenEmployeeExists() {
        when(employeeWorkCenterLookupPort.findByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
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
}
