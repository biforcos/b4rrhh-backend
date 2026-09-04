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
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNotFoundException;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Correcting a working time moves nothing else (ADR-057, decision 3). The
 * timeline service is real and the repository and presence port are mocked:
 * the employee is present from 2024-01-01 onwards.
 */
@ExtendWith(MockitoExtension.class)
class UpdateWorkingTimeServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
    private static final String AGREEMENT_CODE = "99002405011982";
    private static final BigDecimal ANNUAL_HOURS = new BigDecimal("1736.00");
    private static final LocalDate PRESENCE_START = LocalDate.of(2024, 1, 1);

    private static final WorkingTimeDerivedHours DERIVED_HOURS = new WorkingTimeDerivedHours(
            new BigDecimal("20.00"),
            new BigDecimal("4.00"),
            new BigDecimal("86.80")
    );

    @Mock private WorkingTimeRepository workingTimeRepository;
    @Mock private EmployeeWorkingTimeLookupPort employeeWorkingTimeLookupPort;
    @Mock private EmployeeAgreementContextLookupPort employeeAgreementContextLookupPort;
    @Mock private AgreementAnnualHoursLookupPort agreementAnnualHoursLookupPort;
    @Mock private WorkingTimePresenceConsistencyPort presencePort;
    @Mock private WorkingTimeDerivationPolicy workingTimeDerivationPolicy;

    private UpdateWorkingTimeService service;

    @BeforeEach
    void setUp() {
        service = new UpdateWorkingTimeService(
                workingTimeRepository,
                employeeWorkingTimeLookupPort,
                employeeAgreementContextLookupPort,
                agreementAnnualHoursLookupPort,
                new WorkingTimeTimelineService(workingTimeRepository, presencePort),
                workingTimeDerivationPolicy
        );
    }

    @Test
    void correctsThePercentageKeepingTheDates() {
        LocalDate startDate = PRESENCE_START;
        BigDecimal newPercentage = new BigDecimal("50");
        WorkingTime existing = workingTime(1, startDate, null, new BigDecimal("40"));

        givenEmployeeWithSeries(existing);
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 1)).thenReturn(Optional.of(existing));
        stubAgreementResolution(startDate);
        when(workingTimeDerivationPolicy.derive(newPercentage, ANNUAL_HOURS)).thenReturn(DERIVED_HOURS);
        when(workingTimeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkingTime updated = service.update(command(1, startDate, null, newPercentage));

        assertThat(updated.getWorkingTimePercentage()).isEqualByComparingTo(newPercentage);
        assertThat(updated.getStartDate()).isEqualTo(startDate);
        assertThat(updated.getEndDate()).isNull();

        ArgumentCaptor<WorkingTime> captor = ArgumentCaptor.forClass(WorkingTime.class);
        verify(workingTimeRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getWorkingTimePercentage()).isEqualByComparingTo(newPercentage);
    }

    @Test
    void stretchesAClosedOneOverTheGapItLeft() {
        WorkingTime first = workingTime(1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 11, 30), new BigDecimal("40"));
        WorkingTime second = workingTime(2, LocalDate.of(2025, 1, 1), null, new BigDecimal("40"));

        givenEmployeeWithSeries(first, second);
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 1)).thenReturn(Optional.of(first));
        stubAgreementResolution(LocalDate.of(2024, 1, 1));
        when(workingTimeDerivationPolicy.derive(new BigDecimal("40"), ANNUAL_HOURS)).thenReturn(DERIVED_HOURS);
        when(workingTimeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkingTime updated = service.update(
                command(1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), new BigDecimal("40"))
        );

        assertThat(updated.getEndDate()).isEqualTo(LocalDate.of(2024, 12, 31));
        verify(workingTimeRepository, times(1)).save(any());
    }

    @Test
    void movingTheStartLaterDoesNotStretchThePredecessorButNamesIt() {
        WorkingTime predecessor = workingTime(1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), new BigDecimal("40"));
        WorkingTime current = workingTime(2, LocalDate.of(2025, 1, 1), null, new BigDecimal("40"));
        LocalDate newStart = LocalDate.of(2025, 2, 1);

        givenEmployeeWithSeries(predecessor, current);
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 2)).thenReturn(Optional.of(current));
        stubAgreementResolution(newStart);
        when(workingTimeDerivationPolicy.derive(new BigDecimal("60"), ANNUAL_HOURS)).thenReturn(DERIVED_HOURS);

        WorkingTimeCoverageGapException ex = assertThrows(
                WorkingTimeCoverageGapException.class,
                () -> service.update(command(2, newStart, null, new BigDecimal("60")))
        );

        assertEquals(
                List.of(new WorkingTimePeriod(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))),
                ex.gaps()
        );
        assertEquals(
                List.of(
                        new WorkingTimeOccurrence(1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)),
                        new WorkingTimeOccurrence(2, newStart, null)
                ),
                ex.stretchCandidates()
        );
        verify(workingTimeRepository, never()).save(any());
    }

    @Test
    void reachingIntoTheNextOneIsRejectedAsAnOverlap() {
        WorkingTime first = workingTime(1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), new BigDecimal("40"));
        WorkingTime second = workingTime(2, LocalDate.of(2025, 1, 1), null, new BigDecimal("40"));

        givenEmployeeWithSeries(first, second);
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 1)).thenReturn(Optional.of(first));
        stubAgreementResolution(LocalDate.of(2024, 1, 1));
        when(workingTimeDerivationPolicy.derive(new BigDecimal("40"), ANNUAL_HOURS)).thenReturn(DERIVED_HOURS);

        WorkingTimeOverlapException ex = assertThrows(
                WorkingTimeOverlapException.class,
                () -> service.update(command(1, LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 15), new BigDecimal("40")))
        );

        assertEquals(
                List.of(new WorkingTimePeriod(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))),
                ex.overlaps()
        );
        verify(workingTimeRepository, never()).save(any());
    }

    @Test
    void movingBeforeThePresenceIsRejected() {
        WorkingTime existing = workingTime(1, LocalDate.of(2024, 1, 1), null, new BigDecimal("40"));
        LocalDate beforePresence = LocalDate.of(2023, 12, 1);

        givenEmployeeWithSeries(existing);
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 1)).thenReturn(Optional.of(existing));
        stubAgreementResolution(beforePresence);
        when(workingTimeDerivationPolicy.derive(new BigDecimal("40"), ANNUAL_HOURS)).thenReturn(DERIVED_HOURS);

        assertThrows(
                WorkingTimeOutsidePresencePeriodException.class,
                () -> service.update(command(1, beforePresence, null, new BigDecimal("40")))
        );
        verify(workingTimeRepository, never()).save(any());
    }

    @Test
    void rejectsWhenTheWorkingTimeDoesNotExist() {
        whenEmployeeExists();
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 7)).thenReturn(Optional.empty());

        assertThrows(
                WorkingTimeNotFoundException.class,
                () -> service.update(command(7, LocalDate.of(2026, 1, 1), null, new BigDecimal("50")))
        );
    }

    private UpdateWorkingTimeCommand command(int number, LocalDate startDate, LocalDate endDate, BigDecimal percentage) {
        return new UpdateWorkingTimeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER, number, startDate, endDate, percentage
        );
    }

    private void givenEmployeeWithSeries(WorkingTime... occurrences) {
        whenEmployeeExists();
        when(workingTimeRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new DateRange(PRESENCE_START, null)));
    }

    private void whenEmployeeExists() {
        when(employeeWorkingTimeLookupPort.findByBusinessKeyForUpdate(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER
        )).thenReturn(Optional.of(new EmployeeWorkingTimeContext(
                10L, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER
        )));
    }

    private void stubAgreementResolution(LocalDate startDate) {
        when(employeeAgreementContextLookupPort.resolveContext(10L, startDate))
                .thenReturn(new EmployeeAgreementContext(RULE_SYSTEM_CODE, AGREEMENT_CODE));
        when(agreementAnnualHoursLookupPort.resolveAnnualHours(RULE_SYSTEM_CODE, AGREEMENT_CODE))
                .thenReturn(ANNUAL_HOURS);
    }

    private static WorkingTime workingTime(int number, LocalDate startDate, LocalDate endDate, BigDecimal percentage) {
        return WorkingTime.rehydrate(
                (long) number,
                10L,
                number,
                startDate,
                endDate,
                percentage,
                DERIVED_HOURS,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
