package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.payroll.domain.exception.PayrollNotFoundException;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvalidatePayrollService implements InvalidatePayrollUseCase {

    // Como en BulkInvalidatePayrollService: el camino, no un texto libre (backend#150).
    static final String STATUS_REASON_CODE = "MANUAL_INVALIDATION";

    private final PayrollRepository payrollRepository;

    public InvalidatePayrollService(PayrollRepository payrollRepository) {
        this.payrollRepository = payrollRepository;
    }

    @Override
    @Transactional
    public Payroll invalidate(InvalidatePayrollCommand command) {
        String ruleSystemCode = normalizeCode(command.ruleSystemCode(), "ruleSystemCode", 5);
        String employeeTypeCode = normalizeCode(command.employeeTypeCode(), "employeeTypeCode", 30);
        String employeeNumber = normalizeText(command.employeeNumber(), "employeeNumber", 15);
        String payrollPeriodCode = normalizeCode(command.payrollPeriodCode(), "payrollPeriodCode", 30);
        String payrollTypeCode = normalizeCode(command.payrollTypeCode(), "payrollTypeCode", 30);
        Integer presenceNumber = normalizePositive(command.presenceNumber(), "presenceNumber");

        Payroll existing = payrollRepository.findByBusinessKey(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        payrollPeriodCode,
                        payrollTypeCode,
                        presenceNumber
                )
                .orElseThrow(() -> new PayrollNotFoundException(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        payrollPeriodCode,
                        payrollTypeCode,
                        presenceNumber
                ));

        Payroll invalidated = existing.invalidate(STATUS_REASON_CODE);
        return payrollRepository.save(invalidated);
    }

    private String normalizeCode(String value, String fieldName, int maxLength) {
        return normalizeText(value, fieldName, maxLength).toUpperCase();
    }

    private String normalizeText(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidPayrollArgumentException(fieldName + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new InvalidPayrollArgumentException(fieldName + " exceeds max length " + maxLength);
        }
        return normalized;
    }

    private Integer normalizePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new InvalidPayrollArgumentException(fieldName + " must be a positive integer");
        }
        return value;
    }
}