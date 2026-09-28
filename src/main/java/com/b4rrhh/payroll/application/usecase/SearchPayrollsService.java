package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll.domain.port.PayrollSearchPage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class SearchPayrollsService implements SearchPayrollsUseCase {

    private final PayrollRepository payrollRepository;

    public SearchPayrollsService(PayrollRepository payrollRepository) {
        this.payrollRepository = payrollRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollSearchPage search(SearchPayrollsQuery query) {
        return payrollRepository.findPageByFilters(
                query.ruleSystemCode(),
                query.payrollPeriodCode(),
                query.employeeNumber(),
                query.status(),
                query.page(),
                query.size()
        );
    }
}
