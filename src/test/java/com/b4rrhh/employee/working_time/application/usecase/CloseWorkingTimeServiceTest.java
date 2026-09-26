package com.b4rrhh.employee.working_time.application.usecase;

import com.b4rrhh.employee.shared.application.port.DatedWriteNoticePort;
import com.b4rrhh.employee.working_time.application.port.EmployeeWorkingTimeContext;
import com.b4rrhh.employee.working_time.application.port.EmployeeWorkingTimeLookupPort;
import com.b4rrhh.employee.working_time.application.service.StandardWorkingTimeDerivationPolicy;
import com.b4rrhh.employee.working_time.application.service.WorkingTimePresenceConsistencyValidator;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeAlreadyClosedException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNotFoundException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOutsidePresencePeriodException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeDerivedHours;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloseWorkingTimeServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
        private static final StandardWorkingTimeDerivationPolicy DERIVATION_POLICY = new StandardWorkingTimeDerivationPolicy();

    @Mock
    private WorkingTimeRepository workingTimeRepository;
    @Mock
    private EmployeeWorkingTimeLookupPort employeeWorkingTimeLookupPort;
    @Mock
    private WorkingTimePresenceConsistencyValidator workingTimePresenceConsistencyValidator;
    /** Doble sin comportamiento: lo vigila el candado, no este test ({@code backend#130}). */
    @Mock private DatedWriteNoticePort datedWrites;

    private CloseWorkingTimeService service;

    @BeforeEach
    void setUp() {
        service = new CloseWorkingTimeService(
                workingTimeRepository,
                employeeWorkingTimeLookupPort,
                workingTimePresenceConsistencyValidator,
                datedWrites
        );
    }

    @Test
    void closesWorkingTime() {
        WorkingTime existing = activeWorkingTime(1, LocalDate.of(2026, 1, 10));

        when(employeeWorkingTimeLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 1)).thenReturn(Optional.of(existing));
        when(workingTimeRepository.save(any(WorkingTime.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkingTime closed = service.close(new CloseWorkingTimeCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                1,
                LocalDate.of(2026, 1, 20)
        ));

        assertEquals(LocalDate.of(2026, 1, 20), closed.getEndDate());

        ArgumentCaptor<WorkingTime> captor = ArgumentCaptor.forClass(WorkingTime.class);
        verify(workingTimeRepository).save(captor.capture());
        assertEquals(LocalDate.of(2026, 1, 20), captor.getValue().getEndDate());
    }

    @Test
    void rejectsCloseWhenWorkingTimeDoesNotExist() {
        when(employeeWorkingTimeLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 1)).thenReturn(Optional.empty());

        assertThrows(
                WorkingTimeNotFoundException.class,
                () -> service.close(new CloseWorkingTimeCommand(
                        RULE_SYSTEM_CODE,
                        EMPLOYEE_TYPE_CODE,
                        EMPLOYEE_NUMBER,
                        1,
                        LocalDate.of(2026, 1, 20)
                ))
        );
    }

    @Test
    void rejectsCloseWhenAlreadyClosed() {
        WorkingTime existing = workingTime(1, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 15), new BigDecimal("50"));

        when(employeeWorkingTimeLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 1)).thenReturn(Optional.of(existing));

        assertThrows(
                WorkingTimeAlreadyClosedException.class,
                () -> service.close(new CloseWorkingTimeCommand(
                        RULE_SYSTEM_CODE,
                        EMPLOYEE_TYPE_CODE,
                        EMPLOYEE_NUMBER,
                        1,
                        LocalDate.of(2026, 1, 20)
                ))
        );
        verify(workingTimeRepository, never()).save(any(WorkingTime.class));
    }

    @Test
    void rejectsCloseWhenResultingPeriodIsOutsidePresenceHistory() {
        WorkingTime existing = activeWorkingTime(1, LocalDate.of(2026, 1, 10));

        when(employeeWorkingTimeLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(10L, 1)).thenReturn(Optional.of(existing));

        doThrow(new WorkingTimeOutsidePresencePeriodException(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 20)
        )).when(workingTimePresenceConsistencyValidator).validatePeriodWithinPresence(
                10L,
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 20),
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER
        );

        assertThrows(
                WorkingTimeOutsidePresencePeriodException.class,
                () -> service.close(new CloseWorkingTimeCommand(
                        RULE_SYSTEM_CODE,
                        EMPLOYEE_TYPE_CODE,
                        EMPLOYEE_NUMBER,
                        1,
                        LocalDate.of(2026, 1, 20)
                ))
        );
    }

    private WorkingTime activeWorkingTime(int number, LocalDate startDate) {
                return workingTime(number, startDate, null, new BigDecimal("50"));
    }

        private WorkingTime workingTime(int number, LocalDate startDate, LocalDate endDate, BigDecimal percentage) {
                WorkingTimeDerivedHours derivedHours = DERIVATION_POLICY.derive(percentage, new java.math.BigDecimal("1736"));

        return WorkingTime.rehydrate(
                (long) number,
                10L,
                number,
                startDate,
                endDate,
                percentage,
                derivedHours,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private EmployeeWorkingTimeContext employeeContext(Long employeeId) {
        return new EmployeeWorkingTimeContext(employeeId, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER);
    }
}