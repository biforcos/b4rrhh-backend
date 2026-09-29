package com.b4rrhh.employee.employee.application.usecase;

import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.employee.domain.port.EmployeeRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class CreateEmployeeService implements CreateEmployeeUseCase {

    private final EmployeeRepository employeeRepository;

    public CreateEmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public Employee create(CreateEmployeeCommand command) {
        employeeRepository.findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumber(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber()
        ).ifPresent(existing -> {
            throw new IllegalArgumentException("Employee already exists with ruleSystemCode, employeeTypeCode and employeeNumber");
        });

        Employee newEmployee = new Employee(
                null, // id
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber(),
                command.firstName(),
                command.lastName1(),
                command.lastName2(),
                command.preferredName(),
                LocalDateTime.now(), // createdAt
                LocalDateTime.now(),  // updatedAt
                null // photoUrl
        );

        return employeeRepository.save(newEmployee);
    }
}