package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.model.CostCenterDistributionPlan;
import com.b4rrhh.employee.cost_center.application.port.CostCenterPresenceConsistencyPort;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.application.port.PresencePeriod;
import com.b4rrhh.employee.cost_center.application.service.CostCenterTimelineService;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionNotFoundException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterEmployeeNotFoundException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionPeriod;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
import com.b4rrhh.employee.cost_center.domain.service.CostCenterDistributionWindowGrouper;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The plan use case answers and never writes (ADR-057, decision 6). The
 * timeline service is real; the employee is present from 2026-01-01 onwards
 * with one open window from that day, split between two cost centers.
 */
@ExtendWith(MockitoExtension.class)
class PlanCostCenterDistributionChangeServiceTest {

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

    private PlanCostCenterDistributionChangeService service;

    @BeforeEach
    void setUp() {
        CostCenterDistributionWindowGrouper grouper = new CostCenterDistributionWindowGrouper();
        service = new PlanCostCenterDistributionChangeService(
                costCenterRepository,
                employeeCostCenterLookupPort,
                new CostCenterTimelineService(costCenterRepository, presencePort, grouper),
                grouper
        );
    }

    @Test
    void anAddIsPlannedWithoutTouchingTheRepository() {
        givenEmployeeWithSeries(line("CC_ADMIN", 60, JAN_1, null), line("CC_HR", 40, JAN_1, null));

        CostCenterDistributionPlan plan = service.plan(command(TimelineOperation.ADD, null, FEB_1, null));

        assertTrue(plan.isAccepted());
        assertEquals(new CostCenterDistributionPeriod(JAN_1, JAN_31), plan.adjustedOccurrence().after());
        assertEquals(
                List.of(new CostCenterDistributionPeriod(JAN_1, JAN_31), new CostCenterDistributionPeriod(FEB_1, null)),
                plan.projected()
        );
        verify(costCenterRepository, never()).saveAll(any());
        verify(costCenterRepository, never()).adjustWindowEndDate(any(), any(), any());
        verify(costCenterRepository, never()).deleteAllForWindow(any(), any());
    }

    @Test
    void anAddOnAnExistingStartDateComesBackAsTheCorrectionOfThatWindowRejectedAsSuch() {
        givenEmployeeWithSeries(line("CC_ADMIN", 60, JAN_1, null), line("CC_HR", 40, JAN_1, null));

        CostCenterDistributionPlan plan = service.plan(command(TimelineOperation.ADD, null, JAN_1, LocalDate.of(2026, 1, 15)));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(new CostCenterDistributionPeriod(JAN_1, null), plan.correctedOccurrence());
        assertEquals(List.of(new CostCenterDistributionPeriod(JAN_1, LocalDate.of(2026, 1, 15))), plan.projected());
    }

    @Test
    void aRemovalIsPlannedByTheStartDateOfTheWindow() {
        CostCenterAllocation second = line("CC_HR", 100, FEB_1, null);
        givenEmployeeWithSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), second);
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, FEB_1)).thenReturn(List.of(second));

        CostCenterDistributionPlan plan = service.plan(command(TimelineOperation.REMOVE, FEB_1, null, null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.REMOVE, plan.operation());
        assertEquals(new CostCenterDistributionPeriod(JAN_1, null), plan.adjustedOccurrence().after());
        assertEquals(List.of(new CostCenterDistributionPeriod(JAN_1, null)), plan.projected());
        verify(costCenterRepository, never()).deleteAllForWindow(any(), any());
    }

    @Test
    void aCorrectionThatLeavesAGapComesBackRejectedNamingTheGap() {
        CostCenterAllocation second = line("CC_HR", 100, FEB_1, null);
        givenEmployeeWithSeries(line("CC_ADMIN", 100, JAN_1, JAN_31), second);
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, FEB_1)).thenReturn(List.of(second));

        CostCenterDistributionPlan plan = service.plan(command(TimelineOperation.CORRECT, FEB_1, LocalDate.of(2026, 3, 1), null));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertEquals(List.of(new CostCenterDistributionPeriod(FEB_1, LocalDate.of(2026, 2, 28))), plan.gaps());
        verify(costCenterRepository, never()).saveAll(any());
    }

    @Test
    void rejectsARemovalWithoutAWindowStartDate() {
        whenEmployeeExists();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.plan(command(TimelineOperation.REMOVE, null, null, null))
        );
    }

    @Test
    void rejectsAnAddWithoutAStartDate() {
        whenEmployeeExists();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.plan(command(TimelineOperation.ADD, null, null, null))
        );
    }

    @Test
    void rejectsAnUnknownWindow() {
        whenEmployeeExists();
        when(costCenterRepository.findByEmployeeIdAndStartDate(EMPLOYEE_ID, FEB_1)).thenReturn(List.of());

        assertThrows(
                CostCenterDistributionNotFoundException.class,
                () -> service.plan(command(TimelineOperation.REMOVE, FEB_1, null, null))
        );
    }

    @Test
    void rejectsAnUnknownEmployee() {
        when(employeeCostCenterLookupPort.findByBusinessKey(RSC, ETC, EN)).thenReturn(Optional.empty());

        assertThrows(
                CostCenterEmployeeNotFoundException.class,
                () -> service.plan(command(TimelineOperation.ADD, null, FEB_1, null))
        );
    }

    private PlanCostCenterDistributionChangeCommand command(
            TimelineOperation operation,
            LocalDate windowStartDate,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new PlanCostCenterDistributionChangeCommand(RSC, ETC, EN, operation, windowStartDate, startDate, endDate);
    }

    private void givenEmployeeWithSeries(CostCenterAllocation... lines) {
        whenEmployeeExists();
        when(costCenterRepository.findByEmployeeIdOrderByStartDate(EMPLOYEE_ID)).thenReturn(List.of(lines));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of(new PresencePeriod(JAN_1, null)));
    }

    private void whenEmployeeExists() {
        when(employeeCostCenterLookupPort.findByBusinessKey(RSC, ETC, EN))
                .thenReturn(Optional.of(new EmployeeCostCenterContext(EMPLOYEE_ID, RSC, ETC, EN)));
    }

    private static CostCenterAllocation line(String code, int percentage, LocalDate startDate, LocalDate endDate) {
        return new CostCenterAllocation(EMPLOYEE_ID, code, BigDecimal.valueOf(percentage), startDate, endDate);
    }
}
