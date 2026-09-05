package com.b4rrhh.employee.working_time.application.usecase;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.working_time.application.port.AgreementAnnualHoursLookupPort;
import com.b4rrhh.employee.working_time.application.port.EmployeeAgreementContext;
import com.b4rrhh.employee.working_time.application.port.EmployeeAgreementContextLookupPort;
import com.b4rrhh.employee.working_time.application.port.EmployeeWorkingTimeContext;
import com.b4rrhh.employee.working_time.application.port.EmployeeWorkingTimeLookupPort;
import com.b4rrhh.employee.working_time.application.port.WorkingTimePresenceConsistencyPort;
import com.b4rrhh.employee.working_time.application.service.WorkingTimeTimelineService;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeCoverageGapException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeIsACorrectionException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNumberConflictException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOutsidePresencePeriodException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOverlapException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeDerivedHours;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeOccurrence;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimePeriod;
import com.b4rrhh.employee.working_time.domain.port.WorkingTimeRepository;
import com.b4rrhh.employee.working_time.domain.service.WorkingTimeDerivationPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adding a working time is planned against the invariants of the series
 * (ADR-057). The timeline service is real and the repository and presence
 * port are mocked: the employee is present from 2026-01-01 onwards.
 */
@ExtendWith(MockitoExtension.class)
class CreateWorkingTimeServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
    private static final String AGREEMENT_CODE = "99002405011982";
    private static final BigDecimal ANNUAL_HOURS = new BigDecimal("1736.00");
    private static final LocalDate PRESENCE_START = LocalDate.of(2026, 1, 1);
    private static final WorkingTimeDerivedHours DERIVED_HOURS = new WorkingTimeDerivedHours(
            new BigDecimal("16.69"), new BigDecimal("3.34"), new BigDecimal("72.33"));

    @Mock
    private WorkingTimeRepository workingTimeRepository;
    @Mock
    private EmployeeWorkingTimeLookupPort employeeWorkingTimeLookupPort;
    @Mock
    private EmployeeAgreementContextLookupPort employeeAgreementContextLookupPort;
    @Mock
    private AgreementAnnualHoursLookupPort agreementAnnualHoursLookupPort;
    @Mock
    private WorkingTimePresenceConsistencyPort presencePort;
    @Mock
    private WorkingTimeDerivationPolicy workingTimeDerivationPolicy;

    private CreateWorkingTimeService service;

    @BeforeEach
    void setUp() {
        service = new CreateWorkingTimeService(
                workingTimeRepository,
                employeeWorkingTimeLookupPort,
                employeeAgreementContextLookupPort,
                agreementAnnualHoursLookupPort,
                new WorkingTimeTimelineService(workingTimeRepository, presencePort),
                workingTimeDerivationPolicy
        );
    }

    @Test
    void createsTheFirstWorkingTimeAndCalculatesDerivedHours() {
        givenEmployeeWithSeries();
        stubAgreementResolution(PRESENCE_START);
        stubDerivedHours(new BigDecimal("50"));
        when(workingTimeRepository.findMaxWorkingTimeNumberByEmployeeId(10L)).thenReturn(Optional.empty());
        when(workingTimeRepository.save(any(WorkingTime.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        WorkingTime created = service.create(command(PRESENCE_START, null, new BigDecimal("50")));

        assertEquals(99L, created.getId());
        assertEquals(1, created.getWorkingTimeNumber());
        assertNull(created.getEndDate());
        assertEquals(0, created.getWorkingTimePercentage().compareTo(new BigDecimal("50")));
        assertEquals(new BigDecimal("16.69"), created.getWeeklyHours());
        assertEquals(new BigDecimal("3.34"), created.getDailyHours());
        assertEquals(new BigDecimal("72.33"), created.getMonthlyHours());
        verify(workingTimeDerivationPolicy, atLeastOnce())
                .derive(
                        argThat(pct -> pct != null && pct.compareTo(new BigDecimal("50")) == 0),
                        argThat(hrs -> hrs != null && hrs.compareTo(ANNUAL_HOURS) == 0)
                );
    }

    @Test
    void addingFromTheSixteenthClosesTheOpenOneOnTheFifteenthInsteadOfRejectingIt() {
        WorkingTime open = workingTime(1, PRESENCE_START, null);
        LocalDate newStart = LocalDate.of(2026, 1, 16);
        givenEmployeeWithSeries(open);
        stubAgreementResolution(newStart);
        stubDerivedHours(new BigDecimal("50"));
        when(workingTimeRepository.findMaxWorkingTimeNumberByEmployeeId(10L)).thenReturn(Optional.of(1));
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 1)).thenReturn(Optional.of(open));
        when(workingTimeRepository.save(any(WorkingTime.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        WorkingTime created = service.create(command(newStart, null, new BigDecimal("50")));

        assertEquals(2, created.getWorkingTimeNumber());
        assertEquals(newStart, created.getStartDate());

        ArgumentCaptor<WorkingTime> captor = ArgumentCaptor.forClass(WorkingTime.class);
        verify(workingTimeRepository, times(2)).save(captor.capture());
        WorkingTime closedFirst = captor.getAllValues().get(0);
        assertEquals(1, closedFirst.getWorkingTimeNumber());
        assertEquals(PRESENCE_START, closedFirst.getStartDate());
        assertEquals(LocalDate.of(2026, 1, 15), closedFirst.getEndDate());
        assertEquals(2, captor.getAllValues().get(1).getWorkingTimeNumber());
    }

    @Test
    void acceptsAnEndDateFromTheRequest() {
        when(employeeWorkingTimeLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(new EmployeeWorkingTimeContext(10L, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER)));
        when(workingTimeRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of());
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new DateRange(PRESENCE_START, LocalDate.of(2026, 6, 30))));
        stubAgreementResolution(PRESENCE_START);
        stubDerivedHours(new BigDecimal("50"));
        when(workingTimeRepository.findMaxWorkingTimeNumberByEmployeeId(10L)).thenReturn(Optional.empty());
        when(workingTimeRepository.save(any(WorkingTime.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        WorkingTime created = service.create(command(PRESENCE_START, LocalDate.of(2026, 6, 30), new BigDecimal("50")));

        assertEquals(LocalDate.of(2026, 6, 30), created.getEndDate());
    }

    @Test
    void rejectsAWorkingTimeThatLeavesAGapSayingWhichGapAndWhatToStretch() {
        WorkingTime closed = workingTime(1, PRESENCE_START, LocalDate.of(2026, 1, 31));
        LocalDate newStart = LocalDate.of(2026, 3, 1);
        givenEmployeeWithSeries(closed);
        stubAgreementResolution(newStart);
        stubDerivedHours(new BigDecimal("80"));
        when(workingTimeRepository.findMaxWorkingTimeNumberByEmployeeId(10L)).thenReturn(Optional.of(1));

        WorkingTimeCoverageGapException ex = assertThrows(
                WorkingTimeCoverageGapException.class,
                () -> service.create(command(newStart, null, new BigDecimal("80")))
        );

        assertEquals(
                List.of(new WorkingTimePeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))),
                ex.gaps()
        );
        assertEquals(
                List.of(
                        new WorkingTimeOccurrence(1, PRESENCE_START, LocalDate.of(2026, 1, 31)),
                        new WorkingTimeOccurrence(null, newStart, null)
                ),
                ex.stretchCandidates()
        );
        verify(workingTimeRepository, never()).save(any(WorkingTime.class));
    }

    @Test
    void rejectsAWorkingTimeThatReachesIntoTheNextOneAsAnOverlap() {
        WorkingTime first = workingTime(1, PRESENCE_START, LocalDate.of(2026, 1, 31));
        WorkingTime second = workingTime(2, LocalDate.of(2026, 2, 1), null);
        LocalDate newStart = LocalDate.of(2026, 1, 16);
        givenEmployeeWithSeries(first, second);
        stubAgreementResolution(newStart);
        stubDerivedHours(new BigDecimal("80"));
        when(workingTimeRepository.findMaxWorkingTimeNumberByEmployeeId(10L)).thenReturn(Optional.of(2));

        WorkingTimeOverlapException ex = assertThrows(
                WorkingTimeOverlapException.class,
                () -> service.create(command(newStart, LocalDate.of(2026, 2, 10), new BigDecimal("80")))
        );

        assertEquals(
                List.of(new WorkingTimePeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10))),
                ex.overlaps()
        );
        verify(workingTimeRepository, never()).save(any(WorkingTime.class));
    }

    @Test
    void rejectsAWorkingTimeStartingOnTheSameDayAsAnExistingOneAsACorrectionNotAnAdd() {
        WorkingTime first = workingTime(1, PRESENCE_START, LocalDate.of(2026, 1, 31));
        WorkingTime second = workingTime(2, LocalDate.of(2026, 2, 1), null);
        LocalDate sameStart = LocalDate.of(2026, 2, 1);
        givenEmployeeWithSeries(first, second);
        stubAgreementResolution(sameStart);
        stubDerivedHours(new BigDecimal("80"));
        when(workingTimeRepository.findMaxWorkingTimeNumberByEmployeeId(10L)).thenReturn(Optional.of(2));

        WorkingTimeIsACorrectionException ex = assertThrows(
                WorkingTimeIsACorrectionException.class,
                () -> service.create(command(sameStart, null, new BigDecimal("80")))
        );

        assertEquals(new WorkingTimeOccurrence(2, LocalDate.of(2026, 2, 1), null), ex.correctedOccurrence());
        verify(workingTimeRepository, never()).save(any(WorkingTime.class));
    }

    @Test
    void rejectsWhenOutsidePresenceHistory() {
        LocalDate beforePresence = LocalDate.of(2025, 12, 1);
        givenEmployeeWithSeries();
        stubAgreementResolution(beforePresence);
        stubDerivedHours(new BigDecimal("80"));
        when(workingTimeRepository.findMaxWorkingTimeNumberByEmployeeId(10L)).thenReturn(Optional.empty());

        assertThrows(
                WorkingTimeOutsidePresencePeriodException.class,
                () -> service.create(command(beforePresence, null, new BigDecimal("80")))
        );
        verify(workingTimeRepository, never()).save(any(WorkingTime.class));
    }

    @Test
    void translatesFunctionalNumberUniquenessConflict() {
        givenEmployeeWithSeries();
        stubAgreementResolution(PRESENCE_START);
        stubDerivedHours(new BigDecimal("80"));
        when(workingTimeRepository.findMaxWorkingTimeNumberByEmployeeId(10L)).thenReturn(Optional.of(1));
        when(workingTimeRepository.save(any(WorkingTime.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate working_time_number"));

        assertThrows(
                WorkingTimeNumberConflictException.class,
                () -> service.create(command(PRESENCE_START, null, new BigDecimal("80")))
        );
    }

    private CreateWorkingTimeCommand command(LocalDate startDate, LocalDate endDate, BigDecimal percentage) {
        return new CreateWorkingTimeCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                startDate,
                endDate,
                percentage
        );
    }

    private void givenEmployeeWithSeries(WorkingTime... occurrences) {
        when(employeeWorkingTimeLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(new EmployeeWorkingTimeContext(10L, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER)));
        when(workingTimeRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new DateRange(PRESENCE_START, null)));
    }

    private void stubAgreementResolution(LocalDate effectiveDate) {
        when(employeeAgreementContextLookupPort.resolveContext(10L, effectiveDate))
                .thenReturn(new EmployeeAgreementContext(RULE_SYSTEM_CODE, AGREEMENT_CODE));
        when(agreementAnnualHoursLookupPort.resolveAnnualHours(RULE_SYSTEM_CODE, AGREEMENT_CODE))
                .thenReturn(ANNUAL_HOURS);
    }

    private void stubDerivedHours(BigDecimal percentage) {
        when(workingTimeDerivationPolicy.derive(
                argThat(pct -> pct != null && pct.compareTo(percentage) == 0),
                argThat(hrs -> hrs != null && hrs.compareTo(ANNUAL_HOURS) == 0)
        )).thenReturn(DERIVED_HOURS);
    }

    private static WorkingTime persisted(WorkingTime input) {
        return WorkingTime.rehydrate(
                input.getId() == null ? 99L : input.getId(),
                input.getEmployeeId(),
                input.getWorkingTimeNumber(),
                input.getStartDate(),
                input.getEndDate(),
                input.getWorkingTimePercentage(),
                new WorkingTimeDerivedHours(input.getWeeklyHours(), input.getDailyHours(), input.getMonthlyHours()),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
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
