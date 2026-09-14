package com.b4rrhh.payroll_engine.table.infrastructure.web;

import java.util.List;

public record PayrollTableSummaryResponse(
        String ruleSystemCode,
        String tableCode,
        long rowCount,
        long activeRowCount,
        List<PayrollTableBindingResponse> bindings
) {
}
