package com.b4rrhh.payroll.document.application.usecase;

import com.b4rrhh.payroll.document.application.port.PayslipDocumentRenderer;
import com.b4rrhh.payroll.document.application.service.PayslipDocumentArchiver;
import com.b4rrhh.payroll.document.application.service.PayslipDocumentContentFactory;
import com.b4rrhh.payroll.document.domain.model.PayslipDocument;
import com.b4rrhh.payroll.document.domain.model.PayslipDocumentKey;
import com.b4rrhh.payroll.document.domain.port.PayslipDocumentStorage;
import com.b4rrhh.payroll.domain.exception.PayrollNotFoundException;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayslipSectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El documento de un recibo, servido ({@code backend#112}).
 *
 * <h2>Dos regimenes, y cual manda lo dice el estado</h2>
 *
 * <p>Un {@code DEFINITIVE} <b>se sirve desde el almacen</b>, tal cual se guardo al cerrar. Lo
 * guardado no se regenera: pedirlo dos veces devuelve los mismos bytes porque es el mismo objeto,
 * no porque se generara igual. Esa distincion es toda la decision — dos generaciones con
 * plantillas distintas darian dos documentos del mismo recibo, y el empleado tiene uno.
 *
 * <p>Si <b>no esta</b>, se repone de sus filas definitivas, se guarda y se sirve
 * ({@code backend#174}). Hasta entonces era un 409, y es lo que les pasaba a los cerrados de la
 * semilla en la demo: el volcado lleva la base y no el almacen. Reponerlo es licito porque el PDF
 * es funcion pura de esas filas, y por eso sale el mismo; el porque y su limite estan en
 * {@link PayslipDocumentArchiver#restore}.
 *
 * <p>Los otros tres estados se renderizan <b>aqui y ahora, y no se guardan</b>, con la marca de
 * que no son definitivos. Recalcular y volver a pedirlo enseña lo nuevo, y esta bien, porque
 * todavia no se ha entregado nada.
 *
 * <p>{@code NOT_VALID} tambien se sirve, y no es un descuido: el backend contesta lo que hay y es
 * la pantalla la que decide no ofrecer el gesto. Un recibo invalido del que no se pudiera sacar el
 * papel seria un recibo del que no se puede ver que dice.
 */
@Service
public class GetPayslipDocumentService implements GetPayslipDocumentUseCase {

    private final PayrollRepository payrollRepository;
    private final PayslipDocumentStorage storage;
    private final PayslipDocumentContentFactory contentFactory;
    private final PayslipDocumentRenderer renderer;
    private final PayslipSectionRepository payslipSectionRepository;
    private final PayslipDocumentArchiver archiver;

    public GetPayslipDocumentService(
            PayrollRepository payrollRepository,
            PayslipDocumentStorage storage,
            PayslipDocumentContentFactory contentFactory,
            PayslipDocumentRenderer renderer,
            PayslipSectionRepository payslipSectionRepository,
            PayslipDocumentArchiver archiver
    ) {
        this.payrollRepository = payrollRepository;
        this.storage = storage;
        this.contentFactory = contentFactory;
        this.renderer = renderer;
        this.payslipSectionRepository = payslipSectionRepository;
        this.archiver = archiver;
    }

    @Override
    @Transactional(readOnly = true)
    public PayslipDocument getDocument(GetPayslipDocumentCommand command) {
        Payroll payroll = payrollRepository.findByBusinessKey(
                        command.ruleSystemCode(),
                        command.employeeTypeCode(),
                        command.employeeNumber(),
                        command.payrollPeriodCode(),
                        command.payrollTypeCode(),
                        command.presenceNumber())
                .orElseThrow(() -> new PayrollNotFoundException(
                        command.ruleSystemCode(),
                        command.employeeTypeCode(),
                        command.employeeNumber(),
                        command.payrollPeriodCode(),
                        command.payrollTypeCode(),
                        command.presenceNumber()));

        if (payroll.getStatus() == PayrollStatus.DEFINITIVE) {
            byte[] documento = storage.retrieve(PayslipDocumentKey.of(payroll))
                    .orElseGet(() -> archiver.restore(payroll));
            return new PayslipDocument(documento, renderer.contentType(), fileName(payroll, true), true);
        }

        byte[] borrador = renderer.render(contentFactory.contentOf(
                payroll,
                payslipSectionRepository.findByRuleSystemCode(payroll.getRuleSystemCode())));
        return new PayslipDocument(borrador, renderer.contentType(), fileName(payroll, false), false);
    }

    /** {@code recibo-EMP000001-202609.pdf}, y con {@code -borrador} cuando lo es. */
    private static String fileName(Payroll payroll, boolean definitive) {
        return "recibo-" + payroll.getEmployeeNumber() + "-" + payroll.getPayrollPeriodCode()
                + (definitive ? "" : "-borrador") + ".pdf";
    }
}
