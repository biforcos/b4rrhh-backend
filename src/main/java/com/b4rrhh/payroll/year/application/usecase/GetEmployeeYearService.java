package com.b4rrhh.payroll.year.application.usecase;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll.retro.application.usecase.ListEmployeeRetroMarksCommand;
import com.b4rrhh.payroll.retro.application.usecase.ListEmployeeRetroMarksUseCase;
import com.b4rrhh.payroll.retro.domain.model.RetroMark;
import com.b4rrhh.payroll.retro.domain.model.RetroMarkStatus;
import com.b4rrhh.payroll.year.application.model.EmployeeYear;
import com.b4rrhh.payroll.year.application.model.EmployeeYearAbsence;
import com.b4rrhh.payroll.year.application.model.EmployeeYearMonth;
import com.b4rrhh.payroll.year.application.model.EmployeeYearPayrollState;
import com.b4rrhh.payroll.year.application.model.EmployeeYearPresence;
import com.b4rrhh.payroll.year.application.port.EmployeeYearFactsPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * El año de un empleado en una consulta (b4rrhh/backend#151). Compone lo que ya sirven cinco
 * consultas —presencia, recibos, ausencias, entradas y marcas— y no calcula ninguna de otra
 * manera: los contadores de un mes son lo que devuelven las consultas de ese mes.
 */
@Service
public class GetEmployeeYearService implements GetEmployeeYearUseCase {

    // Recibos de un empleado en un mes: uno por presencia y tipo de nómina, así que cabe de sobra.
    private static final int PAYROLLS_OF_A_MONTH = 50;

    private final EmployeeYearFactsPort employeeYearFactsPort;
    private final PayrollRepository payrollRepository;
    private final ListEmployeeRetroMarksUseCase listEmployeeRetroMarksUseCase;

    public GetEmployeeYearService(
            EmployeeYearFactsPort employeeYearFactsPort,
            PayrollRepository payrollRepository,
            ListEmployeeRetroMarksUseCase listEmployeeRetroMarksUseCase
    ) {
        this.employeeYearFactsPort = employeeYearFactsPort;
        this.payrollRepository = payrollRepository;
        this.listEmployeeRetroMarksUseCase = listEmployeeRetroMarksUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeYear get(GetEmployeeYearCommand command) {
        String ruleSystemCode = required(command.ruleSystemCode(), "ruleSystemCode").toUpperCase();
        String employeeTypeCode = required(command.employeeTypeCode(), "employeeTypeCode").toUpperCase();
        String employeeNumber = required(command.employeeNumber(), "employeeNumber");
        int year = command.year();
        if (year < 2000 || year > 9999) {
            throw new IllegalArgumentException("year must be between 2000 and 9999");
        }
        LocalDate firstDay = LocalDate.of(year, 1, 1);
        LocalDate lastDay = LocalDate.of(year, 12, 31);

        // La presencia va primero: es la que dice que el empleado no existe.
        List<EmployeeYearPresence> presences = employeeYearFactsPort
                .presences(ruleSystemCode, employeeTypeCode, employeeNumber).stream()
                .filter(p -> touches(p.startDate(), p.endDate(), firstDay, lastDay))
                .sorted(Comparator.comparing(EmployeeYearPresence::startDate))
                .toList();

        List<EmployeeYearAbsence> absences = employeeYearFactsPort
                .absences(ruleSystemCode, employeeTypeCode, employeeNumber).stream()
                .filter(a -> touches(a.startDate(), a.endDate(), firstDay, lastDay))
                .sorted(Comparator.comparing(EmployeeYearAbsence::startDate))
                .toList();

        // Las descartadas no salen: no le pasó nada al año.
        List<RetroMark> marks = listEmployeeRetroMarksUseCase
                .list(new ListEmployeeRetroMarksCommand(ruleSystemCode, employeeTypeCode, employeeNumber)).stream()
                .filter(m -> m.getStatus() != RetroMarkStatus.DISCARDED)
                .toList();

        List<EmployeeYearMonth> months = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            int period = year * 100 + month;
            String periodCode = Integer.toString(period);
            List<String> inputConcepts = employeeYearFactsPort
                    .payrollInputConcepts(ruleSystemCode, employeeTypeCode, employeeNumber, period);
            months.add(new EmployeeYearMonth(
                    periodCode,
                    payrollStateOf(ruleSystemCode, employeeTypeCode, employeeNumber, periodCode),
                    inputConcepts.size(),
                    new HashSet<>(inputConcepts).size(),
                    count(marks, periodCode, RetroMarkStatus.ACTIVE),
                    count(marks, periodCode, RetroMarkStatus.CONSUMED)
            ));
        }

        return new EmployeeYear(year, presences, months, absences);
    }

    private EmployeeYearPayrollState payrollStateOf(
            String ruleSystemCode, String employeeTypeCode, String employeeNumber, String periodCode) {
        List<Payroll> payrolls = payrollRepository
                .findPageByFilters(ruleSystemCode, periodCode, employeeNumber, null, 0, PAYROLLS_OF_A_MONTH)
                .items().stream()
                .filter(p -> p.getEmployeeTypeCode().equals(employeeTypeCode))
                .toList();
        if (payrolls.isEmpty()) {
            return null;
        }
        return payrolls.stream().allMatch(p -> p.getStatus() == PayrollStatus.DEFINITIVE)
                ? EmployeeYearPayrollState.CLOSED
                : EmployeeYearPayrollState.OPEN;
    }

    private static int count(List<RetroMark> marks, String periodCode, RetroMarkStatus status) {
        return (int) marks.stream()
                .filter(m -> m.getStatus() == status && periodCode.equals(m.getFromPeriodCode()))
                .count();
    }

    private static boolean touches(LocalDate start, LocalDate end, LocalDate firstDay, LocalDate lastDay) {
        return !start.isAfter(lastDay) && (end == null || !end.isBefore(firstDay));
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
