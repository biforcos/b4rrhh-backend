package com.b4rrhh.employee.payroll_input.application.usecase;

import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputAlreadyExistsException;
import com.b4rrhh.employee.payroll_input.domain.model.EmployeePayrollInput;
import com.b4rrhh.employee.payroll_input.domain.port.EmployeePayrollInputRepository;
import com.b4rrhh.employee.shared.application.port.DatedWrite;
import com.b4rrhh.employee.shared.application.port.DatedWriteNoticePort;
import com.b4rrhh.employee.shared.application.port.DatedWriteSources;
import com.b4rrhh.employee.payroll_input.application.service.EmployeePayrollInputGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateEmployeePayrollInputService implements CreateEmployeePayrollInputUseCase {

    private static final String TABLA = "employee.employee_payroll_input";

    private final EmployeePayrollInputRepository repository;
    private final DatedWriteNoticePort datedWrites;
    private final EmployeePayrollInputGuard guard;

    public CreateEmployeePayrollInputService(
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
    public EmployeePayrollInput create(CreateEmployeePayrollInputCommand command) {
        String rsc = command.ruleSystemCode().trim().toUpperCase();
        String etc = command.employeeTypeCode().trim().toUpperCase();
        String en  = command.employeeNumber().trim();
        String cc  = command.conceptCode().trim().toUpperCase();

        // El dominio valida la forma (periodo, cantidad) antes de preguntar nada fuera.
        EmployeePayrollInput input = EmployeePayrollInput.create(rsc, etc, en, cc,
                command.period(), command.quantity());
        // Sólo donde un recibo pueda pagarla (b4rrhh/backend#142).
        guard.check(rsc, etc, en, cc, command.period());

        if (repository.existsByBusinessKey(rsc, etc, en, cc, command.period())) {
            throw new EmployeePayrollInputAlreadyExistsException(cc, command.period());
        }
        EmployeePayrollInput guardado = repository.save(input);

        // La entrada de nomina no lleva fecha: lleva el periodo, asi que no se le inventa una. Y su
        // identidad es la clave de negocio y no un id, asi que la marca guarda concepto y periodo
        // (backend#130).
        datedWrites.notice(DatedWrite.forPeriod(command.period(), rsc, etc, en,
                DatedWriteSources.PAYROLL_INPUT, TABLA, cc + "/" + command.period()));

        return guardado;
    }
}
