package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.port.CostCenterPresenceConsistencyPort;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.application.port.PresencePeriod;
import com.b4rrhh.employee.cost_center.application.service.CostCenterTimelineService;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionCoverageGapException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionNotFoundException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionPeriod;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Removing a distribution window (ADR-057, decision 3): the delete is
 * bounded by the invariants and moves the window whole. The timeline service
 * is real; the repository and the presence port are mocked. The employee is
 * present from 2026-01-01 onwards.
 */
@ExtendWith(MockitoExtension.class)
class DeleteCostCenterDistributionServiceTest {

    private static final String RSC = "ESP";
    private static final String ETC = "INTERNAL";
    private static final String EN = "EMP001";
    private static final Long EMPLOYEE_ID = 10L;
    private static final LocalDate JAN_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate DAY_15 = LocalDate.of(2026, 1, 15);
    private static final LocalDate DAY_16 = LocalDate.of(2026, 1, 16);
    private static final LocalDate JAN_31 = LocalDate.of(2026, 1, 31);
    private static final LocalDate FEB_1 = LocalDate.of(2026, 2, 1);

    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private EmployeeCostCenterLookupPort employeeCostCenterLookupPort;
    @Mock
    private CostCenterPresenceConsistencyPort presencePort;

    private DeleteCostCenterDistributionService service;

    @BeforeEach
    void setUp() {
        CostCenterDistributionWindowGrouper grouper = new CostCenterDistributionWindowGrouper();
        service = new DeleteCostCenterDistributionService(
                costCenterRepository,
                employeeCostCenterLookupPort,
                new CostCenterTimelineService(costCenterRepository, presencePort, grouper),
                grouper
        );
    }

    @Test
    void deletingTheLastWindowReopensThePreviousOneWithAllItsLines() {
        CostCenterAllocation admin = line("CC_ADMIN", 60, JAN_1, DAY_15);
        CostCenterAllocation hr = line("CC_HR", 40, JAN_1, DAY_15);
        CostCenterAllocation last = line("CC_IT", 100, DAY_16, null);
        givenEmployeeWithSeries(admin, hr, last);
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, DAY_16)).thenReturn(List.of(last));

        service.delete(command(DAY_16));

        verify(costCenterRepository).adjustWindowEndDate(EMPLOYEE_ID, JAN_1, null);
        verify(costCenterRepository).deleteAllForWindow(EMPLOYEE_ID, DAY_16);
    }

    // The only window starts the presence: removing it uncovers the presence.
    @Test
    void deletingTheOnlyWindowIsRejectedBecauseThePresenceWouldBeUncovered() {
        CostCenterAllocation only = line("CC_ADMIN", 100, JAN_1, null);
        givenEmployeeWithSeries(only);
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, JAN_1)).thenReturn(List.of(only));

        CostCenterDistributionCoverageGapException ex = assertThrows(
                CostCenterDistributionCoverageGapException.class,
                () -> service.delete(command(JAN_1))
        );

        assertEquals(List.of(new CostCenterDistributionPeriod(JAN_1, null)), ex.gaps());
        verify(costCenterRepository, never()).deleteAllForWindow(any(), any());
        verify(costCenterRepository, never()).adjustWindowEndDate(any(), any(), any());
    }

    @Test
    void deletingAWindowInTheMiddleIsRejectedNamingTheNeighboursToStretch() {
        CostCenterAllocation first = line("CC_ADMIN", 100, JAN_1, DAY_15);
        CostCenterAllocation middle = line("CC_HR", 100, DAY_16, JAN_31);
        CostCenterAllocation last = line("CC_IT", 100, FEB_1, null);
        givenEmployeeWithSeries(first, middle, last);
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, DAY_16)).thenReturn(List.of(middle));

        CostCenterDistributionCoverageGapException ex = assertThrows(
                CostCenterDistributionCoverageGapException.class,
                () -> service.delete(command(DAY_16))
        );

        assertEquals(List.of(new CostCenterDistributionPeriod(DAY_16, JAN_31)), ex.gaps());
        assertEquals(
                List.of(new CostCenterDistributionPeriod(JAN_1, DAY_15), new CostCenterDistributionPeriod(FEB_1, null)),
                ex.stretchCandidates()
        );
        verify(costCenterRepository, never()).deleteAllForWindow(any(), any());
        verify(costCenterRepository, never()).adjustWindowEndDate(any(), any(), any());
    }

    @Test
    void throwsNotFoundWhenTheWindowDoesNotExist() {
        whenEmployeeExists();
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, FEB_1)).thenReturn(List.of());

        assertThrows(CostCenterDistributionNotFoundException.class, () -> service.delete(command(FEB_1)));

        verify(costCenterRepository, never()).deleteAllForWindow(any(), any());
    }

    private DeleteCostCenterDistributionCommand command(LocalDate windowStartDate) {
        return new DeleteCostCenterDistributionCommand(RSC, ETC, EN, windowStartDate);
    }

    private void givenEmployeeWithSeries(CostCenterAllocation... lines) {
        whenEmployeeExists();
        when(costCenterRepository.findByEmployeeIdOrderByStartDate(EMPLOYEE_ID)).thenReturn(List.of(lines));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of(new PresencePeriod(JAN_1, null)));
    }

    private void whenEmployeeExists() {
        when(employeeCostCenterLookupPort.findByBusinessKeyForUpdate(RSC, ETC, EN))
                .thenReturn(Optional.of(new EmployeeCostCenterContext(EMPLOYEE_ID, RSC, ETC, EN)));
    }

    private static CostCenterAllocation line(String code, int percentage, LocalDate startDate, LocalDate endDate) {
        return new CostCenterAllocation(EMPLOYEE_ID, code, BigDecimal.valueOf(percentage), startDate, endDate);
    }
}
