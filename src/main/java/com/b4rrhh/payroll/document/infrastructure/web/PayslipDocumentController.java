package com.b4rrhh.payroll.document.infrastructure.web;

import com.b4rrhh.payroll.document.application.usecase.GetPayslipDocumentCommand;
import com.b4rrhh.payroll.document.application.usecase.GetPayslipDocumentUseCase;
import com.b4rrhh.payroll.document.domain.model.PayslipDocument;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El documento del recibo, servido por su clave de negocio ({@code backend#112}).
 *
 * <p>Cuelga de la direccion del recibo como los pasos de calculo, y por lo mismo: es una cosa del
 * recibo que se pide a demanda, no un campo de su ficha.
 *
 * <p>No declara {@code produces}. Si lo declarase, un error —404, o un definitivo sin documento—
 * se negociaria contra {@code application/pdf} y saldria un 406 sin cuerpo en lugar del mensaje
 * que dice que ha pasado. El tipo lo pone la respuesta, que es donde se sabe.
 */
@RestController
@RequestMapping("/payrolls")
public class PayslipDocumentController {

    private final GetPayslipDocumentUseCase getPayslipDocumentUseCase;

    public PayslipDocumentController(GetPayslipDocumentUseCase getPayslipDocumentUseCase) {
        this.getPayslipDocumentUseCase = getPayslipDocumentUseCase;
    }

    @GetMapping("/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}/document")
    public ResponseEntity<byte[]> getDocument(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String payrollPeriodCode,
            @PathVariable String payrollTypeCode,
            @PathVariable Integer presenceNumber
    ) {
        PayslipDocument document = getPayslipDocumentUseCase.getDocument(new GetPayslipDocumentCommand(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                payrollPeriodCode,
                payrollTypeCode,
                presenceNumber
        ));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename(document.fileName())
                        .build()
                        .toString())
                // Que lo servido sea el documento archivado o un borrador hecho al vuelo se dice
                // aqui ademas de verse en el papel: la pantalla que ofrece la descarga tiene que
                // poder avisar ANTES de que el fichero salga, y no solo dentro del fichero.
                .header("X-Payslip-Document-Definitive", String.valueOf(document.definitive()))
                .body(document.content());
    }
}
