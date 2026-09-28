package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.port.PayrollSearchPage;

public interface SearchPayrollsUseCase {
    PayrollSearchPage search(SearchPayrollsQuery query);
}
