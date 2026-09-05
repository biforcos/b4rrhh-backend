package com.b4rrhh.employee.labor_classification.application.usecase;

import com.b4rrhh.employee.labor_classification.application.command.CloseLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationContext;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationLookupPort;
import com.b4rrhh.employee.labor_classification.application.port.LaborClassificationPresenceConsistencyPort;
import com.b4rrhh.employee.labor_classification.application.port.PresencePeriod;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationTimelineService;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAlreadyClosedException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationCoverageIncompleteException;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassificationPeriod;
import com.b4rrhh.employee.labor_classification.domain.port.LaborClassificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The deprecated {@code close} as an adapter over the temporal component
 * (ADR-057): closing is a correction of the end date and the resulting
 * series decides. The timeline service is real; the repository and the
 * presence port are mocked.
 */
@ExtendWith(MockitoExtension.class)
class CloseLaborClassificationServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";

    @Mock
    private LaborClassificationRepository laborClassificationRepository;
    @Mock
    private EmployeeLaborClassificationLookupPort employeeLaborClassificationLookupPort;
    @Mock
    private LaborClassificationPresenceConsistencyPort presencePort;

    private CloseLaborClassificationService service;

    @BeforeEach
    void setUp() {
        service = new CloseLaborClassificationService(
                laborClassificationRepository,
                employeeLaborClassificationLookupPort,
                new LaborClassificationTimelineService(laborClassificationRepository, presencePort)
        );
    }

    // The termination flow closes the presence first (order 5), so closing the
    // labor classification on the same day leaves nothing uncovered.
    @Test
    void closesWhenValid() {
        CloseLaborClassificationCommand command = new CloseLaborClassificationCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        LaborClassification existing = new LaborClassification(
                10L,
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 1, 1),
                null
        );

        givenEmployeePresent(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), existing);
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.of(existing));

        LaborClassification closed = service.close(command);

        assertEquals(LocalDate.of(2026, 1, 31), closed.getEndDate());

        ArgumentCaptor<LaborClassification> captor = ArgumentCaptor.forClass(LaborClassification.class);
        verify(laborClassificationRepository).update(captor.capture(), any(LocalDate.class));
        assertEquals(LocalDate.of(2026, 1, 31), captor.getValue().getEndDate());
    }

    @Test
    void rejectsCloseWhenAlreadyClosed() {
        CloseLaborClassificationCommand command = new CloseLaborClassificationCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1)
        );

        LaborClassification closed = new LaborClassification(
                10L,
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        whenEmployeeExists();
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.of(closed));

        assertThrows(LaborClassificationAlreadyClosedException.class, () -> service.close(command));
        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
    }

    @Test
    void rejectsCloseWhenCoverageWouldHaveGap() {
        CloseLaborClassificationCommand command = new CloseLaborClassificationCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 15)
        );

        LaborClassification existing = new LaborClassification(
                10L,
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 1, 1),
                null
        );

        givenEmployeePresent(LocalDate.of(2026, 1, 1), null, existing);
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.of(existing));

        LaborClassificationCoverageIncompleteException ex = assertThrows(
                LaborClassificationCoverageIncompleteException.class,
                () -> service.close(command)
        );

        assertEquals(List.of(new LaborClassificationPeriod(LocalDate.of(2026, 1, 16), null)), ex.gaps());
        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
    }

    private void givenEmployeePresent(LocalDate presenceStart, LocalDate presenceEnd, LaborClassification... occurrences) {
        whenEmployeeExists();
        when(laborClassificationRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new PresencePeriod(presenceStart, presenceEnd)));
    }

    private void whenEmployeeExists() {
        when(employeeLaborClassificationLookupPort.findByBusinessKeyForUpdate(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER
        )).thenReturn(Optional.of(new EmployeeLaborClassificationContext(
                10L,
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER
        )));
    }
}
