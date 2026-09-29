package com.b4rrhh.employee.payroll_input.application.usecase;

import com.b4rrhh.employee.payroll_input.domain.model.EmployeePayrollInput;
import com.b4rrhh.employee.payroll_input.domain.port.EmployeePayrollInputRepository;
import com.b4rrhh.employee.payroll_input.application.service.EmployeePayrollInputGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListEmployeePayrollInputsService implements ListEmployeePayrollInputsUseCase {

    private final EmployeePayrollInputRepository repository;
    private final EmployeePayrollInputGuard guard;

    public ListEmployeePayrollInputsService(EmployeePayrollInputRepository repository, EmployeePayrollInputGuard guard) {
        this.repository = repository;
        this.guard = guard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeePayrollInput> listByEmployeeAndPeriod(ListEmployeePayrollInputsCommand command) {
        String rsc = command.ruleSystemCode().trim().toUpperCase();
        String etc = command.employeeTypeCode().trim().toUpperCase();
        String en  = command.employeeNumber().trim();
        // Una lista vacía de un empleado que no existe decía «no tiene entradas» (b4rrhh/backend#144).
        guard.requireEmployee(rsc, etc, en);
        return repository.findByEmployeeAndPeriod(rsc, etc, en, command.period());
    }
}
