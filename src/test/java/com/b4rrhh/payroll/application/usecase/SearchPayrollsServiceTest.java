package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll.domain.port.PayrollSearchPage;
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
    void returnsTheRepositoryPageWithItsTotal() {
        Payroll payroll = minimalPayroll("MAS000001", "202604", PayrollStatus.CALCULATED);
        when(payrollRepository.findPageByFilters(eq(null), eq("202604"), eq(null), eq(null), eq(0), eq(50)))
                .thenReturn(new PayrollSearchPage(List.of(payroll), 0, 50, 7908));

        PayrollSearchPage result = service.search(new SearchPayrollsQuery(null, "202604", null, null, 0, 50));

        assertEquals(1, result.items().size());
        assertEquals("MAS000001", result.items().get(0).getEmployeeNumber());
        assertEquals(7908, result.total());
    }

    @Test
    void passesAllFiltersAndThePageToRepository() {
        when(payrollRepository.findPageByFilters("MAS", "202604", "MAS000001", PayrollStatus.CALCULATED, 3, 20))
                .thenReturn(new PayrollSearchPage(List.of(), 3, 20, 0));

        service.search(new SearchPayrollsQuery("MAS", "202604", "MAS000001", PayrollStatus.CALCULATED, 3, 20));

        verify(payrollRepository).findPageByFilters("MAS", "202604", "MAS000001", PayrollStatus.CALCULATED, 3, 20);
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
