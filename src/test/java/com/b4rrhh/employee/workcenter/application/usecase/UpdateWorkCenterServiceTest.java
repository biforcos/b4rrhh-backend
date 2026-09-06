package com.b4rrhh.employee.workcenter.application.usecase;

import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterContext;
import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterLookupPort;
import com.b4rrhh.employee.workcenter.application.port.PresencePeriod;
import com.b4rrhh.employee.workcenter.application.port.WorkCenterPresenceConsistencyPort;
import com.b4rrhh.employee.workcenter.application.service.WorkCenterCatalogValidator;
import com.b4rrhh.employee.workcenter.application.service.WorkCenterTimelineService;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCatalogValueInvalidException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOverlapException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Correcting a work center assignment through the temporal component
 * (ADR-057): the corrected dates are judged by the same invariants as an
 * add, and nothing else moves. The timeline service is real; the repository
 * and the presence port are mocked. The employee is present from 2026-01-01
 * onwards.
 */
@ExtendWith(MockitoExtension.class)
class UpdateWorkCenterServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
    private static final LocalDate PRESENCE_START = LocalDate.of(2026, 1, 1);

    @Mock
    private WorkCenterRepository workCenterRepository;
    @Mock
    private EmployeeWorkCenterLookupPort employeeWorkCenterLookupPort;
    @Mock
    private RuleSystemRepository ruleSystemRepository;
    @Mock
    private WorkCenterPresenceConsistencyPort presencePort;

    private UpdateWorkCenterService service;

    @BeforeEach
    void setUp() {
        service = new UpdateWorkCenterService(
                workCenterRepository,
                employeeWorkCenterLookupPort,
                ruleSystemRepository,
                new TestWorkCenterCatalogValidator(),
                new WorkCenterTimelineService(workCenterRepository, presencePort)
        );
    }

    // The employee left on 2026-02-20 and the open assignment is corrected to end with the presence.
    @Test
    void correctsTheCodeAndTheDatesOfTheAssignmentKeepingItsNumber() {
        WorkCenter first = workCenter(40L, 1, "MADRID_HQ", PRESENCE_START, LocalDate.of(2026, 1, 31));
        WorkCenter second = workCenter(41L, 2, "MADRID_HQ", LocalDate.of(2026, 2, 1), null);
        givenEmployeeWithSeries(first, second);
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new PresencePeriod(PRESENCE_START, LocalDate.of(2026, 2, 20))));
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 2)).thenReturn(Optional.of(second));
        when(workCenterRepository.save(any(WorkCenter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkCenter updated = service.update(command(2, "barcelona_hq", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 20)));

        assertEquals(41L, updated.getId());
        assertEquals(2, updated.getWorkCenterAssignmentNumber());
        assertEquals("BARCELONA_HQ", updated.getWorkCenterCode());
        assertEquals(LocalDate.of(2026, 2, 1), updated.getStartDate());
        assertEquals(LocalDate.of(2026, 2, 20), updated.getEndDate());

        ArgumentCaptor<WorkCenter> captor = ArgumentCaptor.forClass(WorkCenter.class);
        verify(workCenterRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getWorkCenterAssignmentNumber());
        assertEquals(41L, captor.getValue().getId());
    }

    // The correction the plan proposes for an add on an existing start date (backend#52) is
    // the PUT: same number, same start date, another code.
    @Test
    void correctsTheCodeOfTheOpenAssignmentWithoutTouchingItsDates() {
        WorkCenter open = workCenter(40L, 1, "MADRID_HQ", PRESENCE_START, null);
        givenEmployeeWithSeries(open);
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 1)).thenReturn(Optional.of(open));
        when(workCenterRepository.save(any(WorkCenter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkCenter updated = service.update(command(1, "BARCELONA_HQ", PRESENCE_START, null));

        assertEquals(1, updated.getWorkCenterAssignmentNumber());
        assertEquals("BARCELONA_HQ", updated.getWorkCenterCode());
        assertEquals(PRESENCE_START, updated.getStartDate());
        assertEquals(null, updated.getEndDate());
    }

    @Test
    void rejectsUpdateWhenAssignmentDoesNotBelongToEmployee() {
        whenEmployeeExists();
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 2)).thenReturn(Optional.empty());

        assertThrows(
                WorkCenterNotFoundException.class,
                () -> service.update(command(2, "BARCELONA_HQ", LocalDate.of(2026, 2, 1), null))
        );
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    @Test
    void rejectsOverlappingPeriodAgainstOtherAssignmentsNamingTheSharedDates() {
        WorkCenter first = workCenter(40L, 1, "MADRID_HQ", PRESENCE_START, LocalDate.of(2026, 1, 31));
        WorkCenter second = workCenter(41L, 2, "MADRID_HQ", LocalDate.of(2026, 2, 1), null);
        givenEmployeeWithSeries(first, second);
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 1)).thenReturn(Optional.of(first));

        WorkCenterOverlapException ex = assertThrows(
                WorkCenterOverlapException.class,
                () -> service.update(command(1, "MADRID_HQ", PRESENCE_START, LocalDate.of(2026, 2, 10)))
        );

        assertEquals(List.of(new WorkCenterPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10))), ex.overlaps());
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    @Test
    void rejectsACorrectionThatLeavesAGapNamingTheNeighbourToStretch() {
        WorkCenter first = workCenter(40L, 1, "MADRID_HQ", PRESENCE_START, LocalDate.of(2026, 1, 31));
        WorkCenter second = workCenter(41L, 2, "MADRID_HQ", LocalDate.of(2026, 2, 1), null);
        givenEmployeeWithSeries(first, second);
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 2)).thenReturn(Optional.of(second));

        WorkCenterPresenceCoverageGapException ex = assertThrows(
                WorkCenterPresenceCoverageGapException.class,
                () -> service.update(command(2, "MADRID_HQ", LocalDate.of(2026, 3, 1), null))
        );

        assertEquals(List.of(new WorkCenterPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))), ex.gaps());
        assertEquals(
                List.of(
                        new WorkCenterOccurrence(1, PRESENCE_START, LocalDate.of(2026, 1, 31)),
                        new WorkCenterOccurrence(2, LocalDate.of(2026, 3, 1), null)
                ),
                ex.stretchCandidates()
        );
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    @Test
    void rejectsUpdateOutsidePresenceHistory() {
        WorkCenter only = workCenter(40L, 1, "MADRID_HQ", PRESENCE_START, null);
        givenEmployeeWithSeries(only);
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 1)).thenReturn(Optional.of(only));

        assertThrows(
                WorkCenterOutsidePresencePeriodException.class,
                () -> service.update(command(1, "MADRID_HQ", LocalDate.of(2025, 12, 1), null))
        );
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    @Test
    void rejectsInvalidCatalogValue() {
        WorkCenter only = workCenter(40L, 1, "MADRID_HQ", PRESENCE_START, null);
        whenEmployeeExists();
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 1)).thenReturn(Optional.of(only));

        assertThrows(
                WorkCenterCatalogValueInvalidException.class,
                () -> service.update(command(1, "bad", PRESENCE_START, null))
        );
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    private UpdateWorkCenterCommand command(int number, String workCenterCode, LocalDate startDate, LocalDate endDate) {
        return new UpdateWorkCenterCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                number,
                workCenterCode,
                startDate,
                endDate
        );
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

    private static WorkCenter workCenter(
            Long id,
            int number,
            String workCenterCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new WorkCenter(
                id,
                10L,
                number,
                workCenterCode,
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

    private static final class TestWorkCenterCatalogValidator extends WorkCenterCatalogValidator {

        private TestWorkCenterCatalogValidator() {
            super(null);
        }

        @Override
        public void validateWorkCenterCode(String ruleSystemCode, String workCenterCode, LocalDate referenceDate) {
            if ("BAD".equals(workCenterCode)) {
                throw new WorkCenterCatalogValueInvalidException("workCenterCode", workCenterCode);
            }
        }

        @Override
        public String normalizeRequiredCode(String fieldName, String value) {
            if (value == null || value.trim().isEmpty()) {
                throw new WorkCenterCatalogValueInvalidException(fieldName, String.valueOf(value));
            }

            return value.trim().toUpperCase();
        }
    }
}
