package com.b4rrhh.employee.employee.infrastructure.persistence;

import com.b4rrhh.employee.employee.application.DisplayNameComputationService;
import com.b4rrhh.employee.employee.domain.model.EmployeeDirectoryItem;
import com.b4rrhh.employee.employee.domain.model.EmployeeDirectoryPage;
import com.b4rrhh.employee.employee.domain.port.EmployeeDirectoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class EmployeeDirectoryPersistenceAdapter implements EmployeeDirectoryRepository {

    private final SpringDataEmployeeRepository springDataEmployeeRepository;
    private final DisplayNameComputationService displayNameComputationService;

    public EmployeeDirectoryPersistenceAdapter(
            SpringDataEmployeeRepository springDataEmployeeRepository,
            DisplayNameComputationService displayNameComputationService
    ) {
        this.springDataEmployeeRepository = springDataEmployeeRepository;
        this.displayNameComputationService = displayNameComputationService;
    }

    @Override
    public EmployeeDirectoryPage findDirectoryByFilters(
            String q,
            String ruleSystemCode,
            String employeeTypeCode,
            String status,
            int page,
            int size
    ) {
        Page<EmployeeDirectoryProjection> found = springDataEmployeeRepository.findDirectoryByFilters(
                q,
                ruleSystemCode,
                employeeTypeCode,
                status,
                LocalDate.now(),
                PageRequest.of(page, size)
        );
        return new EmployeeDirectoryPage(
                found.getContent().stream().map(this::toDomain).toList(),
                page,
                size,
                found.getTotalElements()
        );
    }

    private EmployeeDirectoryItem toDomain(EmployeeDirectoryProjection projection) {
        String displayName = displayNameComputationService.compute(
                projection.ruleSystemCode(),
                projection.firstName(),
                projection.lastName1(),
                projection.lastName2(),
                projection.preferredName()
        );
        return new EmployeeDirectoryItem(
                projection.ruleSystemCode(),
                projection.employeeTypeCode(),
                projection.employeeNumber(),
                displayName,
                projection.status(),
                projection.workCenterCode()
        );
    }
}
