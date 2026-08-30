package com.b4rrhh.employee.employee.application.usecase;

import com.b4rrhh.employee.employee.domain.model.EmployeeDirectoryPage;
import com.b4rrhh.employee.employee.domain.port.EmployeeDirectoryRepository;
import org.springframework.stereotype.Service;

@Service
public class ListEmployeesService implements ListEmployeesUseCase {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 200;

    private final EmployeeDirectoryRepository employeeDirectoryRepository;

    public ListEmployeesService(EmployeeDirectoryRepository employeeDirectoryRepository) {
        this.employeeDirectoryRepository = employeeDirectoryRepository;
    }

    @Override
    public EmployeeDirectoryPage list(ListEmployeesQuery query) {
        String normalizedQueryText = normalizeOptionalText(query.q());
        String normalizedRuleSystemCode = normalizeOptionalCode(query.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeOptionalCode(query.employeeTypeCode());
        String normalizedStatus = normalizeOptionalCode(query.status());
        int normalizedPage = normalizePage(query.page());
        int normalizedSize = normalizeSize(query.size());

        return employeeDirectoryRepository.findDirectoryByFilters(
                normalizedQueryText,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedStatus,
                normalizedPage,
                normalizedSize
        );
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        return normalized.toUpperCase();
    }

    private String normalizeOptionalCode(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        return normalized.toUpperCase();
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }

        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }

        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }

        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and 200");
        }

        return size;
    }
}