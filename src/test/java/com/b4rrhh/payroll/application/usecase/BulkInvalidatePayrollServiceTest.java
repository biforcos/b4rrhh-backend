package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.PayrollBulkStatusTransitionPort;
import com.b4rrhh.payroll.application.port.PayrollLaunchEmployeeContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceLookupPort;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkInvalidatePayrollServiceTest {

    @Mock
    private PayrollLaunchPresenceLookupPort payrollLaunchPresenceLookupPort;

    private final Map<String, PayrollStatus> rows = new HashMap<>();
    private final RowsTransitions transitions = new RowsTransitions();

    private BulkInvalidatePayrollService service;

    @BeforeEach
    void setUp() {
        service = new BulkInvalidatePayrollService(
                transitions, new PayrollBulkTargetExpander(payrollLaunchPresenceLookupPort));
    }

    // --- A: SINGLE_EMPLOYEE, existing CALCULATED -> totalInvalidated = 1 ---

    @Test
    void singleEmployee_calculatedPayroll_isInvalidated() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(presence("INTERNAL", "EMP001", 1)));
        rows.put(key("EMP001", 1), PayrollStatus.CALCULATED);

        BulkInvalidatePayrollResult result = service.invalidateBulk(command(
                singleEmployeeSelection("INTERNAL", "EMP001")
        ));

        assertEquals(1, result.totalCandidates());
        assertEquals(1, result.totalFound());
        assertEquals(1, result.totalInvalidated());
        assertEquals(0, result.totalSkippedAlreadyNotValid());
        assertEquals(0, result.totalSkippedProtected());
        assertEquals(0, result.totalSkippedNotFound());

        assertEquals(PayrollStatus.NOT_VALID, rows.get(key("EMP001", 1)));
        assertEquals("BULK_INVALIDATION", transitions.reasonWritten);
    }

    // --- B: EMPLOYEE_LIST, multiple payrolls invalidated, totalCandidates reflects expanded units ---

    @Test
    void employeeList_multiplePresences_allCalculated_allInvalidated() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(presence("INTERNAL", "EMP001", 1), presence("INTERNAL", "EMP001", 2)));
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP002"), any(), any()))
                .thenReturn(List.of(presence("INTERNAL", "EMP002", 1)));
        rows.put(key("EMP001", 1), PayrollStatus.CALCULATED);
        rows.put(key("EMP001", 2), PayrollStatus.CALCULATED);
        rows.put(key("EMP002", 1), PayrollStatus.CALCULATED);

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
        rows.put(key("EMP010", 1), PayrollStatus.CALCULATED);
        rows.put(key("EMP011", 1), PayrollStatus.CALCULATED);

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
        rows.put(key("EMP001", 1), PayrollStatus.NOT_VALID);

        BulkInvalidatePayrollResult result = service.invalidateBulk(command(
                singleEmployeeSelection("INTERNAL", "EMP001")
        ));

        assertEquals(1, result.totalCandidates());
        assertEquals(1, result.totalFound());
        assertEquals(0, result.totalInvalidated());
        assertEquals(1, result.totalSkippedAlreadyNotValid());
        assertEquals(0, result.totalSkippedProtected());
        assertEquals(0, result.totalSkippedNotFound());
        assertEquals(0, transitions.moved);
    }

    // --- E: EXPLICIT_VALIDATED payroll -> protected, totalSkippedProtected increments ---

    @Test
    void explicitValidatedPayroll_isProtectedAndSkipped() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(presence("INTERNAL", "EMP001", 1)));
        rows.put(key("EMP001", 1), PayrollStatus.EXPLICIT_VALIDATED);

        BulkInvalidatePayrollResult result = service.invalidateBulk(command(
                singleEmployeeSelection("INTERNAL", "EMP001")
        ));

        assertEquals(1, result.totalCandidates());
        assertEquals(1, result.totalFound());
        assertEquals(0, result.totalInvalidated());
        assertEquals(0, result.totalSkippedAlreadyNotValid());
        assertEquals(1, result.totalSkippedProtected());
        assertEquals(0, result.totalSkippedNotFound());
        assertEquals(0, transitions.moved);
    }

    // --- F: DEFINITIVE payroll -> protected, totalSkippedProtected increments ---

    @Test
    void definitivePayroll_isProtectedAndSkipped() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(presence("INTERNAL", "EMP001", 1)));
        rows.put(key("EMP001", 1), PayrollStatus.DEFINITIVE);

        BulkInvalidatePayrollResult result = service.invalidateBulk(command(
                singleEmployeeSelection("INTERNAL", "EMP001")
        ));

        assertEquals(0, result.totalInvalidated());
        assertEquals(0, result.totalSkippedAlreadyNotValid());
        assertEquals(1, result.totalSkippedProtected());
        assertEquals(0, transitions.moved);
    }

    // --- G: payroll not found for candidate unit -> totalSkippedNotFound increments ---

    @Test
    void payrollNotFoundForCandidateUnit_countsSkippedNotFound() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(presence("INTERNAL", "EMP001", 1)));

        BulkInvalidatePayrollResult result = service.invalidateBulk(command(
                singleEmployeeSelection("INTERNAL", "EMP001")
        ));

        assertEquals(1, result.totalCandidates());
        assertEquals(0, result.totalFound());
        assertEquals(0, result.totalInvalidated());
        assertEquals(0, result.totalSkippedAlreadyNotValid());
        assertEquals(0, result.totalSkippedProtected());
        assertEquals(1, result.totalSkippedNotFound());
        assertEquals(0, transitions.moved);
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
        assertEquals(0, transitions.moved);
    }

    // --- helpers ---

    private BulkInvalidatePayrollCommand command(PayrollLaunchTargetSelection targetSelection) {
        return new BulkInvalidatePayrollCommand("ESP", "202501", "NORMAL", targetSelection);
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

    private static String key(String employeeNumber, int presenceNumber) {
        return "ESP/INTERNAL/" + employeeNumber + "/202501/NORMAL/" + presenceNumber;
    }

    /** Las filas de payroll.payroll de este test, y la sentencia unica que las cambia. */
    private final class RowsTransitions implements PayrollBulkStatusTransitionPort {

        private int moved;
        private String reasonWritten;

        @Override
        public Map<PayrollStatus, Integer> moveStatus(
                String ruleSystemCode,
                String payrollPeriodCode,
                String payrollTypeCode,
                List<PayrollCalculationUnit> units,
                PayrollStatus from,
                PayrollStatus to,
                String statusReasonCode
        ) {
            Map<PayrollStatus, Integer> before = new EnumMap<>(PayrollStatus.class);
            for (PayrollCalculationUnit unit : units) {
                String k = key(unit.employeeNumber(), unit.presenceNumber());
                PayrollStatus status = rows.get(k);
                if (status == null) {
                    continue;
                }
                before.merge(status, 1, Integer::sum);
                if (status == from) {
                    rows.put(k, to);
                    moved++;
                    reasonWritten = statusReasonCode;
                }
            }
            return before;
        }
    }
}

