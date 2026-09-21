package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceLookupPort;
import com.b4rrhh.payroll.document.application.service.PayslipDocumentArchiver;
import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El tercer verbo, contado (backend#102).
 *
 * <p>Los contadores son el entregable y no el adorno, asi que lo que estos tests miran es que cada
 * estado caiga en su cajon y que ninguno acabe en un «fallidas» generico. El caso que vale es el
 * inválido: {@code NOT_VALID → DEFINITIVE} no existe, y el contador que lo dice es lo que hace que
 * la pantalla ensene la maquina de estados en vez de explicarla.
 */
@ExtendWith(MockitoExtension.class)
class BulkFinalizePayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;
    @Mock
    private PayrollLaunchPresenceLookupPort payrollLaunchPresenceLookupPort;
    @Mock
    private PayslipDocumentArchiver payslipDocumentArchiver;

    private BulkFinalizePayrollService service;

    @BeforeEach
    void setUp() {
        service = new BulkFinalizePayrollService(
                payrollRepository,
                new PayrollBulkTargetExpander(payrollLaunchPresenceLookupPort),
                payslipDocumentArchiver);
    }

    @Test
    void closesACalculatedPayroll() {
        unEmpleadoConUnaPresencia("EMP001");
        conRecibo("EMP001", PayrollStatus.CALCULATED);
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(inv -> inv.getArgument(0));

        BulkFinalizePayrollResult result = service.finalizeBulk(command(singleEmployee("EMP001")));

        assertEquals(1, result.totalCandidates());
        assertEquals(1, result.totalFound());
        assertEquals(1, result.totalFinalized());
        assertEquals(0, result.totalSkippedAlreadyDefinitive());
        assertEquals(0, result.totalSkippedNotEligibleByStatus());
        assertEquals(0, result.totalSkippedNotFound());
    }

    /** Validar es un paso intermedio, no una alternativa a cerrar: desde ahi tambien se cierra. */
    @Test
    void closesAnExplicitlyValidatedPayrollToo() {
        unEmpleadoConUnaPresencia("EMP001");
        conRecibo("EMP001", PayrollStatus.EXPLICIT_VALIDATED);
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(inv -> inv.getArgument(0));

        BulkFinalizePayrollResult result = service.finalizeBulk(command(singleEmployee("EMP001")));

        assertEquals(1, result.totalFinalized());
    }

    /**
     * <b>El contador que vale.</b> Un recibo invalido no se cierra, y eso no es un fallo de nada:
     * cerrar dice «esto ya no se toca» y un recibo invalido no tiene nada que no tocar. Si cayera
     * en un cajon de fallidas, cerrar 870 de 873 pareceria una tanda a medias en vez de la maquina
     * de estados funcionando.
     */
    @Test
    void countsANotValidPayrollApart_andDoesNotTouchIt() {
        unEmpleadoConUnaPresencia("EMP001");
        conRecibo("EMP001", PayrollStatus.NOT_VALID);

        BulkFinalizePayrollResult result = service.finalizeBulk(command(singleEmployee("EMP001")));

        assertEquals(1, result.totalFound());
        assertEquals(0, result.totalFinalized());
        assertEquals(1, result.totalSkippedNotEligibleByStatus());
        assertEquals(0, result.totalSkippedAlreadyDefinitive());
        verify(payrollRepository, never()).save(any(Payroll.class));
    }

    /** Repetir el cierre de un periodo no pide nada de nadie, y tiene su propio cajon. */
    @Test
    void countsAnAlreadyClosedPayrollApart() {
        unEmpleadoConUnaPresencia("EMP001");
        conRecibo("EMP001", PayrollStatus.DEFINITIVE);

        BulkFinalizePayrollResult result = service.finalizeBulk(command(singleEmployee("EMP001")));

        assertEquals(1, result.totalSkippedAlreadyDefinitive());
        assertEquals(0, result.totalSkippedNotEligibleByStatus());
        verify(payrollRepository, never()).save(any(Payroll.class));
    }

    /** Una unidad sin recibo no se cuenta como encontrada, y no entra en totalFound. */
    @Test
    void countsAUnitWithNoPayrollApart() {
        unEmpleadoConUnaPresencia("EMP001");
        when(payrollRepository.findByBusinessKey(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        BulkFinalizePayrollResult result = service.finalizeBulk(command(singleEmployee("EMP001")));

        assertEquals(1, result.totalCandidates());
        assertEquals(0, result.totalFound());
        assertEquals(1, result.totalSkippedNotFound());
    }

    /**
     * La particion, que es lo que tiene que cuadrar para que la pantalla no mienta: cada candidata
     * cae en uno y solo uno de los cuatro cajones. Un mes partido —dos presencias del mismo
     * empleado— es ademas el caso en el que candidatas no es el numero de empleados.
     */
    @Test
    void everyCandidateLandsInExactlyOneBucket() {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP001"), any(), any()))
                .thenReturn(List.of(presence("EMP001", 1), presence("EMP001", 2)));
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(eq("ESP"), eq("INTERNAL"), eq("EMP002"), any(), any()))
                .thenReturn(List.of(presence("EMP002", 1), presence("EMP002", 2)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll("EMP001", 1, PayrollStatus.CALCULATED)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 2))
                .thenReturn(Optional.of(payroll("EMP001", 2, PayrollStatus.NOT_VALID)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP002", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll("EMP002", 1, PayrollStatus.DEFINITIVE)));
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", "EMP002", "202501", "NORMAL", 2))
                .thenReturn(Optional.empty());
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(inv -> inv.getArgument(0));

        BulkFinalizePayrollResult r = service.finalizeBulk(command(new PayrollLaunchTargetSelection(
                PayrollLaunchTargetSelectionType.EMPLOYEE_LIST,
                null,
                List.of(new PayrollLaunchEmployeeTarget("INTERNAL", "EMP001"),
                        new PayrollLaunchEmployeeTarget("INTERNAL", "EMP002")))));

        assertEquals(4, r.totalCandidates(), "dos empleados con dos presencias son cuatro unidades");
        assertEquals(1, r.totalFinalized());
        assertEquals(1, r.totalSkippedNotEligibleByStatus());
        assertEquals(1, r.totalSkippedAlreadyDefinitive());
        assertEquals(1, r.totalSkippedNotFound());
        assertEquals(
                r.totalCandidates(),
                r.totalFinalized() + r.totalSkippedNotEligibleByStatus()
                        + r.totalSkippedAlreadyDefinitive() + r.totalSkippedNotFound(),
                "cada candidata tiene que caer en uno y solo uno de los cuatro cajones");
        assertEquals(3, r.totalFound(), "totalFound es una etapa, no un cajon");
    }

    /** El mismo selector que los otros dos verbos: la misma validacion y el mismo mensaje. */
    @Test
    void rejectsAnAllEmployeesSelectionThatAlsoNamesOne() {
        InvalidPayrollArgumentException ex = assertThrows(InvalidPayrollArgumentException.class, () ->
                service.finalizeBulk(command(new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD,
                        new PayrollLaunchEmployeeTarget("INTERNAL", "EMP001"),
                        null))));

        assertEquals(
                "targetSelection.employee and targetSelection.employees must be null for ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD",
                ex.getMessage());
    }

    @Test
    void rejectsAPeriodThatIsNotAMonth() {
        InvalidPayrollArgumentException ex = assertThrows(InvalidPayrollArgumentException.class, () ->
                service.finalizeBulk(new BulkFinalizePayrollCommand(
                        "ESP", "2025-01", "NORMAL", singleEmployee("EMP001"))));

        assertEquals("payrollPeriodCode must be in yyyyMM format, got: 2025-01", ex.getMessage());
    }

    private void unEmpleadoConUnaPresencia(String employeeNumber) {
        when(payrollLaunchPresenceLookupPort.findRelevantPresences(
                eq("ESP"), eq("INTERNAL"), eq(employeeNumber), any(), any()))
                .thenReturn(List.of(presence(employeeNumber, 1)));
    }

    private void conRecibo(String employeeNumber, PayrollStatus status) {
        when(payrollRepository.findByBusinessKey("ESP", "INTERNAL", employeeNumber, "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll(employeeNumber, 1, status)));
    }

    private BulkFinalizePayrollCommand command(PayrollLaunchTargetSelection selection) {
        return new BulkFinalizePayrollCommand("ESP", "202501", "NORMAL", selection);
    }

    private PayrollLaunchTargetSelection singleEmployee(String employeeNumber) {
        return new PayrollLaunchTargetSelection(
                PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                new PayrollLaunchEmployeeTarget("INTERNAL", employeeNumber),
                null);
    }

    private PayrollLaunchPresenceContext presence(String employeeNumber, int presenceNumber) {
        return new PayrollLaunchPresenceContext("ESP", "INTERNAL", employeeNumber, presenceNumber);
    }

    private Payroll payroll(String employeeNumber, int presenceNumber, PayrollStatus status) {
        return Payroll.rehydrate(
                1L, "ESP", "INTERNAL", employeeNumber, "202501", "NORMAL", presenceNumber,
                status, null, Instant.now(), "ENGINE", "1.0",
                List.of(), List.of(), List.of(),
                LocalDateTime.now(), LocalDateTime.now());
    }
}
