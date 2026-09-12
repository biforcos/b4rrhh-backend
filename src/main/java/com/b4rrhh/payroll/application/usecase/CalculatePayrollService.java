package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.PayrollEmployeePresenceLookupPort;
import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.payroll.domain.exception.PayrollBusinessKeyConflictException;
import com.b4rrhh.payroll.domain.exception.PayrollEmployeePresenceNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollRecalculationNotAllowedException;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalculatePayrollService implements CalculatePayrollUseCase {

    private final PayrollRepository payrollRepository;
    private final PayrollEmployeePresenceLookupPort payrollEmployeePresenceLookupPort;

    public CalculatePayrollService(
            PayrollRepository payrollRepository,
            PayrollEmployeePresenceLookupPort payrollEmployeePresenceLookupPort
    ) {
        this.payrollRepository = payrollRepository;
        this.payrollEmployeePresenceLookupPort = payrollEmployeePresenceLookupPort;
    }

    @Override
    @Transactional
    public Payroll calculate(CalculatePayrollCommand command) {
        String ruleSystemCode = normalizeCode(command.ruleSystemCode(), "ruleSystemCode", 5);
        String employeeTypeCode = normalizeCode(command.employeeTypeCode(), "employeeTypeCode", 30);
        String employeeNumber = normalizeText(command.employeeNumber(), "employeeNumber", 15);
        String payrollPeriodCode = normalizeCode(command.payrollPeriodCode(), "payrollPeriodCode", 30);
        String payrollTypeCode = normalizeCode(command.payrollTypeCode(), "payrollTypeCode", 30);
        Integer presenceNumber = normalizePositive(command.presenceNumber(), "presenceNumber");
        PayrollStatus status = normalizeCalculatedStatus(command.status());

        payrollEmployeePresenceLookupPort.findByBusinessKeyForUpdate(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                presenceNumber
        ).orElseThrow(() -> new PayrollEmployeePresenceNotFoundException(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                presenceNumber
        ));

        payrollRepository.findByBusinessKey(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        payrollPeriodCode,
                        payrollTypeCode,
                        presenceNumber
                )
                .ifPresent(existing -> {
                    if (!existing.canBeRecalculated()) {
                        throw new PayrollRecalculationNotAllowedException(
                                ruleSystemCode,
                                employeeTypeCode,
                                employeeNumber,
                                payrollPeriodCode,
                                payrollTypeCode,
                                presenceNumber,
                                existing.getStatus()
                        );
                    }
                    payrollRepository.deleteById(existing.getId());
                    payrollRepository.flush();
                });

        Payroll payroll = Payroll.create(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                payrollPeriodCode,
                payrollTypeCode,
                presenceNumber,
                status,
                command.statusReasonCode(),
                command.calculatedAt(),
                command.calculationEngineCode(),
                command.calculationEngineVersion(),
                command.runId(),
                command.warnings(),
                command.concepts(),
                command.contextSnapshots(),
                command.segments()
        );

        try {
            return payrollRepository.save(payroll);
        } catch (DataIntegrityViolationException ex) {
            throw new PayrollBusinessKeyConflictException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    payrollPeriodCode,
                    payrollTypeCode,
                    presenceNumber
            );
        }
    }

    private PayrollStatus normalizeCalculatedStatus(PayrollStatus status) {
        if (status == null) {
            throw new InvalidPayrollArgumentException("status is required");
        }
        if (status != PayrollStatus.CALCULATED && status != PayrollStatus.NOT_VALID) {
            throw new InvalidPayrollArgumentException("calculate endpoint only supports CALCULATED or NOT_VALID status");
        }
        return status;
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