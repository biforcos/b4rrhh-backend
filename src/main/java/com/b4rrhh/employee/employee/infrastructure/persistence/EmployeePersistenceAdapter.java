package com.b4rrhh.employee.employee.infrastructure.persistence;

import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.employee.domain.port.EmployeeRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EmployeePersistenceAdapter implements EmployeeRepository {

    private final SpringDataEmployeeRepository springDataEmployeeRepository;

    public EmployeePersistenceAdapter(SpringDataEmployeeRepository springDataEmployeeRepository) {
        this.springDataEmployeeRepository = springDataEmployeeRepository;
    }

    @Override
    public Optional<Employee> findById(Long id) {
        return springDataEmployeeRepository.findById(id)
                .map(this::mapToDomain);
    }

    @Override
    public Optional<Employee> findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumber(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        return springDataEmployeeRepository
                .findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumber(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber
                )
                .map(this::mapToDomain);
    }

    @Override
    public void deleteById(Long id) {
        springDataEmployeeRepository.deleteById(id);
    }

    @Override
    public Employee save(Employee employee) {
        EmployeeEntity entity = mapToEntity(employee);
        EmployeeEntity savedEntity = springDataEmployeeRepository.save(entity);
        return mapToDomain(savedEntity);
    }

    private Employee mapToDomain(EmployeeEntity entity) {
        return new Employee(
                entity.getId(),
                entity.getRuleSystemCode(),
                entity.getEmployeeTypeCode(),
                entity.getEmployeeNumber(),
                entity.getFirstName(),
                entity.getLastName1(),
                entity.getLastName2(),
                entity.getPreferredName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getPhotoUrl()
        );
    }

    private EmployeeEntity mapToEntity(Employee employee) {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setId(employee.getId());
        entity.setRuleSystemCode(employee.getRuleSystemCode());
        entity.setEmployeeTypeCode(employee.getEmployeeTypeCode());
        entity.setEmployeeNumber(employee.getEmployeeNumber());
        entity.setFirstName(employee.getFirstName());
        entity.setLastName1(employee.getLastName1());
        entity.setLastName2(employee.getLastName2());
        entity.setPreferredName(employee.getPreferredName());
        entity.setCreatedAt(employee.getCreatedAt());
        entity.setUpdatedAt(employee.getUpdatedAt());
        entity.setPhotoUrl(employee.getPhotoUrl());
        return entity;
    }
}