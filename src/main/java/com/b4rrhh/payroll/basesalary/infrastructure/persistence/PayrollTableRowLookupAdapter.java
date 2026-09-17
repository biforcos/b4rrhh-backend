package com.b4rrhh.payroll.basesalary.infrastructure.persistence;

import com.b4rrhh.payroll.basesalary.domain.PayrollTableRowLookupPort;
import com.b4rrhh.payroll.basesalary.domain.PayrollTableRowRead;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.entity.PayrollTableRowEntity;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.repository.PayrollTableRowRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Adapter implementing PayrollTableRowLookupPort.
 * Resolves payroll table rows from persistence.
 */
@Component
public class PayrollTableRowLookupAdapter implements PayrollTableRowLookupPort {

    private final PayrollTableRowRepository repository;

    public PayrollTableRowLookupAdapter(PayrollTableRowRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PayrollTableRowRead> findApplicableRow(
            String ruleSystemCode,
            String tableCode,
            String searchCode,
            LocalDate effectiveDate
    ) {
        return repository.findLatestValidByRuleSystemCodeAndTableCodeAndSearchCodeAndEffectiveDate(
                ruleSystemCode,
                tableCode,
                searchCode,
                effectiveDate,
                PageRequest.of(0, 1)
        ).stream().findFirst().map(PayrollTableRowLookupAdapter::toRead);
    }

    private static PayrollTableRowRead toRead(PayrollTableRowEntity entity) {
        return new PayrollTableRowRead(
                entity.getId(),
                entity.getTableCode(),
                entity.getMonthlyValue(),
                entity.getDailyValue()
        );
    }
}
