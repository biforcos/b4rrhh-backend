package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.PayrollEmployeePresenceContext;
import com.b4rrhh.payroll.application.port.PayrollEmployeePresenceLookupPort;
import com.b4rrhh.payroll.domain.exception.PayrollEmployeePresenceNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollRecalculationNotAllowedException;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollConcept;
import com.b4rrhh.payroll.domain.model.PayrollContextSnapshot;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.model.PayrollWarning;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculatePayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;
    @Mock
    private PayrollEmployeePresenceLookupPort payrollEmployeePresenceLookupPort;

    private CalculatePayrollService service;

    @BeforeEach
    void setUp() {
        service = new CalculatePayrollService(payrollRepository, payrollEmployeePresenceLookupPort);
    }

    @Test
    void calculatesPayrollWhenPresenceExistsAndNoCurrentPayrollExists() {
        CalculatePayrollCommand command = command(PayrollStatus.CALCULATED);
        when(payrollEmployeePresenceLookupPort.findByBusinessKeyForUpdate("ESP", "INTERNAL", "EMP001", 1))
                .thenReturn(Optional.of(new PayrollEmployeePresenceContext(10L, 20L, "ESP", "INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.empty());
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payroll payroll = service.calculate(command);

        assertEquals(PayrollStatus.CALCULATED, payroll.getStatus());
        assertEquals(7L, payroll.getRunId());
        verify(payrollRepository).save(any(Payroll.class));
        verify(payrollRepository, never()).deleteById(any());
        verify(payrollRepository, never()).flush();
    }

    @Test
    void recalculatesNotValidPayrollUsingDeleteFlushSaveSequence() {
        CalculatePayrollCommand command = command(PayrollStatus.CALCULATED);
        Payroll existing = Payroll.rehydrate(
                7L,
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                1,
                PayrollStatus.NOT_VALID,
                "USER_INVALIDATED",
                LocalDateTime.of(2026, 1, 31, 10, 15),
                "ENGINE",
                "1.0",
                command.concepts(),
                command.contextSnapshots(),
                LocalDateTime.of(2026, 1, 31, 10, 15),
                LocalDateTime.of(2026, 1, 31, 10, 15)
        );

        when(payrollEmployeePresenceLookupPort.findByBusinessKeyForUpdate("ESP", "INTERNAL", "EMP001", 1))
                .thenReturn(Optional.of(new PayrollEmployeePresenceContext(10L, 20L, "ESP", "INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(existing));
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.calculate(command);

        InOrder inOrder = inOrder(payrollRepository);
        inOrder.verify(payrollRepository).findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1);
        inOrder.verify(payrollRepository).deleteById(7L);
        inOrder.verify(payrollRepository).flush();
        inOrder.verify(payrollRepository).save(any(Payroll.class));
    }

    @Test
    void rejectsRecalculationWhenCurrentPayrollCannotBeRecalculated() {
        CalculatePayrollCommand command = command(PayrollStatus.CALCULATED);
        Payroll existing = Payroll.rehydrate(
                7L,
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                1,
                PayrollStatus.CALCULATED,
                null,
                LocalDateTime.of(2026, 1, 31, 10, 15),
                "ENGINE",
                "1.0",
                command.concepts(),
                command.contextSnapshots(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(payrollEmployeePresenceLookupPort.findByBusinessKeyForUpdate("ESP", "INTERNAL", "EMP001", 1))
                .thenReturn(Optional.of(new PayrollEmployeePresenceContext(10L, 20L, "ESP", "INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(existing));

        assertThrows(PayrollRecalculationNotAllowedException.class, () -> service.calculate(command));
        verify(payrollRepository, never()).deleteById(any());
        verify(payrollRepository, never()).flush();
        verify(payrollRepository, never()).save(any());
    }

    @Test
    void rejectsCalculateWhenPresenceDoesNotExist() {
        when(payrollEmployeePresenceLookupPort.findByBusinessKeyForUpdate("ESP", "INTERNAL", "EMP001", 1))
                .thenReturn(Optional.empty());

        assertThrows(PayrollEmployeePresenceNotFoundException.class, () -> service.calculate(command(PayrollStatus.CALCULATED)));
    }

    private CalculatePayrollCommand command(PayrollStatus status) {
        return new CalculatePayrollCommand(
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                1,
                status,
                status == PayrollStatus.NOT_VALID ? "ENGINE_INVALID" : null,
                LocalDateTime.of(2026, 1, 31, 10, 15),
                "ENGINE",
                "1.0",
                7L,
                                List.of(new PayrollWarning(null, null, "DETERMINISTIC_FAKE_PAYROLL", "INFO", "Payroll generated by deterministic fake calculator", "{\"mode\":\"DETERMINISTIC_FAKE\"}")),
                List.of(new PayrollConcept(1, "BASE", "Base salary", new BigDecimal("1000.00"), null, null, "EARNING", "202501", 1)),
                List.of(new PayrollContextSnapshot("PRESENCE", "EMPLOYEE", "{\"presenceNumber\":1}", "{\"companyCode\":\"ES01\"}")),
                List.of()
        );
    }
}
