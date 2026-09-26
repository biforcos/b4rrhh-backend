package com.b4rrhh.employee.payroll_input.application.usecase;

import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputAlreadyExistsException;
import com.b4rrhh.employee.payroll_input.domain.model.EmployeePayrollInput;
import com.b4rrhh.employee.payroll_input.domain.port.EmployeePayrollInputRepository;
import com.b4rrhh.employee.shared.application.port.DatedWrite;
import com.b4rrhh.employee.shared.application.port.DatedWriteNoticePort;
import com.b4rrhh.employee.shared.application.port.DatedWriteSources;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateEmployeePayrollInputService implements CreateEmployeePayrollInputUseCase {

    private static final String TABLA = "employee.employee_payroll_input";

    private final EmployeePayrollInputRepository repository;
    private final DatedWriteNoticePort datedWrites;

    public CreateEmployeePayrollInputService(
            EmployeePayrollInputRepository repository,
            DatedWriteNoticePort datedWrites
    ) {
        this.repository = repository;
        this.datedWrites = datedWrites;
    }

    @Override
    @Transactional
    public EmployeePayrollInput create(CreateEmployeePayrollInputCommand command) {
        String rsc = command.ruleSystemCode().trim().toUpperCase();
        String etc = command.employeeTypeCode().trim().toUpperCase();
        String en  = command.employeeNumber().trim();
        String cc  = command.conceptCode().trim().toUpperCase();

        if (repository.existsByBusinessKey(rsc, etc, en, cc, command.period())) {
            throw new EmployeePayrollInputAlreadyExistsException(cc, command.period());
        }

        EmployeePayrollInput input = EmployeePayrollInput.create(rsc, etc, en, cc,
                command.period(), command.quantity());
        EmployeePayrollInput guardado = repository.save(input);

        // La entrada de nomina no lleva fecha: lleva el periodo, asi que no se le inventa una. Y su
        // identidad es la clave de negocio y no un id, asi que la marca guarda concepto y periodo
        // (backend#130).
        datedWrites.notice(DatedWrite.forPeriod(command.period(), rsc, etc, en,
                DatedWriteSources.PAYROLL_INPUT, TABLA, cc + "/" + command.period()));

        return guardado;
    }
}
