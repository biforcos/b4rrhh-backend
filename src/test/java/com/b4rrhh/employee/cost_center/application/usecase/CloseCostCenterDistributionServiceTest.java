package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.port.CostCenterPresenceConsistencyPort;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.application.port.PresencePeriod;
import com.b4rrhh.employee.cost_center.application.service.CostCenterTimelineService;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionNotFoundException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterEmployeeNotFoundException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
import com.b4rrhh.employee.cost_center.domain.service.CostCenterDistributionWindowGrouper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The deprecated close as an adapter over the temporal component (ADR-057):
 * a correction of the end date, judged by the invariants. The timeline
 * service is real; the repository and the presence port are mocked.
 */
@ExtendWith(MockitoExtension.class)
class CloseCostCenterDistributionServiceTest {

    private static final String RSC = "ESP";
    private static final String ETC = "INTERNAL";
    private static final String EN = "EMP001";
    private static final Long EMPLOYEE_ID = 10L;
    private static final LocalDate WINDOW_START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 3, 31);

    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private EmployeeCostCenterLookupPort employeeCostCenterLookupPort;
    @Mock
    private CostCenterPresenceConsistencyPort costCenterPresenceConsistencyPort;

    private CloseCostCenterDistributionService service;

    @BeforeEach
    void setUp() {
        CostCenterDistributionWindowGrouper grouper = new CostCenterDistributionWindowGrouper();
        service = new CloseCostCenterDistributionService(
                costCenterRepository,
                employeeCostCenterLookupPort,
                new CostCenterTimelineService(costCenterRepository, costCenterPresenceConsistencyPort, grouper),
                grouper
        );
    }

    // Test 10: close window closes all allocation lines in the window. The presence ended that
    // day (the termination flow closes it first), so the correction leaves no gap.
    @Test
    void closesAllLinesInWindowAndReturnsClosed() {
        CostCenterAllocation lineA = new CostCenterAllocation(
                EMPLOYEE_ID, "CC_A", new BigDecimal("60"), WINDOW_START, null
        );
        CostCenterAllocation lineB = new CostCenterAllocation(
                EMPLOYEE_ID, "CC_B", new BigDecimal("40"), WINDOW_START, null
        );
        givenEmployeeWithSeriesAndPresenceUntil(END_DATE, lineA, lineB);
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, WINDOW_START))
                .thenReturn(List.of(lineA, lineB));

        CostCenterDistributionWindow result = service.close(command(WINDOW_START, END_DATE));

        assertNotNull(result);
        assertEquals(WINDOW_START, result.getStartDate());
        assertEquals(END_DATE, result.getEndDate());
        assertEquals(2, result.getItems().size());
        assertFalse(result.isActive());

        verify(costCenterRepository).adjustWindowEndDate(EMPLOYEE_ID, WINDOW_START, END_DATE);
    }

    // ADR-057: closing the window in force while the presence goes on leaves a gap. This series
    // declares optional coverage (decision 1; backend#54), so the gap is legal and the close goes through.
    @Test
    void closingWhileThePresenceGoesOnIsAcceptedAndLeavesTheGap() {
        CostCenterAllocation line = new CostCenterAllocation(
                EMPLOYEE_ID, "CC_A", new BigDecimal("100"), WINDOW_START, null
        );
        givenEmployeeWithSeriesAndPresenceUntil(null, line);
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, WINDOW_START))
                .thenReturn(List.of(line));

        CostCenterDistributionWindow result = service.close(command(WINDOW_START, END_DATE));

        assertEquals(END_DATE, result.getEndDate());
        verify(costCenterRepository).adjustWindowEndDate(EMPLOYEE_ID, WINDOW_START, END_DATE);
    }

    @Test
    void rejectsWhenWindowNotFound() {
        givenEmployeeFound();
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, WINDOW_START))
                .thenReturn(List.of());

        assertThrows(CostCenterDistributionNotFoundException.class, () ->
                service.close(command(WINDOW_START, END_DATE))
        );
        verify(costCenterRepository, never()).adjustWindowEndDate(any(), any(), any());
    }

    @Test
    void rejectsWhenEndDateBeforeWindowStartDate() {
        assertThrows(CostCenterDistributionInvalidException.class, () ->
                service.close(command(WINDOW_START, WINDOW_START.minusDays(1)))
        );
        verify(costCenterRepository, never()).findByEmployeeIdAndStartDate(any(), any());
    }

    @Test
    void rejectsWhenPeriodIsOutsidePresence() {
        CostCenterAllocation line = new CostCenterAllocation(
                EMPLOYEE_ID, "CC_A", new BigDecimal("100"), WINDOW_START, null
        );
        givenEmployeeWithSeriesAndPresenceUntil(END_DATE.minusDays(1), line);
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, WINDOW_START))
                .thenReturn(List.of(line));

        assertThrows(CostCenterOutsidePresencePeriodException.class, () ->
                service.close(command(WINDOW_START, END_DATE))
        );
        verify(costCenterRepository, never()).adjustWindowEndDate(any(), any(), any());
    }

    @Test
    void rejectsWhenEmployeeNotFound() {
        when(employeeCostCenterLookupPort.findByBusinessKeyForUpdate(RSC, ETC, EN))
                .thenReturn(Optional.empty());

        assertThrows(CostCenterEmployeeNotFoundException.class, () ->
                service.close(command(WINDOW_START, END_DATE))
        );
    }

    // helpers

    private void givenEmployeeFound() {
        when(employeeCostCenterLookupPort.findByBusinessKeyForUpdate(RSC, ETC, EN))
                .thenReturn(Optional.of(new EmployeeCostCenterContext(EMPLOYEE_ID, RSC, ETC, EN)));
    }

    private void givenEmployeeWithSeriesAndPresenceUntil(LocalDate presenceEnd, CostCenterAllocation... lines) {
        givenEmployeeFound();
        when(costCenterRepository.findByEmployeeIdOrderByStartDate(EMPLOYEE_ID)).thenReturn(List.of(lines));
        when(costCenterPresenceConsistencyPort.findPresencePeriodsByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of(new PresencePeriod(WINDOW_START, presenceEnd)));
    }

    private CloseCostCenterDistributionCommand command(LocalDate windowStartDate, LocalDate endDate) {
        return new CloseCostCenterDistributionCommand(RSC, ETC, EN, windowStartDate, endDate);
    }
}
