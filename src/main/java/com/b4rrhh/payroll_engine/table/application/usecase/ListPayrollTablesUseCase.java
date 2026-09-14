package com.b4rrhh.payroll_engine.table.application.usecase;

import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableSummary;

import java.util.List;

public interface ListPayrollTablesUseCase {
    List<PayrollTableSummary> list(String ruleSystemCode);
}
