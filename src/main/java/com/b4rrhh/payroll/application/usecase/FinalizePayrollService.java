package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.document.application.service.PayslipDocumentArchiver;
import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.payroll.domain.exception.PayrollNotFoundException;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cerrar un recibo.
 *
 * <p>Cerrar no es solo cambiar un estado desde el {@code backend#112}: es <b>emitir el
 * documento</b>. Lo que se congela no es el calculo, es el papel, y a partir de este commit el
 * papel existe. Un {@code DEFINITIVE} sin su documento seria una promesa rota, asi que si el
 * almacen no contesta el cierre falla y el recibo se queda como estaba.
 */
@Service
public class FinalizePayrollService implements FinalizePayrollUseCase {

    private final PayrollRepository payrollRepository;
    private final PayslipDocumentArchiver payslipDocumentArchiver;

    public FinalizePayrollService(
            PayrollRepository payrollRepository,
            PayslipDocumentArchiver payslipDocumentArchiver
    ) {
        this.payrollRepository = payrollRepository;
        this.payslipDocumentArchiver = payslipDocumentArchiver;
    }

    @Override
    @Transactional
    public Payroll finalizePayroll(FinalizePayrollCommand command) {
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

        Payroll definitive = existing.finalizePayroll();
        // Antes de guardar, no despues: si el almacen no contesta, esto revienta y no llega a
        // escribirse ningun DEFINITIVE. La transaccion lo desharia igual, pero el orden dice la
        // regla en voz alta — no se cierra lo que no se puede entregar.
        payslipDocumentArchiver.archive(definitive);
        return payrollRepository.save(definitive);
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