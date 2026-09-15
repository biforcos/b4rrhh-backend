package com.b4rrhh.payroll.application.usecase;

public interface BulkFinalizePayrollUseCase {
    BulkFinalizePayrollResult finalizeBulk(BulkFinalizePayrollCommand command);
}
