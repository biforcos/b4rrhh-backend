package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.PayrollBulkStatusTransitionPort;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class BulkInvalidatePayrollService implements BulkInvalidatePayrollUseCase {

    // El motivo no se pide (backend#150): el texto libre que se escribia moria en la columna sin
    // que nada lo leyera. Lo que la columna puede contar con verdad es que camino invalido.
    static final String STATUS_REASON_CODE = "BULK_INVALIDATION";

    private final PayrollBulkStatusTransitionPort statusTransitionPort;
    private final PayrollBulkTargetExpander targetExpander;

    public BulkInvalidatePayrollService(
            PayrollBulkStatusTransitionPort statusTransitionPort,
            PayrollBulkTargetExpander targetExpander
    ) {
        this.statusTransitionPort = statusTransitionPort;
        this.targetExpander = targetExpander;
    }

    @Override
    @Transactional
    public BulkInvalidatePayrollResult invalidateBulk(BulkInvalidatePayrollCommand command) {
        String ruleSystemCode = PayrollFieldNormalizer.code(command.ruleSystemCode(), "ruleSystemCode", 5);
        String payrollPeriodCode = PayrollFieldNormalizer.code(command.payrollPeriodCode(), "payrollPeriodCode", 30);
        String payrollTypeCode = PayrollFieldNormalizer.code(command.payrollTypeCode(), "payrollTypeCode", 30);
        PayrollLaunchTargetSelection targetSelection = targetExpander.normalize(command.targetSelection());
        LocalDate[] periodBounds = PayrollFieldNormalizer.periodBounds(payrollPeriodCode);

        List<PayrollCalculationUnit> candidates = targetExpander.expand(
                targetSelection, ruleSystemCode, payrollPeriodCode, payrollTypeCode,
                periodBounds[0], periodBounds[1]
        );

        // Es la transicion de Payroll.invalidate, CALCULATED -> NOT_VALID, que no hace nada mas
        // que cambiar el estado y el motivo; por eso se puede aplicar en una sola sentencia en vez
        // de cargar y guardar recibo a recibo, que en la semilla costaba mas de un minuto por mes
        // (backend#150). EXPLICIT_VALIDATED y DEFINITIVE quedan protegidas: el invalidador en masa
        // nunca las toca, y este es el sitio donde los dos verbos de periodo se conocen
        // (backend#102).
        Map<PayrollStatus, Integer> before = statusTransitionPort.moveStatus(
                ruleSystemCode, payrollPeriodCode, payrollTypeCode, candidates,
                PayrollStatus.CALCULATED, PayrollStatus.NOT_VALID, STATUS_REASON_CODE
        );
        int totalFound = before.values().stream().mapToInt(Integer::intValue).sum();
        int totalInvalidated = before.getOrDefault(PayrollStatus.CALCULATED, 0);
        int totalSkippedAlreadyNotValid = before.getOrDefault(PayrollStatus.NOT_VALID, 0);
        int totalSkippedProtected = before.getOrDefault(PayrollStatus.EXPLICIT_VALIDATED, 0)
                + before.getOrDefault(PayrollStatus.DEFINITIVE, 0);
        int totalSkippedNotFound = candidates.size() - totalFound;

        return new BulkInvalidatePayrollResult(
                ruleSystemCode,
                payrollPeriodCode,
                payrollTypeCode,
                candidates.size(),
                totalFound,
                totalInvalidated,
                totalSkippedAlreadyNotValid,
                totalSkippedProtected,
                totalSkippedNotFound
        );
    }
}
