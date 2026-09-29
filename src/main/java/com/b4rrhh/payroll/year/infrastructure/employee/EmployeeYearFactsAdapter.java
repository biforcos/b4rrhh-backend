package com.b4rrhh.payroll.year.infrastructure.employee;

import com.b4rrhh.employee.absence.application.usecase.ListEmployeeAbsencesCommand;
import com.b4rrhh.employee.absence.application.usecase.ListEmployeeAbsencesUseCase;
import com.b4rrhh.employee.absence.domain.exception.AbsenceEmployeeNotFoundException;
import com.b4rrhh.employee.payroll_input.application.usecase.ListEmployeePayrollInputsCommand;
import com.b4rrhh.employee.payroll_input.application.usecase.ListEmployeePayrollInputsUseCase;
import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputEmployeeNotFoundException;
import com.b4rrhh.employee.payroll_input.domain.model.EmployeePayrollInput;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.exception.PresenceEmployeeNotFoundException;
import com.b4rrhh.payroll.year.application.model.EmployeeYearAbsence;
import com.b4rrhh.payroll.year.application.model.EmployeeYearPresence;
import com.b4rrhh.payroll.year.application.port.EmployeeYearFactsPort;
import com.b4rrhh.payroll.year.domain.exception.EmployeeYearEmployeeNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * El año pregunta al empleado por las mismas puertas que sus pantallas (b4rrhh/backend#151): los
 * casos de uso de la presencia, las ausencias y las entradas. Así no hay una segunda forma de
 * contar lo mismo, y un cambio en cualquiera de ellas llega al año sin tocarlo.
 */
@Component
public class EmployeeYearFactsAdapter implements EmployeeYearFactsPort {

    private final ListEmployeePresencesUseCase listEmployeePresencesUseCase;
    private final ListEmployeeAbsencesUseCase listEmployeeAbsencesUseCase;
    private final ListEmployeePayrollInputsUseCase listEmployeePayrollInputsUseCase;

    public EmployeeYearFactsAdapter(
            ListEmployeePresencesUseCase listEmployeePresencesUseCase,
            ListEmployeeAbsencesUseCase listEmployeeAbsencesUseCase,
            ListEmployeePayrollInputsUseCase listEmployeePayrollInputsUseCase
    ) {
        this.listEmployeePresencesUseCase = listEmployeePresencesUseCase;
        this.listEmployeeAbsencesUseCase = listEmployeeAbsencesUseCase;
        this.listEmployeePayrollInputsUseCase = listEmployeePayrollInputsUseCase;
    }

    @Override
    public List<EmployeeYearPresence> presences(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        try {
            return listEmployeePresencesUseCase
                    .listByEmployeeBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber).stream()
                    .map(p -> new EmployeeYearPresence(p.getPresenceNumber(), p.getStartDate(), p.getEndDate()))
                    .toList();
        } catch (PresenceEmployeeNotFoundException e) {
            throw new EmployeeYearEmployeeNotFoundException(ruleSystemCode, employeeTypeCode, employeeNumber);
        }
    }

    @Override
    public List<EmployeeYearAbsence> absences(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        try {
            return listEmployeeAbsencesUseCase
                    .listByEmployeeBusinessKey(new ListEmployeeAbsencesCommand(ruleSystemCode, employeeTypeCode, employeeNumber))
                    .stream()
                    .map(a -> new EmployeeYearAbsence(
                            a.getAbsenceTypeCode(), a.getStartDate(), a.getEndDate(), a.isBenefitEntitled()))
                    .toList();
        } catch (AbsenceEmployeeNotFoundException e) {
            throw new EmployeeYearEmployeeNotFoundException(ruleSystemCode, employeeTypeCode, employeeNumber);
        }
    }

    @Override
    public List<String> payrollInputConcepts(
            String ruleSystemCode, String employeeTypeCode, String employeeNumber, int period) {
        try {
            return listEmployeePayrollInputsUseCase
                    .listByEmployeeAndPeriod(new ListEmployeePayrollInputsCommand(
                            ruleSystemCode, employeeTypeCode, employeeNumber, period))
                    .stream()
                    .map(EmployeePayrollInput::getConceptCode)
                    .toList();
        } catch (EmployeePayrollInputEmployeeNotFoundException e) {
            throw new EmployeeYearEmployeeNotFoundException(ruleSystemCode, employeeTypeCode, employeeNumber);
        }
    }
}
