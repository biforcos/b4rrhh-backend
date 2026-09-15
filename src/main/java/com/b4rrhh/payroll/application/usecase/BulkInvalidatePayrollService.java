package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class BulkInvalidatePayrollService implements BulkInvalidatePayrollUseCase {

    private final PayrollRepository payrollRepository;
    private final PayrollBulkTargetExpander targetExpander;

    public BulkInvalidatePayrollService(
            PayrollRepository payrollRepository,
            PayrollBulkTargetExpander targetExpander
    ) {
        this.payrollRepository = payrollRepository;
        this.targetExpander = targetExpander;
    }

    @Override
    @Transactional
    public BulkInvalidatePayrollResult invalidateBulk(BulkInvalidatePayrollCommand command) {
        String ruleSystemCode = PayrollFieldNormalizer.code(command.ruleSystemCode(), "ruleSystemCode", 5);
        String payrollPeriodCode = PayrollFieldNormalizer.code(command.payrollPeriodCode(), "payrollPeriodCode", 30);
        String payrollTypeCode = PayrollFieldNormalizer.code(command.payrollTypeCode(), "payrollTypeCode", 30);
        String statusReasonCode = PayrollFieldNormalizer.code(command.statusReasonCode(), "statusReasonCode", 50);
        PayrollLaunchTargetSelection targetSelection = targetExpander.normalize(command.targetSelection());
        LocalDate[] periodBounds = PayrollFieldNormalizer.periodBounds(payrollPeriodCode);

        List<PayrollCalculationUnit> candidates = targetExpander.expand(
                targetSelection, ruleSystemCode, payrollPeriodCode, payrollTypeCode,
                periodBounds[0], periodBounds[1]
        );

        int totalFound = 0;
        int totalInvalidated = 0;
        int totalSkippedAlreadyNotValid = 0;
        int totalSkippedProtected = 0;
        int totalSkippedNotFound = 0;

        for (PayrollCalculationUnit unit : candidates) {
            Optional<Payroll> found = payrollRepository.findByBusinessKey(
                    unit.ruleSystemCode(),
                    unit.employeeTypeCode(),
                    unit.employeeNumber(),
                    unit.payrollPeriodCode(),
                    unit.payrollTypeCode(),
                    unit.presenceNumber()
            );

            if (found.isEmpty()) {
                totalSkippedNotFound++;
                continue;
            }

            totalFound++;
            Payroll existing = found.get();

            if (existing.getStatus() == PayrollStatus.CALCULATED) {
                Payroll invalidated = existing.invalidate(statusReasonCode);
                payrollRepository.save(invalidated);
                totalInvalidated++;
            } else if (existing.getStatus() == PayrollStatus.NOT_VALID) {
                totalSkippedAlreadyNotValid++;
            } else {
                // EXPLICIT_VALIDATED o DEFINITIVE: protegidas, no se invalidan en masa. El
                // contador llevaba meses valiendo 0 porque no habia forma de cerrar en masa;
                // desde el backend#102 la hay, y este es el sitio donde los dos verbos se
                // conocen.
                totalSkippedProtected++;
            }
        }

        return new BulkInvalidatePayrollResult(
                ruleSystemCode,
                payrollPeriodCode,
                payrollTypeCode,
                candidates.size(),
                totalFound,
                totalInvalidated,
                totalSkippedAlreadyNotValid,
                totalSkippedProtected,
                totalSkippedNotFound,
                statusReasonCode
        );
    }
}
