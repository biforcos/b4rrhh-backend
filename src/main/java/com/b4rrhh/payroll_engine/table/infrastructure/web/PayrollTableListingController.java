package com.b4rrhh.payroll_engine.table.infrastructure.web;

import com.b4rrhh.payroll_engine.table.application.usecase.ListPayrollTablesUseCase;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableBinding;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Las tablas de un sistema de reglas. Solo lista: aqui ya no se crea nada.
 *
 * <p>Tenia tambien un POST que creaba una ranura —un rol de vinculacion— y no una tabla. Los dos
 * compartian ruta y parecian el par obvio sin serlo, porque lo que aquel POST creaba no podia salir
 * en este GET hasta que otra cosa, en otro sitio, le atara una tabla con filas. Se ha ido a
 * {@code POST /payroll-engine/{ruleSystemCode}/binding-roles}, junto al catalogo de objetos, que es
 * lo que de verdad crea (backend#98, ADR-063).
 */
@RestController
@RequestMapping("/payroll-engine/{ruleSystemCode}/tables")
public class PayrollTableListingController {

    private final ListPayrollTablesUseCase listPayrollTablesUseCase;

    public PayrollTableListingController(ListPayrollTablesUseCase listPayrollTablesUseCase) {
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
                .map(PayrollTableListingController::toResponse)
                .toList();
    }

    private static PayrollTableSummaryResponse toResponse(PayrollTableSummary tabla) {
        return new PayrollTableSummaryResponse(
                tabla.ruleSystemCode(),
                tabla.tableCode(),
                tabla.rowCount(),
                tabla.activeRowCount(),
                tabla.bindings().stream()
                        .map(PayrollTableListingController::toResponse)
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
}
