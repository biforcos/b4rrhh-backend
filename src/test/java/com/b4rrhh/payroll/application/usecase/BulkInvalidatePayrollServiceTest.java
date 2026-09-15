package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.PayrollLaunchEmployeeContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceLookupPort;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollConcept;
import com.b4rrhh.payroll.domain.model.PayrollContextSnapshot;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkInvalidatePayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;
    @Mock
    private PayrollLaunchPresenceLookupPort payrollLaunchPresenceLookupPort;

    private BulkInvalidatePayrollService service;

    @BeforeEach
    void setUp() {
        service = new BulkInvalidatePayrollService(
                payrollRepository, new PayrollBulkTargetExpander(payrollLaunchPresenceLookupPort));
    }

    // --- A: SINGLE_EMPLOYEE, existing CALCULATED -> totalInvalidated = 1 ---

    @Test
    void singleEmployee_calculatedPayroll_isInvalidated() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(presence("INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll(1L, "INTERNAL", "EMP001", 1, PayrollStatus.CALCULATED)));
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(inv -> inv.getArgument(0));

        BulkInvalidatePayrollResult result = service.invalidateBulk(command(
                singleEmployeeSelection("INTERNAL", "EMP001")
        ));

        assertEquals(1, result.totalCandidates());
        assertEquals(1, result.totalFound());
        assertEquals(1, result.totalInvalidated());
        assertEquals(0, result.totalSkippedAlreadyNotValid());
        assertEquals(0, result.totalSkippedProtected());
        assertEquals(0, result.totalSkippedNotFound());

        ArgumentCaptor<Payroll> captor = ArgumentCaptor.forClass(Payroll.class);
        verify(payrollRepository).save(captor.capture());
        assertEquals(PayrollStatus.NOT_VALID, captor.getValue().getStatus());
        assertEquals("BULK_RESET", captor.getValue().getStatusReasonCode());
    }

    // --- B: EMPLOYEE_LIST, multiple payrolls invalidated, totalCandidates reflects expanded units ---

    @Test
    void employeeList_multiplePresences_allCalculated_allInvalidated() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(presence("INTERNAL", "EMP001", 1), presence("INTERNAL", "EMP001", 2)));
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP002"), any(), any()))
                .thenReturn(List.of(presence("INTERNAL", "EMP002", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll(1L, "INTERNAL", "EMP001", 1, PayrollStatus.CALCULATED)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 2))
                .thenReturn(Optional.of(payroll(2L, "INTERNAL", "EMP001", 2, PayrollStatus.CALCULATED)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP002", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll(3L, "INTERNAL", "EMP002", 1, PayrollStatus.CALCULATED)));
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(inv -> inv.getArgument(0));

        BulkInvalidatePayrollResult result = service.invalidateBulk(command(
                employeeListSelection(List.of(
                        new PayrollLaunchEmployeeTarget("INTERNAL", "EMP001"),
                        new PayrollLaunchEmployeeTarget("INTERNAL", "EMP002")
                ))
        ));

        assertEquals(3, result.totalCandidates());
        assertEquals(3, result.totalFound());
        assertEquals(3, result.totalInvalidated());
        assertEquals(0, result.totalSkippedAlreadyNotValid());
        assertEquals(0, result.totalSkippedProtected());
        assertEquals(0, result.totalSkippedNotFound());
    }

    // --- C: ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD resolves employees and expands by presence ---

    @Test
    void allEmployeesWithPresenceInPeriod_resolvesEmployeesAndInvalidates() {
        when(payrollLaunchPresenceLookupPort.findEmployeesWithPresenceInPeriod(any(), any(), any()))
                .thenReturn(List.of(
                        new PayrollLaunchEmployeeContext("INTERNAL", "EMP010"),
                        new PayrollLaunchEmployeeContext("INTERNAL", "EMP011")
                ));
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP010"), any(), any()))
                .thenReturn(List.of(presence("INTERNAL", "EMP010", 1)));
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP011"), any(), any()))
                .thenReturn(List.of(presence("INTERNAL", "EMP011", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP010", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll(10L, "INTERNAL", "EMP010", 1, PayrollStatus.CALCULATED)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP011", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll(11L, "INTERNAL", "EMP011", 1, PayrollStatus.CALCULATED)));
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(inv -> inv.getArgument(0));

        BulkInvalidatePayrollResult result = service.invalidateBulk(command(
                allEmployeesWithPresenceSelection()
        ));

        assertEquals(2, result.totalCandidates());
        assertEquals(2, result.totalFound());
        assertEquals(2, result.totalInvalidated());
        assertEquals("ESP", result.ruleSystemCode());
        assertEquals("202501", result.payrollPeriodCode());
    }

    // --- D: already NOT_VALID payroll -> skipped, totalSkippedAlreadyNotValid increments ---

    @Test
    void alreadyNotValidPayroll_isSkipped() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(presence("INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll(1L, "INTERNAL", "EMP001", 1, PayrollStatus.NOT_VALID)));

        BulkInvalidatePayrollResult result = service.invalidateBulk(command(
                singleEmployeeSelection("INTERNAL", "EMP001")
        ));

        assertEquals(1, result.totalCandidates());
        assertEquals(1, result.totalFound());
        assertEquals(0, result.totalInvalidated());
        assertEquals(1, result.totalSkippedAlreadyNotValid());
        assertEquals(0, result.totalSkippedProtected());
        assertEquals(0, result.totalSkippedNotFound());
        verify(payrollRepository, never()).save(any());
    }

    // --- E: EXPLICIT_VALIDATED payroll -> protected, totalSkippedProtected increments ---

    @Test
    void explicitValidatedPayroll_isProtectedAndSkipped() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(presence("INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll(1L, "INTERNAL", "EMP001", 1, PayrollStatus.EXPLICIT_VALIDATED)));

        BulkInvalidatePayrollResult result = service.invalidateBulk(command(
                singleEmployeeSelection("INTERNAL", "EMP001")
        ));

        assertEquals(1, result.totalCandidates());
        assertEquals(1, result.totalFound());
        assertEquals(0, result.totalInvalidated());
        assertEquals(0, result.totalSkippedAlreadyNotValid());
        assertEquals(1, result.totalSkippedProtected());
        assertEquals(0, result.totalSkippedNotFound());
        verify(payrollRepository, never()).save(any());
    }

    // --- F: DEFINITIVE payroll -> protected, totalSkippedProtected increments ---

    @Test
    void definitivePayroll_isProtectedAndSkipped() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(presence("INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll(1L, "INTERNAL", "EMP001", 1, PayrollStatus.DEFINITIVE)));

        BulkInvalidatePayrollResult result = service.invalidateBulk(command(
                singleEmployeeSelection("INTERNAL", "EMP001")
        ));

        assertEquals(0, result.totalInvalidated());
        assertEquals(0, result.totalSkippedAlreadyNotValid());
        assertEquals(1, result.totalSkippedProtected());
        verify(payrollRepository, never()).save(any());
    }

    // --- G: payroll not found for candidate unit -> totalSkippedNotFound increments ---

    @Test
    void payrollNotFoundForCandidateUnit_countsSkippedNotFound() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(presence("INTERNAL", "EMP001", 1)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.empty());

        BulkInvalidatePayrollResult result = service.invalidateBulk(command(
                singleEmployeeSelection("INTERNAL", "EMP001")
        ));

        assertEquals(1, result.totalCandidates());
        assertEquals(0, result.totalFound());
        assertEquals(0, result.totalInvalidated());
        assertEquals(0, result.totalSkippedAlreadyNotValid());
        assertEquals(0, result.totalSkippedProtected());
        assertEquals(1, result.totalSkippedNotFound());
        verify(payrollRepository, never()).save(any());
    }

    // --- mixed: employee with no presences does not contribute to totalCandidates ---

    @Test
    void employeeWithNoPresences_doesNotContributeToCandidates() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of());

        BulkInvalidatePayrollResult result = service.invalidateBulk(command(
                singleEmployeeSelection("INTERNAL", "EMP001")
        ));

        assertEquals(0, result.totalCandidates());
        assertEquals(0, result.totalFound());
        assertEquals(0, result.totalInvalidated());
        verify(payrollRepository, never()).findByBusinessKey(any(), any(), any(), any(), any(), any());
        verify(payrollRepository, never()).save(any());
    }

    // --- helpers ---

    private BulkInvalidatePayrollCommand command(PayrollLaunchTargetSelection targetSelection) {
        return new BulkInvalidatePayrollCommand("ESP", "202501", "NORMAL", "BULK_RESET", targetSelection);
    }

    private PayrollLaunchTargetSelection singleEmployeeSelection(String employeeTypeCode, String employeeNumber) {
        return new PayrollLaunchTargetSelection(
                PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                new PayrollLaunchEmployeeTarget(employeeTypeCode, employeeNumber),
                null
        );
    }

    private PayrollLaunchTargetSelection employeeListSelection(List<PayrollLaunchEmployeeTarget> employees) {
        return new PayrollLaunchTargetSelection(
                PayrollLaunchTargetSelectionType.EMPLOYEE_LIST,
                null,
                employees
        );
    }

    private PayrollLaunchTargetSelection allEmployeesWithPresenceSelection() {
        return new PayrollLaunchTargetSelection(
                PayrollLaunchTargetSelectionType.ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD,
                null,
                null
        );
    }

    private PayrollLaunchPresenceContext presence(String employeeTypeCode, String employeeNumber, int presenceNumber) {
        return new PayrollLaunchPresenceContext("ESP", employeeTypeCode, employeeNumber, presenceNumber);
    }

    private Payroll payroll(Long id, String employeeTypeCode, String employeeNumber, int presenceNumber, PayrollStatus status) {
        return Payroll.rehydrate(
                id,
                "ESP",
                employeeTypeCode,
                employeeNumber,
                "202501",
                "NORMAL",
                presenceNumber,
                status,
                null,
                LocalDateTime.of(2026, 1, 31, 10, 0),
                "ENGINE",
                "1.0",
                List.of(new PayrollConcept(1, "BASE", "Base salary", new BigDecimal("1000.00"), null, null, "EARNING", "202501", 1)),
                List.of(new PayrollContextSnapshot("PRESENCE", "EMPLOYEE", "{\"presenceNumber\":1}", "{\"companyCode\":\"ES01\"}")),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}

