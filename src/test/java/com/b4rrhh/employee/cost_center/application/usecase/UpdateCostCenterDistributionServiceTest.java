package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.port.CostCenterPresenceConsistencyPort;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.application.port.PresencePeriod;
import com.b4rrhh.employee.cost_center.application.service.CostCenterCatalogValidator;
import com.b4rrhh.employee.cost_center.application.service.CostCenterTimelineService;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterCatalogValueInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionNotFoundException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionOverlapException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionPercentageExceededException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterEmployeeNotFoundException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionPeriod;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
import com.b4rrhh.employee.cost_center.domain.service.CostCenterDistributionTimelineValidator;
import com.b4rrhh.employee.cost_center.domain.service.CostCenterDistributionWindowGrouper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Correcting a distribution window through the temporal component
 * (ADR-057): the lines are replaced as a set, the dates only when the
 * correction gives them, and the corrected dates are judged by the same
 * invariants as an add. The timeline service is real; the repository and
 * the presence port are mocked. The employee is present from 2026-01-01
 * onwards.
 */
@ExtendWith(MockitoExtension.class)
class UpdateCostCenterDistributionServiceTest {

    private static final String RSC = "ESP";
    private static final String ETC = "INTERNAL";
    private static final String EN = "EMP001";
    private static final Long EMPLOYEE_ID = 10L;
    private static final LocalDate JAN_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JAN_31 = LocalDate.of(2026, 1, 31);
    private static final LocalDate FEB_1 = LocalDate.of(2026, 2, 1);

    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private EmployeeCostCenterLookupPort employeeCostCenterLookupPort;
    @Mock
    private CostCenterPresenceConsistencyPort presencePort;

    private UpdateCostCenterDistributionService service;

    @BeforeEach
    void setUp() {
        CostCenterDistributionWindowGrouper grouper = new CostCenterDistributionWindowGrouper();
        service = new UpdateCostCenterDistributionService(
                costCenterRepository,
                employeeCostCenterLookupPort,
                new TestCatalogValidator(),
                new CostCenterTimelineService(costCenterRepository, presencePort, grouper),
                new CostCenterDistributionTimelineValidator(),
                grouper
        );
    }

    // The typo the issue is about: a percentage is fixed where it was, no window is invented.
    @Test
    void correctsTheLinesWithoutTouchingTheDatesWhenNoStartDateComes() {
        CostCenterAllocation hr = line("CC_HR", 60, FEB_1, null);
        CostCenterAllocation it = line("CC_IT", 40, FEB_1, null);
        givenEmployeeWithSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), hr, it);
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, FEB_1)).thenReturn(List.of(hr, it));

        CostCenterDistributionWindow corrected = service.update(command(FEB_1, null, null,
                List.of(item("CC_HR", 70), item("CC_IT", 30))));

        assertEquals(FEB_1, corrected.getStartDate());
        assertNull(corrected.getEndDate());
        assertEquals(new BigDecimal("100"), corrected.getTotalAllocationPercentage());
        verify(costCenterRepository).deleteAllForWindow(EMPLOYEE_ID, FEB_1);
        ArgumentCaptor<List<CostCenterAllocation>> saved = ArgumentCaptor.forClass(List.class);
        verify(costCenterRepository).saveAll(saved.capture());
        assertEquals(2, saved.getValue().size());
        assertEquals(new BigDecimal("70"), saved.getValue().get(0).getAllocationPercentage());
        assertEquals(FEB_1, saved.getValue().get(0).getStartDate());
        assertNull(saved.getValue().get(0).getEndDate());
        verify(costCenterRepository, never()).adjustWindowEndDate(any(), any(), any());
    }

    @Test
    void correctsTheDatesWhenTheyComeAndTheLinesTakeThem() {
        CostCenterAllocation hr = line("CC_HR", 100, FEB_1, LocalDate.of(2026, 2, 28));
        givenEmployeeWithSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), hr);
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, FEB_1)).thenReturn(List.of(hr));

        CostCenterDistributionWindow corrected = service.update(command(FEB_1, FEB_1, null,
                List.of(item("CC_HR", 100))));

        assertEquals(FEB_1, corrected.getStartDate());
        assertNull(corrected.getEndDate());
        ArgumentCaptor<List<CostCenterAllocation>> saved = ArgumentCaptor.forClass(List.class);
        verify(costCenterRepository).saveAll(saved.capture());
        assertNull(saved.getValue().get(0).getEndDate());
    }

    // Optional coverage (ADR-057, decision 1; backend#54): the gap the corrected dates leave is
    // legal, so the correction goes through and nothing else moves (decision 3): the previous
    // window keeps its end.
    @Test
    void correctedDatesThatLeaveAGapAreAcceptedAndTheGapStays() {
        CostCenterAllocation hr = line("CC_HR", 100, FEB_1, null);
        givenEmployeeWithSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), hr);
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, FEB_1)).thenReturn(List.of(hr));

        CostCenterDistributionWindow corrected = service.update(command(FEB_1, LocalDate.of(2026, 3, 1), null, List.of(item("CC_HR", 100))));

        assertEquals(LocalDate.of(2026, 3, 1), corrected.getStartDate());
        assertNull(corrected.getEndDate());
        verify(costCenterRepository).deleteAllForWindow(EMPLOYEE_ID, FEB_1);
        ArgumentCaptor<List<CostCenterAllocation>> saved = ArgumentCaptor.forClass(List.class);
        verify(costCenterRepository).saveAll(saved.capture());
        assertEquals(LocalDate.of(2026, 3, 1), saved.getValue().get(0).getStartDate());
        verify(costCenterRepository, never()).adjustWindowEndDate(any(), any(), any());
    }

    @Test
    void correctedDatesThatOverlapAnotherWindowAreRejectedWithTheSharedDates() {
        CostCenterAllocation hr = line("CC_HR", 100, FEB_1, null);
        givenEmployeeWithSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), hr);
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, FEB_1)).thenReturn(List.of(hr));

        CostCenterDistributionOverlapException ex = assertThrows(
                CostCenterDistributionOverlapException.class,
                () -> service.update(command(FEB_1, LocalDate.of(2026, 1, 15), null, List.of(item("CC_HR", 100))))
        );

        assertEquals(List.of(new CostCenterDistributionPeriod(LocalDate.of(2026, 1, 15), JAN_31)), ex.overlaps());
        verify(costCenterRepository, never()).saveAll(any());
    }

    @Test
    void rejectsLinesThatAddUpToMoreThan100BeforeAskingThePlan() {
        CostCenterAllocation hr = line("CC_HR", 100, FEB_1, null);
        givenEmployeeFound();
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, FEB_1)).thenReturn(List.of(hr));

        assertThrows(
                CostCenterDistributionPercentageExceededException.class,
                () -> service.update(command(FEB_1, null, null, List.of(item("CC_HR", 80), item("CC_IT", 30))))
        );
        verify(costCenterRepository, never()).saveAll(any());
    }

    @Test
    void rejectsAnUnknownCatalogValue() {
        CostCenterAllocation hr = line("CC_HR", 100, FEB_1, null);
        givenEmployeeFound();
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, FEB_1)).thenReturn(List.of(hr));

        assertThrows(
                CostCenterCatalogValueInvalidException.class,
                () -> service.update(command(FEB_1, null, null, List.of(item("INVALID_CC", 100))))
        );
    }

    @Test
    void rejectsWhenTheWindowDoesNotExist() {
        givenEmployeeFound();
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, FEB_1)).thenReturn(List.of());

        assertThrows(
                CostCenterDistributionNotFoundException.class,
                () -> service.update(command(FEB_1, null, null, List.of(item("CC_HR", 100))))
        );
        verify(costCenterRepository, never()).deleteAllForWindow(any(), any());
    }

    @Test
    void rejectsWhenEmployeeNotFound() {
        when(employeeCostCenterLookupPort.findByBusinessKeyForUpdate(RSC, ETC, EN)).thenReturn(Optional.empty());

        assertThrows(
                CostCenterEmployeeNotFoundException.class,
                () -> service.update(command(FEB_1, null, null, List.of(item("CC_HR", 100))))
        );
    }

    @Test
    void rejectsWhenNoItemsProvided() {
        assertThrows(
                CostCenterDistributionInvalidException.class,
                () -> service.update(command(FEB_1, null, null, List.of()))
        );
    }

    private void givenEmployeeFound() {
        when(employeeCostCenterLookupPort.findByBusinessKeyForUpdate(RSC, ETC, EN))
                .thenReturn(Optional.of(new EmployeeCostCenterContext(EMPLOYEE_ID, RSC, ETC, EN)));
    }

    private void givenEmployeeWithSeries(CostCenterAllocation... lines) {
        givenEmployeeFound();
        when(costCenterRepository.findByEmployeeIdOrderByStartDate(EMPLOYEE_ID)).thenReturn(List.of(lines));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of(new PresencePeriod(JAN_1, null)));
    }

    private UpdateCostCenterDistributionCommand command(
            LocalDate windowStartDate,
            LocalDate startDate,
            LocalDate endDate,
            List<CostCenterDistributionItem> items
    ) {
        return new UpdateCostCenterDistributionCommand(RSC, ETC, EN, windowStartDate, startDate, endDate, items);
    }

    private static CostCenterDistributionItem item(String code, int percentage) {
        return new CostCenterDistributionItem(code, BigDecimal.valueOf(percentage));
    }

    private static CostCenterAllocation line(String code, int percentage, LocalDate startDate, LocalDate endDate) {
        return new CostCenterAllocation(EMPLOYEE_ID, code, BigDecimal.valueOf(percentage), startDate, endDate);
    }

    private static final class TestCatalogValidator extends CostCenterCatalogValidator {
        TestCatalogValidator() {
            super(null);
        }

        @Override
        public String normalizeRequiredCode(String fieldName, String value) {
            if (value == null || value.trim().isEmpty()) {
                throw new CostCenterCatalogValueInvalidException(fieldName, String.valueOf(value));
            }
            return value.trim().toUpperCase();
        }

        @Override
        public void validateCostCenterCode(String ruleSystemCode, String costCenterCode, LocalDate referenceDate) {
            if (costCenterCode.startsWith("INVALID")) {
                throw new CostCenterCatalogValueInvalidException("costCenterCode", costCenterCode);
            }
        }
    }
}
