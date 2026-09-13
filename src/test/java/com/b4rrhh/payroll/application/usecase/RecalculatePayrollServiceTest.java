package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.exception.PayrollNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollRecalculationNotAllowedException;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll_engine.metamodel.domain.port.RuleSystemMetamodelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecalculatePayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;
    @Mock
    private CalculatePayrollUnitUseCase calculatePayrollUnitUseCase;
    @Mock
    private RuleSystemMetamodelRepository ruleSystemMetamodelRepository;

    private RecalculatePayrollService service;

    @BeforeEach
    void setUp() {
        service = new RecalculatePayrollService(
                payrollRepository, calculatePayrollUnitUseCase, ruleSystemMetamodelRepository);
    }

    @Test
    void delegatesToCalculateUnitWhenPayrollIsNotValid() {
        RecalculatePayrollCommand command = command("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1);
        Payroll notValidPayroll = payroll("MAS000001", "202604", PayrollStatus.NOT_VALID, "ENGINE_001", "1.0");
        Payroll recalculated = payroll("MAS000001", "202604", PayrollStatus.CALCULATED, "ENGINE_001", "1.0");

        when(payrollRepository.findByBusinessKey("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1))
                .thenReturn(Optional.of(notValidPayroll));
        when(calculatePayrollUnitUseCase.calculate(any())).thenReturn(recalculated);

        Payroll result = service.recalculate(command);

        assertEquals(PayrollStatus.CALCULATED, result.getStatus());

        ArgumentCaptor<CalculatePayrollUnitCommand> captor = ArgumentCaptor.forClass(CalculatePayrollUnitCommand.class);
        verify(calculatePayrollUnitUseCase).calculate(captor.capture());
        CalculatePayrollUnitCommand sent = captor.getValue();
        assertEquals("MAS000001", sent.employeeNumber());
        assertEquals("202604", sent.payrollPeriodCode());
        assertEquals("ENGINE_001", sent.calculationEngineCode());
        assertEquals("1.0", sent.calculationEngineVersion());
    }

    @Test
    void throwsWhenPayrollNotFound() {
        when(payrollRepository.findByBusinessKey(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(PayrollNotFoundException.class, () ->
                service.recalculate(command("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1)));
    }

    @Test
    void throwsWhenPayrollIsNotInNotValidState() {
        when(payrollRepository.findByBusinessKey("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1))
                .thenReturn(Optional.of(payroll("MAS000001", "202604", PayrollStatus.CALCULATED, "ENGINE_001", "1.0")));

        assertThrows(PayrollRecalculationNotAllowedException.class, () ->
                service.recalculate(command("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1)));
    }

    @Test
    void derivesPeriodDatesFrom6DigitPeriodCode() {
        when(payrollRepository.findByBusinessKey(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(payroll("MAS000001", "202604", PayrollStatus.NOT_VALID, "ENG", "1")));
        when(calculatePayrollUnitUseCase.calculate(any())).thenReturn(
                payroll("MAS000001", "202604", PayrollStatus.CALCULATED, "ENG", "1"));

        service.recalculate(command("MAS", "EMP", "MAS000001", "202604", "NORMAL", 1));

        ArgumentCaptor<CalculatePayrollUnitCommand> captor = ArgumentCaptor.forClass(CalculatePayrollUnitCommand.class);
        verify(calculatePayrollUnitUseCase).calculate(captor.capture());
        assertEquals(1, captor.getValue().periodStart().getDayOfMonth());
        assertEquals(4, captor.getValue().periodStart().getMonthValue());
        assertEquals(2026, captor.getValue().periodStart().getYear());
        assertEquals(30, captor.getValue().periodEnd().getDayOfMonth());
    }

    private RecalculatePayrollCommand command(String rsc, String etc, String en, String ppc, String ptc, int pn) {
        return new RecalculatePayrollCommand(rsc, etc, en, ppc, ptc, pn);
    }

    private Payroll payroll(String employeeNumber, String periodCode, PayrollStatus status, String engCode, String engVer) {
        // id, ruleSystemCode, employeeTypeCode, employeeNumber, periodCode, payrollTypeCode, presenceNumber,
        // status, statusReasonCode (null=none), calculatedAt, engCode, engVer, warnings, concepts, contextSnapshots, createdAt, updatedAt
        return Payroll.rehydrate(
                1L, "MAS", "EMP", employeeNumber, periodCode, "NORMAL", 1,
                status, null, LocalDateTime.now(), engCode, engVer,
                List.of(), List.of(), List.of(),
                LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
