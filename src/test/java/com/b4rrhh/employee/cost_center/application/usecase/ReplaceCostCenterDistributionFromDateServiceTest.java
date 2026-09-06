package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.port.CostCenterPresenceConsistencyPort;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.application.port.PresencePeriod;
import com.b4rrhh.employee.cost_center.application.service.CostCenterCatalogValidator;
import com.b4rrhh.employee.cost_center.application.service.CostCenterTimelineService;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterCatalogValueInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionCoverageGapException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionIsACorrectionException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The deprecated replace-from-date as an adapter over the temporal component
 * (ADR-057): an add whose end date is the tail of the window in force. The
 * timeline service is real; the repository and the presence port are mocked.
 * The employee is present from 2026-01-01 onwards.
 */
@ExtendWith(MockitoExtension.class)
class ReplaceCostCenterDistributionFromDateServiceTest {

    private static final String RSC = "ESP";
    private static final String ETC = "INTERNAL";
    private static final String EN = "EMP001";
    private static final Long EMPLOYEE_ID = 10L;
    private static final LocalDate ORIGINAL_START = LocalDate.of(2026, 1, 1);
    private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2026, 4, 1);

    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private EmployeeCostCenterLookupPort employeeCostCenterLookupPort;
    @Mock
    private CostCenterPresenceConsistencyPort costCenterPresenceConsistencyPort;

    private ReplaceCostCenterDistributionFromDateService service;

    @BeforeEach
    void setUp() {
        CostCenterCatalogValidator validator = new TestCatalogValidator();
        CostCenterDistributionTimelineValidator timelineValidator = new CostCenterDistributionTimelineValidator();
        CostCenterDistributionWindowGrouper grouper = new CostCenterDistributionWindowGrouper();
        service = new ReplaceCostCenterDistributionFromDateService(
                costCenterRepository,
                employeeCostCenterLookupPort,
                validator,
                new CostCenterTimelineService(costCenterRepository, costCenterPresenceConsistencyPort, grouper),
                timelineValidator,
                grouper
        );
    }

    // Test 9: replace-from-date closes previous active window and creates new one
    @Test
    void replacesActiveWindowAndCreatesNewOne() {
        CostCenterAllocation existingLine = new CostCenterAllocation(
                EMPLOYEE_ID, "CC_X", new BigDecimal("100"), ORIGINAL_START, null
        );
        givenEmployeeWithSeries(existingLine);

        CostCenterDistributionWindow result = service.replaceFromDate(command(
                List.of(new CostCenterDistributionItem("CC_A", new BigDecimal("60")),
                        new CostCenterDistributionItem("CC_B", new BigDecimal("40")))
        ));

        assertNotNull(result);
        assertEquals(EFFECTIVE_DATE, result.getStartDate());
        assertNull(result.getEndDate());
        assertEquals(2, result.getItems().size());
        assertEquals(new BigDecimal("100"), result.getTotalAllocationPercentage());

        // Previous window must have been closed
        verify(costCenterRepository).adjustWindowEndDate(EMPLOYEE_ID, ORIGINAL_START, EFFECTIVE_DATE.minusDays(1));
        verify(costCenterRepository).saveAll(any());
    }

    // Test 16: the occurrence is the window, not the line (ADR-057, decision 0)
    @Test
    void replaceUsesWindowStartDateNotIndividualLineStartDates() {
        // Two parallel lines forming a window with the same startDate
        CostCenterAllocation lineA = new CostCenterAllocation(
                EMPLOYEE_ID, "CC_X", new BigDecimal("60"), ORIGINAL_START, null
        );
        CostCenterAllocation lineB = new CostCenterAllocation(
                EMPLOYEE_ID, "CC_Y", new BigDecimal("40"), ORIGINAL_START, null
        );
        givenEmployeeWithSeries(lineA, lineB);

        service.replaceFromDate(command(
                List.of(new CostCenterDistributionItem("CC_NEW", new BigDecimal("100")))
        ));

        // Both lines of the window (identified by ORIGINAL_START) must be closed together
        verify(costCenterRepository).adjustWindowEndDate(EMPLOYEE_ID, ORIGINAL_START, EFFECTIVE_DATE.minusDays(1));
    }

    // The old SPLIT: inside a closed window the replacement takes its tail and the series stays covered.
    @Test
    void replaceInsideAClosedWindowTakesItsTail() {
        LocalDate may31 = LocalDate.of(2026, 5, 31);
        CostCenterAllocation closed = new CostCenterAllocation(EMPLOYEE_ID, "CC_X", new BigDecimal("100"), ORIGINAL_START, may31);
        CostCenterAllocation next = new CostCenterAllocation(EMPLOYEE_ID, "CC_Y", new BigDecimal("100"), may31.plusDays(1), null);
        givenEmployeeWithSeries(closed, next);

        CostCenterDistributionWindow result = service.replaceFromDate(command(
                List.of(new CostCenterDistributionItem("CC_A", new BigDecimal("100")))
        ));

        assertEquals(EFFECTIVE_DATE, result.getStartDate());
        assertEquals(may31, result.getEndDate());
        verify(costCenterRepository).adjustWindowEndDate(EMPLOYEE_ID, ORIGINAL_START, EFFECTIVE_DATE.minusDays(1));
    }

    // The old EXACT_START replaced silently; now it says it is a correction and writes nothing.
    @Test
    void replaceOnTheStartDateOfAWindowIsRejectedAsItsCorrectionAndWritesNothing() {
        CostCenterAllocation existing = new CostCenterAllocation(EMPLOYEE_ID, "CC_X", new BigDecimal("100"), ORIGINAL_START, null);
        givenEmployeeWithSeries(existing);

        CostCenterDistributionIsACorrectionException ex = assertThrows(
                CostCenterDistributionIsACorrectionException.class,
                () -> service.replaceFromDate(new ReplaceCostCenterDistributionFromDateCommand(
                        RSC, ETC, EN, ORIGINAL_START,
                        List.of(new CostCenterDistributionItem("CC_A", new BigDecimal("100")))
                ))
        );

        assertEquals(new CostCenterDistributionPeriod(ORIGINAL_START, null), ex.correctedOccurrence());
        verify(costCenterRepository, never()).saveAll(any());
        verify(costCenterRepository, never()).adjustWindowEndDate(any(), any(), any());
    }

    // What used to be "no active distribution before effectiveDate" is now the invariant's
    // call: with nothing in force, the add leaves the presence uncovered before it.
    @Test
    void replacingWhereNothingIsInForceIsJudgedByTheInvariantsAndWritesNothing() {
        givenEmployeeWithSeries();

        CostCenterDistributionCoverageGapException ex = assertThrows(
                CostCenterDistributionCoverageGapException.class,
                () -> service.replaceFromDate(command(
                        List.of(new CostCenterDistributionItem("CC_A", new BigDecimal("100")))
                ))
        );

        assertEquals(List.of(new CostCenterDistributionPeriod(ORIGINAL_START, EFFECTIVE_DATE.minusDays(1))), ex.gaps());
        verify(costCenterRepository, never()).saveAll(any());
    }

    @Test
    void rejectsWhenEmployeeNotFound() {
        when(employeeCostCenterLookupPort.findByBusinessKeyForUpdate(RSC, ETC, EN))
                .thenReturn(Optional.empty());

        assertThrows(CostCenterEmployeeNotFoundException.class, () ->
                service.replaceFromDate(command(
                        List.of(new CostCenterDistributionItem("CC_A", new BigDecimal("100")))
                ))
        );
    }

    @Test
    void rejectsWhenNoItemsProvided() {
        assertThrows(CostCenterDistributionInvalidException.class, () ->
                service.replaceFromDate(command(List.of()))
        );
    }

    // helpers

    private void givenEmployeeFound() {
        when(employeeCostCenterLookupPort.findByBusinessKeyForUpdate(RSC, ETC, EN))
                .thenReturn(Optional.of(new EmployeeCostCenterContext(EMPLOYEE_ID, RSC, ETC, EN)));
    }

    private void givenEmployeeWithSeries(CostCenterAllocation... lines) {
        givenEmployeeFound();
        when(costCenterRepository.findByEmployeeIdOrderByStartDate(EMPLOYEE_ID)).thenReturn(List.of(lines));
        when(costCenterPresenceConsistencyPort.findPresencePeriodsByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of(new PresencePeriod(ORIGINAL_START, null)));
    }

    private ReplaceCostCenterDistributionFromDateCommand command(List<CostCenterDistributionItem> items) {
        return new ReplaceCostCenterDistributionFromDateCommand(RSC, ETC, EN, EFFECTIVE_DATE, items);
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
