package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.port.CostCenterPresenceConsistencyPort;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.application.port.PresencePeriod;
import com.b4rrhh.employee.cost_center.application.service.CostCenterCatalogValidator;
import com.b4rrhh.employee.cost_center.application.service.CostCenterTimelineService;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterCatalogValueInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionIsACorrectionException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionPercentageExceededException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterEmployeeNotFoundException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.cost_center.domain.exception.InvalidAllocationPercentageException;
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
 * Adding a distribution window through the temporal component (ADR-057).
 * The timeline service is real; the repository and the presence port are
 * mocked. The employee is present from 2026-04-01 onwards.
 */
@ExtendWith(MockitoExtension.class)
class CreateCostCenterDistributionServiceTest {

    private static final String RSC = "ESP";
    private static final String ETC = "INTERNAL";
    private static final String EN = "EMP001";
    private static final Long EMPLOYEE_ID = 10L;
    private static final LocalDate START = LocalDate.of(2026, 4, 1);

    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private EmployeeCostCenterLookupPort employeeCostCenterLookupPort;
    @Mock
    private CostCenterPresenceConsistencyPort costCenterPresenceConsistencyPort;

    private CreateCostCenterDistributionService service;

    @BeforeEach
    void setUp() {
        CostCenterCatalogValidator validator = new TestCatalogValidator();
        CostCenterDistributionTimelineValidator timelineValidator = new CostCenterDistributionTimelineValidator();
        service = new CreateCostCenterDistributionService(
                costCenterRepository,
                employeeCostCenterLookupPort,
                validator,
                new CostCenterTimelineService(
                        costCenterRepository,
                        costCenterPresenceConsistencyPort,
                        new CostCenterDistributionWindowGrouper()
                ),
                timelineValidator
        );
    }

    // Test 1: create valid single-line 100% distribution
    @Test
    void createsValidSingleLineFull100Distribution() {
        givenEmployeeFound();
        givenEmptySeriesWithPresenceFromStart();

        CostCenterDistributionWindow window = service.create(command(
                List.of(item("CC_A", new BigDecimal("100")))
        ));

        assertNotNull(window);
        assertEquals(1, window.getItems().size());
        assertEquals(new BigDecimal("100"), window.getTotalAllocationPercentage());
        assertNull(window.getEndDate());
        verify(costCenterRepository).saveAll(any());
        verify(costCenterRepository, never()).adjustWindowEndDate(any(), any(), any());
    }

    // Test 2: create valid 50/50 parallel distribution
    @Test
    void createsValid5050ParallelDistribution() {
        givenEmployeeFound();
        givenEmptySeriesWithPresenceFromStart();

        CostCenterDistributionWindow window = service.create(command(
                List.of(item("CC_A", new BigDecimal("50")), item("CC_B", new BigDecimal("50")))
        ));

        assertEquals(2, window.getItems().size());
        assertEquals(new BigDecimal("100"), window.getTotalAllocationPercentage());
    }

    // ADR-057 §2: the add closes the window in force the day before, every line of it, instead
    // of returning the old conflict. The two lines of that window are one occurrence.
    @Test
    void addingAfterTheOpenWindowClosesItTheDayBeforeWithAllItsLines() {
        givenEmployeeFound();
        givenSeriesWithPresenceFromStart(
                line("CC_X", 60, START, null),
                line("CC_Y", 40, START, null)
        );

        CostCenterDistributionWindow window = service.create(command(START.plusDays(15),
                List.of(item("CC_A", new BigDecimal("100")))
        ));

        assertEquals(START.plusDays(15), window.getStartDate());
        verify(costCenterRepository).adjustWindowEndDate(EMPLOYEE_ID, START, START.plusDays(14));
        verify(costCenterRepository).saveAll(any());
    }

    // What the old conflict said ("use replace-from-date") is now a plan that names the window
    // the add would correct (backend#52), and nothing is written.
    @Test
    void addingOnTheStartDateOfTheOpenWindowIsRejectedAsItsCorrection() {
        givenEmployeeFound();
        givenSeriesWithPresenceFromStart(line("CC_X", 100, START, null));

        CostCenterDistributionIsACorrectionException ex = assertThrows(
                CostCenterDistributionIsACorrectionException.class,
                () -> service.create(command(List.of(item("CC_A", new BigDecimal("100")))))
        );

        assertEquals(new CostCenterDistributionPeriod(START, null), ex.correctedOccurrence());
        verify(costCenterRepository, never()).saveAll(any());
        verify(costCenterRepository, never()).adjustWindowEndDate(any(), any(), any());
    }

    // Optional coverage (ADR-057, decision 1; backend#54): the rest of the presence stays uncovered
    // and that is legal here. The window in force still closes the day before, as in any add.
    @Test
    void addingAClosedWindowThatLeavesTheRestOfThePresenceUncoveredIsAccepted() {
        givenEmployeeFound();
        givenSeriesWithPresenceFromStart(line("CC_X", 100, START, null));

        CostCenterDistributionWindow window = service.create(command(START.plusDays(15), START.plusDays(30),
                List.of(item("CC_A", new BigDecimal("100")))));

        assertEquals(START.plusDays(15), window.getStartDate());
        assertEquals(START.plusDays(30), window.getEndDate());
        verify(costCenterRepository).adjustWindowEndDate(EMPLOYEE_ID, START, START.plusDays(14));
        verify(costCenterRepository).saveAll(any());
    }

    // backend#54: an employee without a distribution may start one later than the hire date. The
    // stretch before it is a gap, and a gap is legal for this series.
    @Test
    void anEmployeeWithoutADistributionCanStartOneAfterTheHireDate() {
        givenEmployeeFound();
        givenEmptySeriesWithPresenceFromStart();

        CostCenterDistributionWindow window = service.create(command(START.plusDays(15),
                List.of(item("CC_A", new BigDecimal("100")))));

        assertEquals(START.plusDays(15), window.getStartDate());
        assertNull(window.getEndDate());
        verify(costCenterRepository).saveAll(any());
        verify(costCenterRepository, never()).adjustWindowEndDate(any(), any(), any());
    }

    // Test 3: reject total percentage > 100
    @Test
    void rejectsTotalPercentageExceeds100() {
        givenEmployeeFound();

        assertThrows(CostCenterDistributionPercentageExceededException.class, () ->
                service.create(command(
                        List.of(item("CC_A", new BigDecimal("80")), item("CC_B", new BigDecimal("30")))
                ))
        );
        verify(costCenterRepository, never()).saveAll(any());
    }

    // Test 4: reject zero percentage
    @Test
    void rejectsZeroAllocationPercentage() {
        givenEmployeeFound();

        assertThrows(InvalidAllocationPercentageException.class, () ->
                service.create(command(
                        List.of(item("CC_A", BigDecimal.ZERO))
                ))
        );
    }

    // Test 5: reject negative percentage
    @Test
    void rejectsNegativeAllocationPercentage() {
        givenEmployeeFound();

        assertThrows(InvalidAllocationPercentageException.class, () ->
                service.create(command(
                        List.of(item("CC_A", new BigDecimal("-10")))
                ))
        );
    }

    // Test 7: reject invalid COST_CENTER catalog value
    @Test
    void rejectsInvalidCostCenterCatalogValue() {
        givenEmployeeFound();

        assertThrows(CostCenterCatalogValueInvalidException.class, () ->
                service.create(command(
                        List.of(item("INVALID_CC", new BigDecimal("100")))
                ))
        );
        verify(costCenterRepository, never()).saveAll(any());
    }

    // Test 8: reject lines outside presence
    @Test
    void rejectsWhenPeriodIsOutsidePresence() {
        givenEmployeeFound();
        when(costCenterRepository.findByEmployeeIdOrderByStartDate(EMPLOYEE_ID)).thenReturn(List.of());
        when(costCenterPresenceConsistencyPort.findPresencePeriodsByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of(new PresencePeriod(START.plusDays(1), null)));

        assertThrows(CostCenterOutsidePresencePeriodException.class, () ->
                service.create(command(
                        List.of(item("CC_A", new BigDecimal("100")))
                ))
        );
        verify(costCenterRepository, never()).saveAll(any());
    }

    // Test 13: no technical IDs in canonical API (window returned has no IDs exposed)
    @Test
    void windowResponseContainsNoTechnicalIds() {
        givenEmployeeFound();
        givenEmptySeriesWithPresenceFromStart();

        CostCenterDistributionWindow window = service.create(command(
                List.of(item("CC_A", new BigDecimal("100")))
        ));

        // CostCenterDistributionWindow exposes startDate, endDate, items, totalPercentage — no employeeId
        assertNotNull(window.getStartDate());
        assertNull(window.getEndDate());
    }

    // Test 14: ownership enforced by employee business key
    @Test
    void rejectsWhenEmployeeNotFound() {
        when(employeeCostCenterLookupPort.findByBusinessKeyForUpdate(RSC, ETC, EN))
                .thenReturn(Optional.empty());

        assertThrows(CostCenterEmployeeNotFoundException.class, () ->
                service.create(command(List.of(item("CC_A", new BigDecimal("100")))))
        );
    }

    @Test
    void rejectsWhenNoItemsProvided() {
        assertThrows(CostCenterDistributionInvalidException.class, () ->
                service.create(command(List.of()))
        );
    }

    // helpers

    private void givenEmployeeFound() {
        when(employeeCostCenterLookupPort.findByBusinessKeyForUpdate(RSC, ETC, EN))
                .thenReturn(Optional.of(new EmployeeCostCenterContext(EMPLOYEE_ID, RSC, ETC, EN)));
    }

    private void givenEmptySeriesWithPresenceFromStart() {
        givenSeriesWithPresenceFromStart();
    }

    private void givenSeriesWithPresenceFromStart(CostCenterAllocation... lines) {
        when(costCenterRepository.findByEmployeeIdOrderByStartDate(EMPLOYEE_ID)).thenReturn(List.of(lines));
        when(costCenterPresenceConsistencyPort.findPresencePeriodsByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of(new PresencePeriod(START, null)));
    }

    private CreateCostCenterDistributionCommand command(List<CostCenterDistributionItem> items) {
        return command(START, null, items);
    }

    private CreateCostCenterDistributionCommand command(LocalDate startDate, List<CostCenterDistributionItem> items) {
        return command(startDate, null, items);
    }

    private CreateCostCenterDistributionCommand command(
            LocalDate startDate,
            LocalDate endDate,
            List<CostCenterDistributionItem> items
    ) {
        return new CreateCostCenterDistributionCommand(RSC, ETC, EN, startDate, endDate, items);
    }

    private CostCenterDistributionItem item(String code, BigDecimal percentage) {
        return new CostCenterDistributionItem(code, percentage);
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
