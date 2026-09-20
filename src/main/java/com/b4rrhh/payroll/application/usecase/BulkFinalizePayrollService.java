package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.document.application.service.PayslipDocumentArchiver;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * El tercer verbo del periodo: cerrar en masa (backend#102).
 *
 * <p><b>No cierra un periodo.</b> No existe una entidad «periodo» que se cierre, y no haberla
 * inventado es el resultado bueno: una nomina es un recibo univoco de un periodo y una relacion
 * laboral, asi que lo inmutable es el recibo y no el mes. Cerrar en masa es aplicar a muchos
 * recibos el mismo verbo que el {@code backend#90} puso en uno, y nada mas.
 *
 * <p>De ahi salen tres cosas que este servicio <b>no</b> hace, y conviene que esten escritas aqui
 * porque son las tres que alguien va a echar de menos:
 *
 * <ul>
 *   <li><b>No hay reapertura.</b> Un recibo cerrado no se edita; lo que venga despues es otro
 *       recibo —retros, complementarias—. La irreversibilidad no es una carencia que tapar.</li>
 *   <li><b>No hay nada que «abra» el cierre.</b> Cerrar dice «estas nominas ya no se tocan», y de
 *       ahi salen las salidas de nomina.</li>
 *   <li><b>Cerrar no prohibe calcular en el periodo.</b> Un alta posterior produce un recibo que
 *       nunca se cerro, asi que no viola nada: un recibo que no existia no estaba cerrado.</li>
 * </ul>
 *
 * <p>Transaccional entero, como el invalidador masivo y al contrario que el lanzamiento: aqui no
 * hay calculo, son transiciones de estado sobre filas que ya existen, y tardan lo que tarda un
 * update. Media tanda cerrada no tendria ningun lector que lo agradeciera.
 */
@Service
public class BulkFinalizePayrollService implements BulkFinalizePayrollUseCase {

    private final PayrollRepository payrollRepository;
    private final PayrollBulkTargetExpander targetExpander;
    private final PayslipDocumentArchiver payslipDocumentArchiver;

    public BulkFinalizePayrollService(
            PayrollRepository payrollRepository,
            PayrollBulkTargetExpander targetExpander,
            PayslipDocumentArchiver payslipDocumentArchiver
    ) {
        this.payrollRepository = payrollRepository;
        this.targetExpander = targetExpander;
        this.payslipDocumentArchiver = payslipDocumentArchiver;
    }

    @Override
    @Transactional
    public BulkFinalizePayrollResult finalizeBulk(BulkFinalizePayrollCommand command) {
        String ruleSystemCode = PayrollFieldNormalizer.code(command.ruleSystemCode(), "ruleSystemCode", 5);
        String payrollPeriodCode = PayrollFieldNormalizer.code(command.payrollPeriodCode(), "payrollPeriodCode", 30);
        String payrollTypeCode = PayrollFieldNormalizer.code(command.payrollTypeCode(), "payrollTypeCode", 30);
        PayrollLaunchTargetSelection targetSelection = targetExpander.normalize(command.targetSelection());
        LocalDate[] periodBounds = PayrollFieldNormalizer.periodBounds(payrollPeriodCode);

        List<PayrollCalculationUnit> candidates = targetExpander.expand(
                targetSelection, ruleSystemCode, payrollPeriodCode, payrollTypeCode,
                periodBounds[0], periodBounds[1]
        );

        int totalFound = 0;
        int totalFinalized = 0;
        int totalSkippedAlreadyDefinitive = 0;
        int totalSkippedNotEligibleByStatus = 0;
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

            if (existing.getStatus() == PayrollStatus.DEFINITIVE) {
                totalSkippedAlreadyDefinitive++;
            } else if (existing.getStatus() == PayrollStatus.CALCULATED
                    || existing.getStatus() == PayrollStatus.EXPLICIT_VALIDATED) {
                // Cada recibo cerrado emite su documento, igual que en el cierre de uno: la
                // invariante es «un DEFINITIVE tiene su papel», y no «un DEFINITIVE cerrado de uno
                // en uno». Si el almacen se cae a mitad de tanda, esto revienta y la transaccion
                // entera se deshace: no quedan ni recibos cerrados sin documento ni media tanda.
                Payroll definitive = existing.finalizePayroll();
                payslipDocumentArchiver.archive(definitive);
                payrollRepository.save(definitive);
                totalFinalized++;
            } else {
                // NOT_VALID. No es un fallo y no se cuenta como tal: NOT_VALID -> DEFINITIVE no
                // existe porque no hay nada que proteger en un recibo invalido. Que tenga su
                // propio contador es lo que hace que la pantalla ensene la maquina de estados en
                // vez de tener que explicarla (backend#102).
                totalSkippedNotEligibleByStatus++;
            }
        }

        return new BulkFinalizePayrollResult(
                ruleSystemCode,
                payrollPeriodCode,
                payrollTypeCode,
                candidates.size(),
                totalFound,
                totalFinalized,
                totalSkippedAlreadyDefinitive,
                totalSkippedNotEligibleByStatus,
                totalSkippedNotFound
        );
    }
}
