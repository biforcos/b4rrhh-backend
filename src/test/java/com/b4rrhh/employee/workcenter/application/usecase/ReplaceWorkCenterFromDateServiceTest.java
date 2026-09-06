package com.b4rrhh.employee.workcenter.application.usecase;

import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterContext;
import com.b4rrhh.employee.workcenter.application.port.EmployeeWorkCenterLookupPort;
import com.b4rrhh.employee.workcenter.application.port.PresencePeriod;
import com.b4rrhh.employee.workcenter.application.port.WorkCenterPresenceConsistencyPort;
import com.b4rrhh.employee.workcenter.application.service.WorkCenterCatalogValidator;
import com.b4rrhh.employee.workcenter.application.service.WorkCenterTimelineService;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCatalogValueInvalidException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCompanyMismatchException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterEmployeeNotFoundException;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The deprecated {@code replace-from-date} as an adapter over the temporal
 * component (ADR-057). The behaviour the old planner gave the loader is kept
 * where the ADR keeps it: SPLIT inside an open or a closed assignment,
 * insertion when nothing covers. EXACT_START is the one that changes: it is
 * no longer a silent replacement but a rejected plan that names the
 * assignment to correct (backend#52). The employee is present from
 * 2026-01-01 onwards with company COMP.
 */
@ExtendWith(MockitoExtension.class)
class ReplaceWorkCenterFromDateServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
    private static final LocalDate PRESENCE_START = LocalDate.of(2026, 1, 1);
    private static final LocalDate MARCH_1 = LocalDate.of(2026, 3, 1);

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

    private ReplaceWorkCenterFromDateService service;

    @BeforeEach
    void setUp() {
        service = new ReplaceWorkCenterFromDateService(
                workCenterRepository,
                employeeWorkCenterLookupPort,
                ruleSystemRepository,
                new TestWorkCenterCatalogValidator(),
                new WorkCenterTimelineService(workCenterRepository, presencePort),
                new WorkCenterEmployeeCompanyDomainService(presencePort, workCenterCompanyLookupPort)
        );
    }

    // The old SPLIT inside the open assignment: closed the day before, the new one open.
    @Test
    void replaceWhenActiveAssignmentExistsClosesCurrentAndCreatesNewOne() {
        WorkCenter existing = workCenter(41L, 2, "MADRID_HQ", PRESENCE_START, null);
        givenEmployeeWithSeries(existing);
        givenWorkCenterOfTheEmployeeCompany("BARCELONA_HQ", MARCH_1);
        when(workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(10L)).thenReturn(Optional.of(2));
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 2)).thenReturn(Optional.of(existing));
        when(workCenterRepository.save(any(WorkCenter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkCenter replaced = service.replaceFromDate(command(MARCH_1, "barcelona_hq"));

        assertEquals(3, replaced.getWorkCenterAssignmentNumber());
        assertEquals("BARCELONA_HQ", replaced.getWorkCenterCode());
        assertEquals(MARCH_1, replaced.getStartDate());
        assertNull(replaced.getEndDate());

        ArgumentCaptor<WorkCenter> saveCaptor = ArgumentCaptor.forClass(WorkCenter.class);
        verify(workCenterRepository, times(2)).save(saveCaptor.capture());

        List<WorkCenter> savedValues = saveCaptor.getAllValues();
        assertEquals(2, savedValues.get(0).getWorkCenterAssignmentNumber());
        assertEquals("MADRID_HQ", savedValues.get(0).getWorkCenterCode());
        assertEquals(LocalDate.of(2026, 2, 28), savedValues.get(0).getEndDate());
        assertEquals(3, savedValues.get(1).getWorkCenterAssignmentNumber());
        assertEquals(MARCH_1, savedValues.get(1).getStartDate());
    }

    // The old SPLIT inside a closed assignment: the replacement takes its tail.
    @Test
    void replaceInsideClosedPeriodPreservesOriginalEndDate() {
        WorkCenter closed = workCenter(41L, 1, "MADRID_HQ", PRESENCE_START, LocalDate.of(2026, 3, 31));
        WorkCenter next = workCenter(42L, 2, "MADRID_HQ", LocalDate.of(2026, 4, 1), null);
        givenEmployeeWithSeries(closed, next);
        givenWorkCenterOfTheEmployeeCompany("BARCELONA_HQ", MARCH_1);
        when(workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(10L)).thenReturn(Optional.of(2));
        when(workCenterRepository.findByEmployeeIdAndWorkCenterAssignmentNumber(10L, 1)).thenReturn(Optional.of(closed));
        when(workCenterRepository.save(any(WorkCenter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkCenter replaced = service.replaceFromDate(command(MARCH_1, "BARCELONA_HQ"));

        assertEquals(MARCH_1, replaced.getStartDate());
        assertEquals(LocalDate.of(2026, 3, 31), replaced.getEndDate());

        ArgumentCaptor<WorkCenter> saveCaptor = ArgumentCaptor.forClass(WorkCenter.class);
        verify(workCenterRepository, times(2)).save(saveCaptor.capture());
        assertEquals(1, saveCaptor.getAllValues().get(0).getWorkCenterAssignmentNumber());
        assertEquals(LocalDate.of(2026, 2, 28), saveCaptor.getAllValues().get(0).getEndDate());
    }

    // The old EXACT_START replaced the covering assignment silently. Now it is a rejected
    // plan that names it (backend#52), and nothing is written (backend#58).
    @Test
    void replaceAtExactStartDateIsRejectedAsACorrectionAndWritesNothing() {
        WorkCenter existing = workCenter(41L, 2, "MADRID_HQ", MARCH_1, null);
        givenEmployeeWithSeries(workCenter(40L, 1, "MADRID_HQ", PRESENCE_START, LocalDate.of(2026, 2, 28)), existing);
        givenWorkCenterOfTheEmployeeCompany("BARCELONA_HQ", MARCH_1);
        when(workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(10L)).thenReturn(Optional.of(2));

        WorkCenterIsACorrectionException ex = assertThrows(
                WorkCenterIsACorrectionException.class,
                () -> service.replaceFromDate(command(MARCH_1, "BARCELONA_HQ"))
        );

        assertEquals(new WorkCenterOccurrence(2, MARCH_1, null), ex.correctedOccurrence());
        assertEquals(new WorkCenterPeriod(MARCH_1, null), ex.requested());
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    // The old NO_COVERING: nothing in force on the effective date, the new one is inserted open.
    @Test
    void replaceWhenNoActiveAssignmentCreatesNewOneDirectlyIfValid() {
        givenEmployeeWithSeries(new PresencePeriod(MARCH_1, null));
        givenWorkCenterOfTheEmployeeCompany("SEVILLA_HQ", MARCH_1);
        when(workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(10L)).thenReturn(Optional.empty());
        when(workCenterRepository.save(any(WorkCenter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkCenter replaced = service.replaceFromDate(command(MARCH_1, "sevilla_hq"));

        assertEquals(1, replaced.getWorkCenterAssignmentNumber());
        assertEquals("SEVILLA_HQ", replaced.getWorkCenterCode());
        assertEquals(MARCH_1, replaced.getStartDate());
        assertNull(replaced.getEndDate());

        ArgumentCaptor<WorkCenter> saveCaptor = ArgumentCaptor.forClass(WorkCenter.class);
        verify(workCenterRepository).save(saveCaptor.capture());
        assertEquals(MARCH_1, saveCaptor.getValue().getStartDate());
        assertNull(saveCaptor.getValue().getEndDate());
    }

    @Test
    void rejectsWhenEmployeeDoesNotExist() {
        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeWorkCenterLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.empty());

        assertThrows(
                WorkCenterEmployeeNotFoundException.class,
                () -> service.replaceFromDate(command(MARCH_1, "BARCELONA_HQ"))
        );
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    @Test
    void rejectsWhenWorkCenterCodeIsInvalid() {
        whenEmployeeExists();

        assertThrows(
                WorkCenterCatalogValueInvalidException.class,
                () -> service.replaceFromDate(command(MARCH_1, "bad"))
        );
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    // Nothing covers March 1 and the new open assignment would run into the one from April.
    @Test
    void rejectsWhenProjectedTimelineOverlapsFutureAssignment() {
        WorkCenter futureOpen = workCenter(52L, 2, "BILBAO_HQ", LocalDate.of(2026, 4, 1), null);
        givenEmployeeWithSeries(new PresencePeriod(MARCH_1, null), futureOpen);
        givenWorkCenterOfTheEmployeeCompany("BARCELONA_HQ", MARCH_1);
        when(workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(10L)).thenReturn(Optional.of(2));

        WorkCenterOverlapException ex = assertThrows(
                WorkCenterOverlapException.class,
                () -> service.replaceFromDate(command(MARCH_1, "BARCELONA_HQ"))
        );

        assertEquals(List.of(new WorkCenterPeriod(LocalDate.of(2026, 4, 1), null)), ex.overlaps());
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    @Test
    void rejectsWhenReplacementFallsOutsidePresence() {
        WorkCenter existing = workCenter(41L, 2, "MADRID_HQ", PRESENCE_START, null);
        givenEmployeeWithSeries(new PresencePeriod(PRESENCE_START, LocalDate.of(2026, 2, 15)), existing);
        givenWorkCenterOfTheEmployeeCompany("BARCELONA_HQ", MARCH_1);
        when(workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(10L)).thenReturn(Optional.of(2));

        assertThrows(
                WorkCenterOutsidePresencePeriodException.class,
                () -> service.replaceFromDate(command(MARCH_1, "BARCELONA_HQ"))
        );
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    // Nothing covers March 1 because the only assignment closed in January: inserting the
    // new one leaves February uncovered, and the gap is named with its neighbours.
    @Test
    void rejectsWhenReplacementBreaksPresenceCoverage() {
        WorkCenter closedEarly = workCenter(41L, 2, "MADRID_HQ", PRESENCE_START, LocalDate.of(2026, 1, 31));
        givenEmployeeWithSeries(closedEarly);
        givenWorkCenterOfTheEmployeeCompany("BARCELONA_HQ", MARCH_1);
        when(workCenterRepository.findMaxWorkCenterAssignmentNumberByEmployeeId(10L)).thenReturn(Optional.of(2));

        WorkCenterPresenceCoverageGapException ex = assertThrows(
                WorkCenterPresenceCoverageGapException.class,
                () -> service.replaceFromDate(command(MARCH_1, "BARCELONA_HQ"))
        );

        assertEquals(List.of(new WorkCenterPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))), ex.gaps());
        assertEquals(
                List.of(
                        new WorkCenterOccurrence(2, PRESENCE_START, LocalDate.of(2026, 1, 31)),
                        new WorkCenterOccurrence(null, MARCH_1, null)
                ),
                ex.stretchCandidates()
        );
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    @Test
    void rejectsWhenReplacementWorkCenterBelongsToDifferentCompany() {
        whenEmployeeExists();
        when(presencePort.findActiveCompanyCode(10L, MARCH_1)).thenReturn(Optional.of("COMP"));
        when(workCenterCompanyLookupPort.findCompanyCode(RULE_SYSTEM_CODE, "BARCELONA_HQ", MARCH_1))
                .thenReturn(Optional.of("OTHER"));

        assertThrows(
                WorkCenterCompanyMismatchException.class,
                () -> service.replaceFromDate(command(MARCH_1, "BARCELONA_HQ"))
        );
        verify(workCenterRepository, never()).save(any(WorkCenter.class));
    }

    private void givenEmployeeWithSeries(WorkCenter... occurrences) {
        givenEmployeeWithSeries(new PresencePeriod(PRESENCE_START, null), occurrences);
    }

    private void givenEmployeeWithSeries(PresencePeriod presence, WorkCenter... occurrences) {
        whenEmployeeExists();
        when(workCenterRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(presence));
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

    private ReplaceWorkCenterFromDateCommand command(LocalDate effectiveDate, String workCenterCode) {
        return new ReplaceWorkCenterFromDateCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                effectiveDate,
                workCenterCode
        );
    }

    private static WorkCenter workCenter(
            Long id,
            int assignmentNumber,
            String workCenterCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new WorkCenter(
                id,
                10L,
                assignmentNumber,
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
        public String normalizeRequiredCode(String fieldName, String value) {
            if (value == null || value.trim().isEmpty()) {
                throw new WorkCenterCatalogValueInvalidException(fieldName, String.valueOf(value));
            }

            return value.trim().toUpperCase();
        }

        @Override
        public void validateWorkCenterCode(String ruleSystemCode, String workCenterCode, LocalDate referenceDate) {
            if ("BAD".equals(workCenterCode)) {
                throw new WorkCenterCatalogValueInvalidException("workCenterCode", workCenterCode);
            }
        }
    }
}
