package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterEmployeeNotFoundException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCurrentCostCenterDistributionServiceTest {

    private static final String RSC = "ESP";
    private static final String ETC = "INTERNAL";
    private static final String EN = "EMP001";
    private static final Long EMPLOYEE_ID = 10L;
    private static final LocalDate WINDOW_START = LocalDate.of(2026, 1, 1);

    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private EmployeeCostCenterLookupPort employeeCostCenterLookupPort;

    private GetCurrentCostCenterDistributionService service;

    @BeforeEach
    void setUp() {
        service = new GetCurrentCostCenterDistributionService(
                costCenterRepository,
                employeeCostCenterLookupPort
        );
    }

    // Test 11: current distribution returns grouped response
    @Test
    void returnsCurrentDistributionGroupedByWindow() {
        givenEmployeeFound();
        CostCenterAllocation lineA = new CostCenterAllocation(
                EMPLOYEE_ID, "CC_A", new BigDecimal("60"), WINDOW_START, null
        );
        CostCenterAllocation lineB = new CostCenterAllocation(
                EMPLOYEE_ID, "CC_B", new BigDecimal("40"), WINDOW_START, null
        );
        when(costCenterRepository.findActiveAtDate(any(), any()))
                .thenReturn(List.of(lineA, lineB));

        CostCenterDistributionReadModel.CurrentDistribution result = service.getCurrent(query());

        assertNotNull(result);
        assertEquals(RSC, result.ruleSystemCode());
        assertEquals(ETC, result.employeeTypeCode());
        assertEquals(EN, result.employeeNumber());

        CostCenterDistributionReadModel.Window window = result.currentDistribution();
        assertNotNull(window);
        assertEquals(WINDOW_START, window.startDate());
        assertNull(window.endDate());
        assertEquals(2, window.items().size());
        assertEquals(new BigDecimal("100"), window.totalAllocationPercentage());

        CostCenterDistributionReadModel.Item itemA = window.items().stream()
                .filter(i -> "CC_A".equals(i.costCenterCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("60"), itemA.allocationPercentage());

        CostCenterDistributionReadModel.Item itemB = window.items().stream()
                .filter(i -> "CC_B".equals(i.costCenterCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("40"), itemB.allocationPercentage());
    }

    @Test
    void returnsNullCurrentDistributionWhenNoActiveLines() {
        givenEmployeeFound();
        when(costCenterRepository.findActiveAtDate(any(), any())).thenReturn(List.of());

        CostCenterDistributionReadModel.CurrentDistribution result = service.getCurrent(query());

        assertNotNull(result);
        assertNull(result.currentDistribution());
    }

    @Test
    void rejectsWhenEmployeeNotFound() {
        when(employeeCostCenterLookupPort.findByBusinessKey(RSC, ETC, EN))
                .thenReturn(Optional.empty());

        assertThrows(CostCenterEmployeeNotFoundException.class, () ->
                service.getCurrent(query())
        );
    }

    // helpers

    private void givenEmployeeFound() {
        when(employeeCostCenterLookupPort.findByBusinessKey(RSC, ETC, EN))
                .thenReturn(Optional.of(new EmployeeCostCenterContext(EMPLOYEE_ID, RSC, ETC, EN)));
    }

    private GetCurrentCostCenterDistributionQuery query() {
        return new GetCurrentCostCenterDistributionQuery(RSC, ETC, EN);
    }
}
