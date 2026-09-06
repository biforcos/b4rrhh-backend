package com.b4rrhh.employee.workcenter.application.usecase;

import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterContext;
import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterLookupPort;
import com.b4rrhh.employee.workcenter.application.port.PresencePeriod;
import com.b4rrhh.employee.workcenter.application.port.WorkCenterPresenceConsistencyPort;
import com.b4rrhh.employee.workcenter.application.service.WorkCenterCatalogValidator;
import com.b4rrhh.employee.workcenter.application.service.WorkCenterTimelineService;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCatalogValueInvalidException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCompanyMismatchException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterIsACorrectionException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOverlapException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterPresenceCoverageGapException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterOccurrence;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterPeriod;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterCompanyLookupPort;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterRepository;
import com.b4rrhh.employee.workcenter.domain.service.WorkCenterEmployeeCompanyDomainService;
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
 * Adding a work center assignment through the temporal component (ADR-057).
 * The timeline service is real; the repository and the presence port are
 * mocked. The employee is present from 2026-01-01 onwards, and the presence
 * port doubles as the company lookup: the presence is with company COMP.
 */
@ExtendWith(MockitoExtension.class)
class CreateWorkCenterServiceTest {

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
    @Mock
    private WorkCenterCompanyLookupPort workCenterCompanyLookupPort;

    private CreateWorkCenterService service;

    @BeforeEach
    void setUp() {
        service = new CreateWorkCenterService(
                workCenterRepository,
                employeeWorkCenterLookupPort,
                ruleSystemRepository,
                new TestWorkCenterCatalogValidator(),
                new WorkCenterTimelineService(workCenterRepository, presencePort),
                new WorkCenterEmployeeCompanyDomainService(presencePort, workCenterCompanyLookupPort)
        );
    }

    @Test
    void createsTheFirstAssignmentAndAssignsNextNumber() {
        givenEmployeeWithSeries();
        givenWorkCenterOfTheEmployeeCompany("MADRID_HQ", PRESENCE_START);
        when(workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(10L)).thenReturn(Optional.of(2));
        when(workCenterRepository.save(any(WorkCenter.class))).thenAnswer(invocation -> {
            WorkCenter input = invocation.getArgument(0);
            return new WorkCenter(
                    99L,
                    input.getEmployeeId(),
                    input.getWorkCenterAssignmentNumber(),
                    input.getWorkCenterCode(),
                    input.getStartDate(),
                    input.getEndDate(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
        });

        WorkCenter created = service.create(command("madrid_hq", PRESENCE_START, null));

        assertEquals(99L, created.getId());
        assertEquals(3, created.getWorkCenterAssignmentNumber());
        assertEquals("MADRID_HQ", created.getWorkCenterCode());

        ArgumentCaptor<WorkCenter> captor = ArgumentCaptor.forClass(WorkCenter.class);
        verify(workCenterRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getWorkCenterAssignmentNumber());
    }

    @Test
    void allowsSameDayValidity() {
        givenEmployeeWithSeries();
        givenWorkCenterOfTheEmployeeCompany("MADRID_HQ", PRESENCE_START);
        when(workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(10L)).thenReturn(Optional.empty());
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new PresencePeriod(PRESENCE_START, PRESENCE_START)));
        when(workCenterRepository.save(any(WorkCenter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkCenter created = service.create(command("MADRID_HQ", PRESENCE_START, PRESENCE_START));

        assertEquals(PRESENCE_START, created.getStartDate());
        assertEquals(PRESENCE_START, created.getEndDate());
    }

    // The five cases of backend#48, first: adding after the open one closes it the day before.
    @Test
    void addingAfterTheOpenOneClosesItTheDayBeforeInsteadOfRejectingAnOverlap() {
        WorkCenter open = workCenter(1, "MADRID_HQ", PRESENCE_START, null);
        givenEmployeeWithSeries(open);
        givenWorkCenterOfTheEmployeeCompany("BARCELONA_HQ", LocalDate.of(2026, 3, 1));
        when(workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(10L)).thenReturn(Optional.of(1));
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 1)).thenReturn(Optional.of(open));
        when(workCenterRepository.save(any(WorkCenter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkCenter created = service.create(command("barcelona_hq", LocalDate.of(2026, 3, 1), null));

        assertEquals(2, created.getWorkCenterAssignmentNumber());
        assertEquals(LocalDate.of(2026, 3, 1), created.getStartDate());
        assertNull(created.getEndDate());

        ArgumentCaptor<WorkCenter> saved = ArgumentCaptor.forClass(WorkCenter.class);
        verify(workCenterRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        WorkCenter closed = saved.getAllValues().get(0);
        assertEquals(1, closed.getWorkCenterAssignmentNumber());
        assertEquals("MADRID_HQ", closed.getWorkCenterCode());
        assertEquals(PRESENCE_START, closed.getStartDate());
        assertEquals(LocalDate.of(2026, 2, 28), closed.getEndDate());
        assertEquals(2, saved.getAllValues().get(1).getWorkCenterAssignmentNumber());
    }

    @Test
    void rejectsAnOverlapWithAnAssignmentTheNewOneDoesNotStartInsideOf() {
        WorkCenter first = workCenter(1, "MADRID_HQ", PRESENCE_START, LocalDate.of(2026, 1, 31));
        WorkCenter second = workCenter(2, "MADRID_HQ", LocalDate.of(2026, 2, 1), null);
        givenEmployeeWithSeries(first, second);
        givenWorkCenterOfTheEmployeeCompany("BARCELONA_HQ", LocalDate.of(2026, 1, 15));
        when(workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(10L)).thenReturn(Optional.of(2));

        WorkCenterOverlapException ex = assertThrows(
                WorkCenterOverlapException.class,
                () -> service.create(command("BARCELONA_HQ", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 10)))
        );

        assertEquals(List.of(new WorkCenterPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10))), ex.overlaps());
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    @Test
    void rejectsInvalidCatalogValue() {
        whenEmployeeExists();

        assertThrows(
                WorkCenterCatalogValueInvalidException.class,
                () -> service.create(command("bad", PRESENCE_START, null))
        );
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    @Test
    void rejectsWhenOutsidePresenceHistory() {
        givenEmployeeWithSeries();
        givenWorkCenterOfTheEmployeeCompany("MADRID_HQ", LocalDate.of(2025, 12, 1));
        when(workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(10L)).thenReturn(Optional.empty());

        assertThrows(
                WorkCenterOutsidePresencePeriodException.class,
                () -> service.create(command("MADRID_HQ", LocalDate.of(2025, 12, 1), null))
        );
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    @Test
    void rejectsAnAddThatLeavesAGapNamingTheGapAndTheNeighbourToStretch() {
        WorkCenter closed = workCenter(1, "MADRID_HQ", PRESENCE_START, LocalDate.of(2026, 1, 31));
        givenEmployeeWithSeries(closed);
        givenWorkCenterOfTheEmployeeCompany("BARCELONA_HQ", LocalDate.of(2026, 3, 1));
        when(workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(10L)).thenReturn(Optional.of(1));

        WorkCenterPresenceCoverageGapException ex = assertThrows(
                WorkCenterPresenceCoverageGapException.class,
                () -> service.create(command("BARCELONA_HQ", LocalDate.of(2026, 3, 1), null))
        );

        assertEquals(List.of(new WorkCenterPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))), ex.gaps());
        assertEquals(
                List.of(
                        new WorkCenterOccurrence(1, PRESENCE_START, LocalDate.of(2026, 1, 31)),
                        new WorkCenterOccurrence(null, LocalDate.of(2026, 3, 1), null)
                ),
                ex.stretchCandidates()
        );
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    // What the old EXACT_START replaced silently is now a rejection that names the
    // assignment to correct (backend#52), and nothing is persisted (backend#58).
    @Test
    void rejectsAnAddOnTheStartDateOfAnExistingOneAsItsCorrectionAndPersistsNothing() {
        WorkCenter open = workCenter(1, "MADRID_HQ", PRESENCE_START, null);
        givenEmployeeWithSeries(open);
        givenWorkCenterOfTheEmployeeCompany("BARCELONA_HQ", PRESENCE_START);
        when(workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(10L)).thenReturn(Optional.of(1));

        WorkCenterIsACorrectionException ex = assertThrows(
                WorkCenterIsACorrectionException.class,
                () -> service.create(command("BARCELONA_HQ", PRESENCE_START, null))
        );

        assertEquals(new WorkCenterOccurrence(1, PRESENCE_START, null), ex.correctedOccurrence());
        assertEquals(new WorkCenterPeriod(PRESENCE_START, null), ex.requested());
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    @Test
    void rejectsWhenWorkCenterBelongsToDifferentCompany() {
        whenEmployeeExists();
        when(workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(10L)).thenReturn(Optional.empty());
        when(presencePort.findActiveCompanyCode(10L, PRESENCE_START)).thenReturn(Optional.of("COMP"));
        when(workCenterCompanyLookupPort.findCompanyCode(RULE_SYSTEM_CODE, "MADRID_HQ", PRESENCE_START))
                .thenReturn(Optional.of("OTHER"));

        assertThrows(
                WorkCenterCompanyMismatchException.class,
                () -> service.create(command("MADRID_HQ", PRESENCE_START, null))
        );
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    private CreateWorkCenterCommand command(String workCenterCode, LocalDate startDate, LocalDate endDate) {
        return new CreateWorkCenterCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
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

    private void givenWorkCenterOfTheEmployeeCompany(String workCenterCode, LocalDate referenceDate) {
        when(presencePort.findActiveCompanyCode(10L, referenceDate)).thenReturn(Optional.of("COMP"));
        when(workCenterCompanyLookupPort.findCompanyCode(RULE_SYSTEM_CODE, workCenterCode, referenceDate))
                .thenReturn(Optional.of("COMP"));
    }

    private void whenEmployeeExists() {
        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeWorkCenterLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(new EmployeeWorkCenterContext(10L, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER)));
    }

    private static WorkCenter workCenter(int number, String workCenterCode, LocalDate startDate, LocalDate endDate) {
        return new WorkCenter(
                (long) number,
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
