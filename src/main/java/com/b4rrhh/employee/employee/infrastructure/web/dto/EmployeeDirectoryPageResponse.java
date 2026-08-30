package com.b4rrhh.employee.employee.infrastructure.web.dto;

import java.util.List;

public record EmployeeDirectoryPageResponse(
        List<EmployeeDirectoryItemResponse> items,
        int page,
        int size,
        long total
) {
}
