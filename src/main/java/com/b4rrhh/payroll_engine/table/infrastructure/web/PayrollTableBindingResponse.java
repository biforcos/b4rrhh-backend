package com.b4rrhh.payroll_engine.table.infrastructure.web;

public record PayrollTableBindingResponse(
        String ownerTypeCode,
        String ownerCode,
        String bindingRoleCode,
        boolean active
) {
}
