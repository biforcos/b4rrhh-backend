package com.b4rrhh.payroll.infrastructure;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll.domain.port.PayrollSearchPage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryPayrollRepository implements PayrollRepository {

    private final Map<Long, Payroll> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public Payroll save(Payroll payroll) {
        Long id = payroll.getId() == null ? sequence.incrementAndGet() : payroll.getId();
        Payroll persisted = Payroll.rehydrate(
                id,
                payroll.getRuleSystemCode(),
                payroll.getEmployeeTypeCode(),
                payroll.getEmployeeNumber(),
                payroll.getPayrollPeriodCode(),
                payroll.getPayrollTypeCode(),
                payroll.getPresenceNumber(),
                payroll.getStatus(),
                payroll.getStatusReasonCode(),
                payroll.getCalculatedAt(),
                payroll.getCalculationEngineCode(),
                payroll.getCalculationEngineVersion(),
                payroll.getWarnings(),
                payroll.getConcepts(),
                payroll.getContextSnapshots(),
                payroll.getCreatedAt() == null ? LocalDateTime.now() : payroll.getCreatedAt(),
                LocalDateTime.now()
        );
        storage.put(id, persisted);
        return persisted;
    }

    @Override
    public void flush() {
        // No-op for in-memory implementation
    }

    @Override
    public void deleteById(Long id) {
        storage.remove(id);
    }

    @Override
    public PayrollSearchPage findPageByFilters(
            String ruleSystemCode, String payrollPeriodCode, String employeeNumber, PayrollStatus status,
            int page, int size) {
        List<Payroll> all = storage.values().stream()
                .filter(p -> ruleSystemCode == null || Objects.equals(p.getRuleSystemCode(), ruleSystemCode))
                .filter(p -> payrollPeriodCode == null || Objects.equals(p.getPayrollPeriodCode(), payrollPeriodCode))
                .filter(p -> employeeNumber == null || Objects.equals(p.getEmployeeNumber(), employeeNumber))
                .filter(p -> status == null || Objects.equals(p.getStatus(), status))
                .toList();
        List<Payroll> slice = all.stream().skip((long) page * size).limit(size).toList();
        return new PayrollSearchPage(slice, page, size, all.size());
    }

    @Override
    public Optional<Payroll> findByBusinessKey(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode,
            Integer presenceNumber
    ) {
        return storage.values().stream()
                .filter(payroll -> payroll.getRuleSystemCode().equals(ruleSystemCode)
                        && payroll.getEmployeeTypeCode().equals(employeeTypeCode)
                        && payroll.getEmployeeNumber().equals(employeeNumber)
                        && payroll.getPayrollPeriodCode().equals(payrollPeriodCode)
                        && payroll.getPayrollTypeCode().equals(payrollTypeCode)
                        && payroll.getPresenceNumber().equals(presenceNumber))
                .findFirst();
    }
}