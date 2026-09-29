package com.b4rrhh.payroll.year.infrastructure.web;

import com.b4rrhh.payroll.year.application.model.EmployeeYear;
import com.b4rrhh.payroll.year.application.usecase.GetEmployeeYearCommand;
import com.b4rrhh.payroll.year.application.usecase.GetEmployeeYearUseCase;
import com.b4rrhh.payroll.year.infrastructure.web.dto.EmployeeYearAbsenceResponse;
import com.b4rrhh.payroll.year.infrastructure.web.dto.EmployeeYearMonthResponse;
import com.b4rrhh.payroll.year.infrastructure.web.dto.EmployeeYearPresenceResponse;
import com.b4rrhh.payroll.year.infrastructure.web.dto.EmployeeYearSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** El año de un empleado para la tira del {@code frontend#109} (b4rrhh/backend#151). */
@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/year-summary")
public class EmployeeYearController {

    private final GetEmployeeYearUseCase getEmployeeYearUseCase;

    public EmployeeYearController(GetEmployeeYearUseCase getEmployeeYearUseCase) {
        this.getEmployeeYearUseCase = getEmployeeYearUseCase;
    }

    @GetMapping
    public EmployeeYearSummaryResponse get(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestParam int year
    ) {
        EmployeeYear employeeYear = getEmployeeYearUseCase.get(
                new GetEmployeeYearCommand(ruleSystemCode, employeeTypeCode, employeeNumber, year));
        return new EmployeeYearSummaryResponse(
                employeeYear.year(),
                employeeYear.presences().stream()
                        .map(p -> new EmployeeYearPresenceResponse(p.presenceNumber(), p.startDate(), p.endDate()))
                        .toList(),
                employeeYear.months().stream()
                        .map(m -> new EmployeeYearMonthResponse(
                                m.payrollPeriodCode(),
                                m.payrollState() == null ? null : m.payrollState().name(),
                                m.payrollInputCount(),
                                m.payrollInputConceptCount(),
                                m.activeRetroMarkCount(),
                                m.consumedRetroMarkCount()))
                        .toList(),
                employeeYear.absences().stream()
                        .map(a -> new EmployeeYearAbsenceResponse(
                                a.absenceTypeCode(), a.startDate(), a.endDate(), a.benefitEntitled()))
                        .toList()
        );
    }
}
