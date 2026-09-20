package com.b4rrhh.payroll.document.application.usecase;

import com.b4rrhh.payroll.document.domain.model.PayslipDocument;

/** Traer el documento de un recibo ({@code backend#112}). */
public interface GetPayslipDocumentUseCase {

    PayslipDocument getDocument(GetPayslipDocumentCommand command);
}
