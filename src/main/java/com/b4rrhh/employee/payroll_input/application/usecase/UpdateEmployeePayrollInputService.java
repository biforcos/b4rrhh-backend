package com.b4rrhh.employee.payroll_input.application.usecase;

import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputNotFoundException;
import com.b4rrhh.employee.payroll_input.domain.model.EmployeePayrollInput;
import com.b4rrhh.employee.payroll_input.domain.port.EmployeePayrollInputRepository;
import com.b4rrhh.employee.shared.application.port.DatedWrite;
import com.b4rrhh.employee.shared.application.port.DatedWriteNoticePort;
import com.b4rrhh.employee.shared.application.port.DatedWriteSources;
import com.b4rrhh.employee.payroll_input.application.service.EmployeePayrollInputGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateEmployeePayrollInputService implements UpdateEmployeePayrollInputUseCase {

    private static final String TABLA = "employee.employee_payroll_input";

    private final EmployeePayrollInputRepository repository;
    private final DatedWriteNoticePort datedWrites;
    private final EmployeePayrollInputGuard guard;

    public UpdateEmployeePayrollInputService(
            EmployeePayrollInputRepository repository,
            DatedWriteNoticePort datedWrites,
            EmployeePayrollInputGuard guard
    ) {
        this.repository = repository;
        this.datedWrites = datedWrites;
        this.guard = guard;
    }

    @Override
    @Transactional
    public EmployeePayrollInput update(UpdateEmployeePayrollInputCommand command) {
        String rsc = command.ruleSystemCode().trim().toUpperCase();
        String etc = command.employeeTypeCode().trim().toUpperCase();
        String en  = command.employeeNumber().trim();
        String cc  = command.conceptCode().trim().toUpperCase();

        guard.requireEmployee(rsc, etc, en);
        EmployeePayrollInput input = repository
                .findByBusinessKey(rsc, etc, en, cc, command.period())
                .orElseThrow(() -> new EmployeePayrollInputNotFoundException(cc, command.period()));
        // Corregir es escribir: la misma comprobación que al crear (b4rrhh/backend#142).
        guard.check(rsc, etc, en, cc, command.period());

        input.updateQuantity(command.quantity());
        EmployeePayrollInput guardado = repository.save(input);

        datedWrites.notice(DatedWrite.forPeriod(command.period(), rsc, etc, en,
                DatedWriteSources.PAYROLL_INPUT, TABLA, cc + "/" + command.period()));

        return guardado;
    }
}
