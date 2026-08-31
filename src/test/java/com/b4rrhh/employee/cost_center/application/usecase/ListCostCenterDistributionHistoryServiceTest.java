package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterEmployeeNotFoundException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCostCenterDistributionHistoryServiceTest {

    private static final String RSC = "ESP";
    private static final String ETC = "INTERNAL";
    private static final String EN = "EMP001";
    private static final Long EMPLOYEE_ID = 10L;

    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private EmployeeCostCenterLookupPort employeeCostCenterLookupPort;

    private ListCostCenterDistributionHistoryService service;

    @BeforeEach
    void setUp() {
        service = new ListCostCenterDistributionHistoryService(
                costCenterRepository,
                employeeCostCenterLookupPort,
                new CostCenterDistributionWindowGrouper()
        );
    }

    // Test 12: history groups rows by window
    @Test
    void groupsAllocationsIntoWindows() {
        givenEmployeeFound();

        LocalDate start1 = LocalDate.of(2026, 1, 1);
        LocalDate end1 = LocalDate.of(2026, 3, 31);
        LocalDate start2 = LocalDate.of(2026, 4, 1);

        CostCenterAllocation w1Line = new CostCenterAllocation(
                EMPLOYEE_ID, "CC_A", new BigDecimal("100"), start1, end1
        );
        CostCenterAllocation w2LineA = new CostCenterAllocation(
                EMPLOYEE_ID, "CC_B", new BigDecimal("60"), start2, null
        );
        CostCenterAllocation w2LineB = new CostCenterAllocation(
                EMPLOYEE_ID, "CC_C", new BigDecimal("40"), start2, null
        );

        when(costCenterRepository.findByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of(w1Line, w2LineA, w2LineB));

        CostCenterDistributionReadModel.History result = service.listHistory(query());

        assertNotNull(result);
        assertEquals(RSC, result.ruleSystemCode());
        assertEquals(2, result.windows().size());

        CostCenterDistributionReadModel.Window window1 = result.windows().get(0);
        assertEquals(start1, window1.startDate());
        assertEquals(end1, window1.endDate());
        assertEquals(1, window1.items().size());
        assertEquals("CC_A", window1.items().get(0).costCenterCode());

        CostCenterDistributionReadModel.Window window2 = result.windows().get(1);
        assertEquals(start2, window2.startDate());
        assertNull(window2.endDate());
        assertEquals(2, window2.items().size());
        assertEquals(new BigDecimal("100"), window2.totalAllocationPercentage());

        CostCenterDistributionReadModel.Item itemB = window2.items().stream()
                .filter(i -> "CC_B".equals(i.costCenterCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("60"), itemB.allocationPercentage());

        CostCenterDistributionReadModel.Item itemC = window2.items().stream()
                .filter(i -> "CC_C".equals(i.costCenterCode())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("40"), itemC.allocationPercentage());
    }

    @Test
    void returnsEmptyHistoryWhenNoAllocations() {
        givenEmployeeFound();
        when(costCenterRepository.findByEmployeeIdOrderByStartDate(EMPLOYEE_ID))
                .thenReturn(List.of());

        CostCenterDistributionReadModel.History result = service.listHistory(query());

        assertNotNull(result);
        assertEquals(0, result.windows().size());
    }

    @Test
    void rejectsWhenEmployeeNotFound() {
        when(employeeCostCenterLookupPort.findByBusinessKey(RSC, ETC, EN))
                .thenReturn(Optional.empty());

        assertThrows(CostCenterEmployeeNotFoundException.class, () ->
                service.listHistory(query())
        );
    }

    // helpers

    private void givenEmployeeFound() {
        when(employeeCostCenterLookupPort.findByBusinessKey(RSC, ETC, EN))
                .thenReturn(Optional.of(new EmployeeCostCenterContext(EMPLOYEE_ID, RSC, ETC, EN)));
    }

    private ListCostCenterDistributionHistoryQuery query() {
        return new ListCostCenterDistributionHistoryQuery(RSC, ETC, EN);
    }
}
