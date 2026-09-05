package com.b4rrhh.employee.labor_classification.application.usecase;

import com.b4rrhh.employee.labor_classification.application.command.DeleteLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationContext;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationLookupPort;
import com.b4rrhh.employee.labor_classification.application.port.LaborClassificationPresenceConsistencyPort;
import com.b4rrhh.employee.labor_classification.application.port.PresencePeriod;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationTimelineService;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationCoverageIncompleteException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Removing a labor classification (ADR-057, decision 3). The timeline
 * service is real and the repository and presence port are mocked: the
 * employee is present from 2026-01-01 onwards.
 */
@ExtendWith(MockitoExtension.class)
class DeleteLaborClassificationServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
    private static final LocalDate PRESENCE_START = LocalDate.of(2026, 1, 1);

    @Mock
    private LaborClassificationRepository laborClassificationRepository;
    @Mock
    private EmployeeLaborClassificationLookupPort employeeLaborClassificationLookupPort;
    @Mock
    private LaborClassificationPresenceConsistencyPort presencePort;

    private DeleteLaborClassificationService service;

    @BeforeEach
    void setUp() {
        service = new DeleteLaborClassificationService(
                laborClassificationRepository,
                employeeLaborClassificationLookupPort,
                new LaborClassificationTimelineService(laborClassificationRepository, presencePort)
        );
    }

    @Test
    void deletingTheLastOneReopensThePreviousOne() {
        LaborClassification first = occurrence(PRESENCE_START, LocalDate.of(2026, 1, 15));
        LaborClassification last = occurrence(LocalDate.of(2026, 1, 16), null);
        givenEmployeeWithSeries(first, last);
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, last.getStartDate())).thenReturn(Optional.of(last));
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, first.getStartDate())).thenReturn(Optional.of(first));

        service.delete(command(LocalDate.of(2026, 1, 16)));

        ArgumentCaptor<LaborClassification> reopened = ArgumentCaptor.forClass(LaborClassification.class);
        verify(laborClassificationRepository).update(reopened.capture(), eq(PRESENCE_START));
        assertEquals(PRESENCE_START, reopened.getValue().getStartDate());
        assertNull(reopened.getValue().getEndDate());
        verify(laborClassificationRepository).delete(last);
    }

    @Test
    void deletingTheOnlyOneIsRejectedBecauseThePresenceWouldBeUncovered() {
        LaborClassification only = occurrence(PRESENCE_START, null);
        givenEmployeeWithSeries(only);
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, PRESENCE_START)).thenReturn(Optional.of(only));

        LaborClassificationCoverageIncompleteException ex = assertThrows(
                LaborClassificationCoverageIncompleteException.class,
                () -> service.delete(command(PRESENCE_START))
        );

        assertEquals(List.of(new LaborClassificationPeriod(PRESENCE_START, null)), ex.gaps());
        verify(laborClassificationRepository, never()).delete(any());
        verify(laborClassificationRepository, never()).update(any(), any());
    }

    @Test
    void deletingOneInTheMiddleIsRejectedNamingTheNeighboursToStretch() {
        LaborClassification first = occurrence(PRESENCE_START, LocalDate.of(2026, 1, 15));
        LaborClassification middle = occurrence(LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31));
        LaborClassification last = occurrence(LocalDate.of(2026, 2, 1), null);
        givenEmployeeWithSeries(first, middle, last);
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, middle.getStartDate())).thenReturn(Optional.of(middle));

        LaborClassificationCoverageIncompleteException ex = assertThrows(
                LaborClassificationCoverageIncompleteException.class,
                () -> service.delete(command(LocalDate.of(2026, 1, 16)))
        );

        assertEquals(
                List.of(new LaborClassificationPeriod(LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31))),
                ex.gaps()
        );
        assertEquals(
                List.of(
                        new LaborClassificationPeriod(PRESENCE_START, LocalDate.of(2026, 1, 15)),
                        new LaborClassificationPeriod(LocalDate.of(2026, 2, 1), null)
                ),
                ex.stretchCandidates()
        );
        verify(laborClassificationRepository, never()).delete(any());
        verify(laborClassificationRepository, never()).update(any(), any());
    }

    @Test
    void rejectsWhenTheOccurrenceDoesNotExist() {
        whenEmployeeExists();
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, LocalDate.of(2026, 7, 1))).thenReturn(Optional.empty());

        assertThrows(LaborClassificationNotFoundException.class, () -> service.delete(command(LocalDate.of(2026, 7, 1))));
        verify(laborClassificationRepository, never()).delete(any());
    }

    private DeleteLaborClassificationCommand command(LocalDate startDate) {
        return new DeleteLaborClassificationCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER, startDate);
    }

    private void givenEmployeeWithSeries(LaborClassification... occurrences) {
        whenEmployeeExists();
        when(laborClassificationRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new PresencePeriod(PRESENCE_START, null)));
    }

    private void whenEmployeeExists() {
        when(employeeLaborClassificationLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(new EmployeeLaborClassificationContext(10L, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER)));
    }

    private static LaborClassification occurrence(LocalDate startDate, LocalDate endDate) {
        return new LaborClassification(10L, "AGR_OFFICE", "CAT_ADMIN", startDate, endDate);
    }
}
