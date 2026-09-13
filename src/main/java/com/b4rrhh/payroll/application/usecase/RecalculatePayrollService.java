package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.payroll.domain.exception.PayrollNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollRecalculationNotAllowedException;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll_engine.metamodel.domain.port.RuleSystemMetamodelRepository;
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
    private final RuleSystemMetamodelRepository ruleSystemMetamodelRepository;

    public RecalculatePayrollService(
            PayrollRepository payrollRepository,
            CalculatePayrollUnitUseCase calculatePayrollUnitUseCase,
            RuleSystemMetamodelRepository ruleSystemMetamodelRepository
    ) {
        this.payrollRepository = payrollRepository;
        this.calculatePayrollUnitUseCase = calculatePayrollUnitUseCase;
        this.ruleSystemMetamodelRepository = ruleSystemMetamodelRepository;
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
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());

        // Un recalculo puntual tambien es una ejecucion, de una sola unidad: lee su
        // reglamentacion aqui y calcula contra ella. Por eso ve los cambios del grafo que
        // haya habido desde la corrida que produjo el recibo anterior (backend#87).
        return calculatePayrollUnitUseCase.calculate(new CalculatePayrollUnitCommand(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber(),
                command.payrollPeriodCode(),
                command.payrollTypeCode(),
                command.presenceNumber(),
                periodStart,
                periodEnd,
                payroll.getCalculationEngineCode(),
                payroll.getCalculationEngineVersion(),
                // El recalculo puntual no nace de un lanzamiento: no hay ejecucion que anotar. No se
                // arrastra la del recibo anterior, porque no es la que produjo este (backend#62).
                null,
                ruleSystemMetamodelRepository.load(command.ruleSystemCode(), periodEnd)
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
