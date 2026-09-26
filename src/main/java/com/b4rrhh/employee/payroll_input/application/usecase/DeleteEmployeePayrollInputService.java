package com.b4rrhh.employee.payroll_input.application.usecase;

import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputNotFoundException;
import com.b4rrhh.employee.payroll_input.domain.port.EmployeePayrollInputRepository;
import com.b4rrhh.employee.shared.application.port.DatedWrite;
import com.b4rrhh.employee.shared.application.port.DatedWriteNoticePort;
import com.b4rrhh.employee.shared.application.port.DatedWriteSources;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteEmployeePayrollInputService implements DeleteEmployeePayrollInputUseCase {

    private static final String TABLA = "employee.employee_payroll_input";

    private final EmployeePayrollInputRepository repository;
    private final DatedWriteNoticePort datedWrites;

    public DeleteEmployeePayrollInputService(
            EmployeePayrollInputRepository repository,
            DatedWriteNoticePort datedWrites
    ) {
        this.repository = repository;
        this.datedWrites = datedWrites;
    }

    @Override
    @Transactional
    public void delete(DeleteEmployeePayrollInputCommand command) {
        String rsc = command.ruleSystemCode().trim().toUpperCase();
        String etc = command.employeeTypeCode().trim().toUpperCase();
        String en  = command.employeeNumber().trim();
        String cc  = command.conceptCode().trim().toUpperCase();

        if (!repository.existsByBusinessKey(rsc, etc, en, cc, command.period())) {
            throw new EmployeePayrollInputNotFoundException(cc, command.period());
        }
        repository.deleteByBusinessKey(rsc, etc, en, cc, command.period());

        // Borrar unas horas de un mes cerrado mueve su recibo igual que anadirlas: el atraso sale
        // negativo (backend#130).
        datedWrites.notice(DatedWrite.forPeriod(command.period(), rsc, etc, en,
                DatedWriteSources.PAYROLL_INPUT, TABLA, cc + "/" + command.period()));
    }
}
