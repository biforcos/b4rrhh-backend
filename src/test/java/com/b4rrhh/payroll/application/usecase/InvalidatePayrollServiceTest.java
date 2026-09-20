package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.exception.PayrollInvalidStateTransitionException;
import com.b4rrhh.payroll.domain.exception.PayrollNotFoundException;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollConcept;
import com.b4rrhh.payroll.domain.model.PayrollContextSnapshot;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvalidatePayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;

    private InvalidatePayrollService service;

    @BeforeEach
    void setUp() {
        service = new InvalidatePayrollService(payrollRepository);
    }

    @Test
    void invalidatesExistingPayrollPreservingIdentity() {
        Payroll existing = payroll(PayrollStatus.CALCULATED);
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(existing));
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payroll invalidated = service.invalidate(new InvalidatePayrollCommand(
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                1,
                "USER_INVALIDATED"
        ));

        assertEquals(7L, invalidated.getId());
        assertEquals(PayrollStatus.NOT_VALID, invalidated.getStatus());
        assertEquals("USER_INVALIDATED", invalidated.getStatusReasonCode());
        verify(payrollRepository).save(any(Payroll.class));
        verify(payrollRepository, never()).deleteById(any());
        verify(payrollRepository, never()).flush();
    }

    @Test
    void throwsWhenPayrollDoesNotExist() {
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.empty());

        assertThrows(PayrollNotFoundException.class, () -> service.invalidate(new InvalidatePayrollCommand(
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                1,
                "USER_INVALIDATED"
        )));
    }

    @Test
    void rejectsInvalidateWhenPayrollIsAlreadyDefinitive() {
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll(PayrollStatus.DEFINITIVE)));

        assertThrows(PayrollInvalidStateTransitionException.class, () -> service.invalidate(new InvalidatePayrollCommand(
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                1,
                "USER_INVALIDATED"
        )));
    }

    private Payroll payroll(PayrollStatus status) {
        return Payroll.rehydrate(
                7L,
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                1,
                status,
                null,
                LocalDateTime.of(2026, 1, 31, 10, 15),
                "ENGINE",
                "1.0",
                List.of(new PayrollConcept(1, "BASE", "SALARIO_BASE", "Base salary", new BigDecimal("1000.00"), null, null, "EARNING", "202501", 1)),
                List.of(new PayrollContextSnapshot("PRESENCE", "EMPLOYEE", "{\"presenceNumber\":1}", "{\"companyCode\":\"ES01\"}")),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
