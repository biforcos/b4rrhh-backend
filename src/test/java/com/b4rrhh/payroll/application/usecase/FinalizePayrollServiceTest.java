package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.document.application.service.PayslipDocumentArchiver;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinalizePayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;
    @Mock
    private PayslipDocumentArchiver payslipDocumentArchiver;

    private FinalizePayrollService service;

    @BeforeEach
    void setUp() {
        service = new FinalizePayrollService(payrollRepository, payslipDocumentArchiver);
    }

    @Test
    void finalizesExistingPayrollPreservingIdentity() {
        Payroll existing = Payroll.rehydrate(
                7L,
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                1,
                PayrollStatus.EXPLICIT_VALIDATED,
                null,
                LocalDateTime.of(2026, 1, 31, 10, 15),
                "ENGINE",
                "1.0",
                List.of(),
                List.of(),
                LocalDateTime.of(2026, 1, 31, 10, 15),
                LocalDateTime.of(2026, 1, 31, 10, 15)
        );

        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(existing));
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payroll definitive = service.finalizePayroll(new FinalizePayrollCommand("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1));

        assertEquals(7L, definitive.getId());
        assertEquals(PayrollStatus.DEFINITIVE, definitive.getStatus());
        verify(payrollRepository).save(any(Payroll.class));
        verify(payrollRepository, never()).deleteById(any());
    }
}
