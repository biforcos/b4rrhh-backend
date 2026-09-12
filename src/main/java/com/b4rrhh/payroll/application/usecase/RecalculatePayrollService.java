package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.payroll.domain.exception.PayrollNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollRecalculationNotAllowedException;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class RecalculatePayrollService implements RecalculatePayrollUseCase {

    private static final DateTimeFormatter PAYROLL_PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final PayrollRepository payrollRepository;
    private final CalculatePayrollUnitUseCase calculatePayrollUnitUseCase;

    public RecalculatePayrollService(
            PayrollRepository payrollRepository,
            CalculatePayrollUnitUseCase calculatePayrollUnitUseCase
    ) {
        this.payrollRepository = payrollRepository;
        this.calculatePayrollUnitUseCase = calculatePayrollUnitUseCase;
    }

    @Override
    @Transactional
    public Payroll recalculate(RecalculatePayrollCommand command) {
        Payroll payroll = payrollRepository.findByBusinessKey(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber(),
                command.payrollPeriodCode(),
                command.payrollTypeCode(),
                command.presenceNumber()
        ).orElseThrow(() -> new PayrollNotFoundException(
                command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber(),
                command.payrollPeriodCode(), command.payrollTypeCode(), command.presenceNumber()
        ));

        if (!payroll.canBeRecalculated()) {
            throw new PayrollRecalculationNotAllowedException(
                    command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber(),
                    command.payrollPeriodCode(), command.payrollTypeCode(), command.presenceNumber(),
                    payroll.getStatus()
            );
        }

        LocalDate periodStart = parsePeriodStart(command.payrollPeriodCode());
        return calculatePayrollUnitUseCase.calculate(new CalculatePayrollUnitCommand(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber(),
                command.payrollPeriodCode(),
                command.payrollTypeCode(),
                command.presenceNumber(),
                periodStart,
                periodStart.withDayOfMonth(periodStart.lengthOfMonth()),
                payroll.getCalculationEngineCode(),
                payroll.getCalculationEngineVersion(),
                // El recalculo puntual no nace de un lanzamiento: no hay ejecucion que anotar. No se
                // arrastra la del recibo anterior, porque no es la que produjo este (backend#62).
                null
        ));
    }

    private LocalDate parsePeriodStart(String periodCode) {
        try {
            return YearMonth.parse(periodCode, PAYROLL_PERIOD_FORMATTER).atDay(1);
        } catch (DateTimeParseException ex) {
            throw new InvalidPayrollArgumentException("payrollPeriodCode must be in yyyyMM format, got: " + periodCode);
        }
    }
}
