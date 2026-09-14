package com.b4rrhh.payroll_engine.table.infrastructure.web;

import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.table.application.usecase.CreatePayrollTableCommand;
import com.b4rrhh.payroll_engine.table.application.usecase.CreatePayrollTableUseCase;
import com.b4rrhh.payroll_engine.table.application.usecase.ListPayrollTablesUseCase;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableBinding;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/payroll-engine/{ruleSystemCode}/tables")
public class PayrollTableManagementController {

    private final CreatePayrollTableUseCase createPayrollTableUseCase;
    private final ListPayrollTablesUseCase listPayrollTablesUseCase;

    public PayrollTableManagementController(
            CreatePayrollTableUseCase createPayrollTableUseCase,
            ListPayrollTablesUseCase listPayrollTablesUseCase
    ) {
        this.createPayrollTableUseCase = createPayrollTableUseCase;
        this.listPayrollTablesUseCase = listPayrollTablesUseCase;
    }

    /**
     * Las tablas de verdad, que no son las ranuras (backend#95). El designer
     * venia preguntando por objects?type=TABLE y recibia una ranura -y solo
     * una de las tres, porque las otras dos no son objeto ninguno-.
     */
    @GetMapping
    public List<PayrollTableSummaryResponse> list(@PathVariable String ruleSystemCode) {
        return listPayrollTablesUseCase.list(ruleSystemCode).stream()
                .map(PayrollTableManagementController::toResponse)
                .toList();
    }

    private static PayrollTableSummaryResponse toResponse(PayrollTableSummary tabla) {
        return new PayrollTableSummaryResponse(
                tabla.ruleSystemCode(),
                tabla.tableCode(),
                tabla.rowCount(),
                tabla.activeRowCount(),
                tabla.bindings().stream()
                        .map(PayrollTableManagementController::toResponse)
                        .toList()
        );
    }

    private static PayrollTableBindingResponse toResponse(PayrollTableBinding vinculacion) {
        return new PayrollTableBindingResponse(
                vinculacion.ownerTypeCode(),
                vinculacion.ownerCode(),
                vinculacion.bindingRoleCode(),
                vinculacion.active()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PayrollTableResponse create(
            @PathVariable String ruleSystemCode,
            @Valid @RequestBody CreatePayrollTableRequest request
    ) {
        PayrollObject saved = createPayrollTableUseCase.create(
                new CreatePayrollTableCommand(ruleSystemCode, request.objectCode())
        );
        return new PayrollTableResponse(saved.getRuleSystemCode(), saved.getObjectCode());
    }
}
