package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchPayrollsServiceTest {

    @Mock
    private PayrollRepository payrollRepository;

    private SearchPayrollsService service;

    @BeforeEach
    void setUp() {
        service = new SearchPayrollsService(payrollRepository);
    }

    @Test
    void returnsMatchingPayrollsForPeriodFilter() {
        Payroll payroll = minimalPayroll("MAS000001", "202604", PayrollStatus.CALCULATED);
        when(payrollRepository.findByFilters(eq(null), eq("202604"), eq(null), eq(null)))
                .thenReturn(List.of(payroll));

        List<Payroll> result = service.search(new SearchPayrollsQuery(null, "202604", null, null));

        assertEquals(1, result.size());
        assertEquals("MAS000001", result.get(0).getEmployeeNumber());
    }

    @Test
    void returnsEmptyListWhenNoMatch() {
        when(payrollRepository.findByFilters(eq(null), eq("202605"), eq(null), eq(null)))
                .thenReturn(List.of());

        List<Payroll> result = service.search(new SearchPayrollsQuery(null, "202605", null, null));

        assertEquals(0, result.size());
    }

    @Test
    void passesAllFiltersToRepository() {
        when(payrollRepository.findByFilters(eq("MAS"), eq("202604"), eq("MAS000001"), eq(PayrollStatus.CALCULATED)))
                .thenReturn(List.of());

        service.search(new SearchPayrollsQuery("MAS", "202604", "MAS000001", PayrollStatus.CALCULATED));

        verify(payrollRepository).findByFilters("MAS", "202604", "MAS000001", PayrollStatus.CALCULATED);
    }

    private Payroll minimalPayroll(String employeeNumber, String periodCode, PayrollStatus status) {
        return Payroll.rehydrate(
                1L, "MAS", "EMP", employeeNumber, periodCode, "NORMAL", 1,
                status, null,
                Instant.now(), "ENGINE_001", "1.0",
                List.of(), List.of(), List.of(),
                LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
